/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.datatype.EI;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_SPECIMEN;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.model.v251.segment.OBR;
import ca.uhn.hl7v2.model.v251.segment.OBX;
import ca.uhn.hl7v2.model.v251.segment.SPM;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

/**
 * Implements NAACCR message protocol extension to HL7
 * This processor inspects an HL7 formatted message and 
 * detects the type of NAACCR Pathology Report format and
 * adds additional exchange headers denoting report metadata.
 */
public final class Hl7NaaccrProcessor implements Processor {

    private final String routePropertyNamespace;
    private final Logger logger = LoggerFactory.getLogger(Hl7NaaccrProcessor.class);

    /**
     * Creates a new instance, associating it with the specified route property
     * namespace.
     * 
     * @param routePropertyNamespace The property namespace of the route creating
     *                               the instance.
     */
    public Hl7NaaccrProcessor(String routePropertyNamespace) {
        this.routePropertyNamespace = routePropertyNamespace;
    }

    /**
     * Sets metadata fields on the exchange
     * 
     * @param exchange The current {@link Exchange}
     * @throws Exception If an error occurs parsing simple expressions
     */
    @Override
    public void process(Exchange exchange) throws Exception {

        logger.info("processing exchange "+exchange.getFromRouteId() +" from " + exchange.getFromEndpoint()+ " "+exchange.getProperty("messageType"));

        String exchangeBody = exchange.getIn().getBody(String.class);
    
        String routeUri = URLDecoder.decode(exchange.getFromEndpoint().getEndpointUri(), StandardCharsets.UTF_8.name());
        String topicName = exchange.getProperty("dataFormat") + "_" + exchange.getProperty("messageType");

        String rawHL7Msg = new String(Base64.getDecoder().decode(exchangeBody.getBytes(StandardCharsets.UTF_8)));

        processHL7Message(exchange, rawHL7Msg);

        //set target kafka topic - TODO consider changing to use property placeholder
        exchange.setProperty("dataStoreUri", "kafka:NAACCRv2-XML?brokers=localhost:9094");

        for(String key : exchange.getProperties().keySet()) {
            System.out.println(">"+key+":"+exchange.getProperty(key));
        }

        /*
        PatientXmlWriter writer = new PatientXmlWriter();
        Patient patient = new Patient();
        Tumor tumor = new Tumor();
        tumor.addItem(new Item());
        patient.addTumor(tumor);
        writer.writePatient(patient);
        writer.writePatient(patient);
        */
        // create new NAACCR XML format for patient
        //exchange.getOut().setBody("<NaaccrData><Patient><Item naaccrId=\"patientIdNumber\">000001</Item></Patient>");

        logger.info("processing completed for "+exchange.getFromRouteId() +" from " + exchange.getFromEndpoint()+ " "+exchange.getProperty("messageType"));

    }

    private void processHL7Message(Exchange exchange, String message) throws HL7Exception {

        HapiContext context = new DefaultHapiContext();
        context.setValidationContext(ValidationContextFactory.defaultValidation());	
        PipeParser parser = context.getPipeParser();
        Message parsedMsg = parser.parse(message);
        Terser terser = new Terser(parsedMsg);
        String type = terser.get("/.MSH-9-1") + "_" + terser.get("/.MSH-9-2");

        if ("ORU_R01".equals(type)) { //check MSH message type

            logger.info("processing message type: "+type);

            ORU_R01 oruMsg = (ORU_R01) parsedMsg;
            MSH msh = oruMsg.getMSH();
            EI subprotocolType = msh.getMsh21_MessageProfileIdentifier(0);
            String namespace = subprotocolType.getEi2_NamespaceID().getValue();
            String version = subprotocolType.getEi1_EntityIdentifier().getValue();

            logger.info("detected protocol namespace:"+ namespace+ " version:"+version);

            if ("NAACCR_CP".equals(namespace) || "VOL_V_50_ORU_R01".equals(version)) { //NAACCR report

                exchange.setProperty("messageType", "NAACCR_CP");
                exchange.setProperty("naaccrVersion", version);

                processNaaccrReport(exchange, oruMsg);

            } else {
                //not an NAACCR ORU_R01 formattd message
                logger.info("not an NAACCR ORU_R01 HL7 message type, no further processing.");
            }

        } else {// not a lab report
            logger.info("not a ORU_R01 HL7 message type, no further processing.");
        }
    }

