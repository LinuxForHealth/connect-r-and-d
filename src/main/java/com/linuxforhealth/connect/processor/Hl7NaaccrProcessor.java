/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

        if ("LN".equals(reportTypeCodeSystem) && "60567-5".equals(reportTypeCode)) { //comprehensive report
            //Comprehensive pathology report with multiple reports

            logger.info("detected comprehensive report format");

            //TODO Future: this represents a small fraction of
            //real world report structures
            //this report structure includes multiple OBR
            //loop through each one to gather report metadata
        }

        //check to see what reporting format is being used
        if ("LN".equals(reportTypeCodeSystem) && "60568-3".equals(reportTypeCode)) { //synoptic report format

            processSynopticReport(exchange, obrContainer);
 
        } else if ("LN".equals(reportTypeCodeSystem) && "11529-5".equals(reportTypeCode)) { //narrative report format
           
            processNarrativeReport(exchange, obrContainer);

        } else { //unknown (non-standard) report format
            logger.warn("unknown or non-standard report format, no further processing");
        }

    }

    private void processNarrativeReport(Exchange exchange, ORU_R01_ORDER_OBSERVATION obrContainer) {

        logger.info("detected narrative report format");
        //Detect if SPM-style format <OBR><SPM><OBX>
        //use LOINC for path report sections if found

    }

    private void processSynopticReport(Exchange exchange, ORU_R01_ORDER_OBSERVATION obrContainer) throws HL7Exception {
            
        //now figure out what kind of synoptic report
        //some reports can follow synoptic structure
        //others can implement CAP eCC itemized observations
        
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
    

}