    private void processNaaccrReport(Exchange exchange,  ORU_R01 oruMsg) throws HL7Exception {

        ORU_R01_ORDER_OBSERVATION obrContainer = oruMsg.getPATIENT_RESULT().getORDER_OBSERVATION();
        OBR obrMsg = obrContainer.getOBR();

        String reportTypeCode = obrMsg.getObr4_UniversalServiceIdentifier().getCe1_Identifier().getValue();
        String reportTypeName = obrMsg.getObr4_UniversalServiceIdentifier().getCe2_Text().getValue();
        String reportTypeCodeSystem = obrMsg.getObr4_UniversalServiceIdentifier().getCe3_NameOfCodingSystem().getValue();

        exchange.setProperty("naaccrReportTypeCode",reportTypeCode);
        exchange.setProperty("naaccrReportType", reportTypeName);

        logger.info("detected report structure type: "+reportTypeCode+" "+reportTypeName+" "+reportTypeCodeSystem);

        //comprehensive report
        if ("LN".equals(reportTypeCodeSystem) && "60567-5".equals(reportTypeCode)) { 

            processComprehensiveReport(exchange, obrContainer);

        } else if ("LN".equals(reportTypeCodeSystem) && "60568-3".equals(reportTypeCode)) { //synoptic report format

            processSynopticReport(exchange, obrContainer);
 
        } else if ("LN".equals(reportTypeCodeSystem) && "11529-5".equals(reportTypeCode)) { //narrative report format
           
            processNarrativeReport(exchange, obrContainer);

        } else { //unknown (non-standard) report format
            //TODO consider adding support for other report types
            //such as Autopsy, Cytogenetics, and Cytology
            logger.warn("unknown or non-standard report format, no further processing");
        }

    }

    private void processComprehensiveReport(Exchange exchange, ORU_R01_ORDER_OBSERVATION obrContainer) {
        //Comprehensive pathology report with multiple reports
        logger.info("detected comprehensive report format");
    }

    /**
     * Process a Narrative Report Adds metadata to the exchange based on the
     * attributes discovered from the report
     * 
     * @param exchange
     * @param obrContainer
     * @throws HL7Exception
     */
    private void processNarrativeReport(Exchange exchange, ORU_R01_ORDER_OBSERVATION obrContainer) throws HL7Exception {
        logger.info("detected narrative report format");

        //NAACCR defines two types of Narrative report formats:
        // (1)"fully unstructured" narrative report
        //    which is discouraged and should no longer be used
        //    this report uses OBX segment for text blob and does
        //    not use LOINC coded sections
        // (2)"Structured" Narrative report
        //    uses LOINC coded sections for each NAACCR 
        //    pathology report section and each section
        //    is represented as in an OBX observation

        if(hasSPMSegment(obrContainer)) { //should have a Specimen defined

            //used to collect all specimen observations
            Map<String, Map<String, Object>> allObxMap = new HashMap<>();

            //for each Specimen defined in the report
            for (ORU_R01_SPECIMEN spmContainer : obrContainer.getSPECIMENAll()) {
                
                for (OBX obx : spmContainer.getOBXAll()) {
                    //looks like this: 22637-3^Path report.final diagnosis^LN
                    String obxCode = obx.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
                    String valueType = obx.getObx2_ValueType().getValue();
                    String groupId = obx.getObx4_ObservationSubID().getValue();
                    
                    if (groupId == null) {
                        groupId = ""; //default group is the empty string
                    }

                    //this is implemented inconsistently by lab systems
                    //and therefore needs to be flexible in looking for 
                    //the group id on each Obx entry and not rely on Specimen Id
                    Map<String, Object> obxGroupMap;
                    if (!allObxMap.containsKey(groupId)) {
                        obxGroupMap = new HashMap<>();
                        allObxMap.put(groupId, obxGroupMap);
                    } else {
                        obxGroupMap = allObxMap.get(groupId);
                    }

                    if ("TX".equalsIgnoreCase(valueType)) {
                        //narrative entires should be only Text by definition
                        String val = obx.getObx5_ObservationValue(0).encode();
                        obxGroupMap.put(obxCode, val);
                    } else {
                        //Attempt to represent value as text
                        String val = obx.getObx5_ObservationValue(0).encode();
                        logger.warn("unexpected data type encountered for Narrative report: "
                            +obxCode+" "+valueType+" "+val
                            +"; treating value as text data type");
                        obxGroupMap.put(obxCode, val);
                    }
                }

                logger.info("found "+allObxMap.size()+" number of specimen groups");

                for (String key : allObxMap.keySet()) {
                    Map<String, Object> map = allObxMap.get(key);

                    for (String innerKey : map.keySet()) {
                        String val = map.get(innerKey).toString();
                        logger.info(key+"."+innerKey+" : "+val);

                        if ("22637-3".equals(innerKey)) { //final diagnosis
                            exchange.setProperty("naaccrDiagonsis."+key, val);
                        } else if ("22633-2".equals(innerKey)) { //site / origin
                            exchange.setProperty("naaccrSite."+key, val);
                        }
                    }
                }


            }
            

        } else { 
            //this report departs from NAACCR protocol
            //make an attempt to look for findings under OBR directly"
            logger.warn("expected an SPM (specimen) definition, none found");


        }


    }

    /**
     * Process a Synoptic Report format
     * Adds metadata to the exchange object properties
     * based on the discovered pathology report attributes
     * @param exchange
     * @param obrContainer
     * @throws HL7Exception
     */
    private void processSynopticReport(Exchange exchange, ORU_R01_ORDER_OBSERVATION obrContainer) throws HL7Exception { 
        //now figure out what kind of synoptic report
        //some reports can follow synoptic structure based
        //on institutional templates others using CAP
        //others can fully implement CAP eCC itemized observations

        //fully itemized reports do no use SPM segments
        //since the segments are implied in each coded item
        if (hasSPMSegment(obrContainer)) {

        }

        //the first 3 OBX segments contain the synoptic report descriptor 
        int obxCount = obrContainer.getOBSERVATIONReps();

        //CAP eCC reports do not use SPM-style structure
        if (obxCount >= 3) {

            OBX obx0 = obrContainer.getOBSERVATION(0).getOBX();
            OBX obx1 = obrContainer.getOBSERVATION(1).getOBX();
            OBX obx2 = obrContainer.getOBSERVATION(2).getOBX();

            processReportDescriptors(exchange, obx0, obx1, obx2);

        } else if (obrContainer.getSPECIMENReps() >= 1) { //SPM-style

            logger.info("detected SPM-style report");
            exchange.setProperty("naaccrReportStyle","SPM-format");

                //go through the SPMs
            for (ORU_R01_SPECIMEN specimenContainer : obrContainer.getSPECIMENAll()) {

                    SPM spm = specimenContainer.getSPM();
                    obxCount = specimenContainer.getOBXReps();
                    
                    if (obxCount >= 3) {
                        OBX obx0 = specimenContainer.getOBX(0);
                        OBX obx1 = specimenContainer.getOBX(1);
                        OBX obx2 = specimenContainer.getOBX(2);

                        String reportTemplateSource = obx0.getObx5_ObservationValue()[0].encode();
                        String reportTemplateCode = obx0.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();

                        logger.info(reportTemplateCode+" "+reportTemplateSource);

                        String reportTemplate = obx0.getObx5_ObservationValue()[0].encode();
                        String reportTemplateId = obx0.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
                    }
            }

        } else {
            logger.warn("Missing expected OBX report descriptors, no further processing.");
        }

        


    }

    private void processReportDescriptors(Exchange exchange, OBX obx0, OBX obx1, OBX obx2) throws HL7Exception {

        String reportTemplateSource = obx0.getObx5_ObservationValue()[0].encode();
        String reportFieldDescriptor= obx0.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();

        if ("60573-3".equals(reportFieldDescriptor)) { 
            exchange.setProperty("naaccrReportTemplateSource", reportTemplateSource);
        }

        String reportTemplateId= obx1.getObx5_ObservationValue()[0].encode();
        reportFieldDescriptor = obx1.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();

        if ("60572-5".equals(reportFieldDescriptor)) {
            exchange.setProperty("naaccrReportTemplateId", reportTemplateId);
        }

        String reportTemplate = obx2.getObx5_ObservationValue()[0].encode();
        reportFieldDescriptor = obx2.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
        
        if ("60574-1".equals(reportFieldDescriptor)) {
            exchange.setProperty("naaccrReportVersion", reportTemplateId);
        }

    }

    /**
     * @param obrContainer
     * @return true - report has SPM segements; false - otherwise
     */
    private boolean hasSPMSegment(ORU_R01_ORDER_OBSERVATION obrContainer) {
        return obrContainer.getSPECIMENReps() >= 1;
    }
    

}
