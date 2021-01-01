/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.datatype.EI;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_SPECIMEN;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.model.v251.segment.OBR;
import ca.uhn.hl7v2.model.v251.segment.OBX;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

/**
 * Implements NAACCR message protocol, an extension of HL7,
 * This processor inspects an HL7 formatted message and 
 * detects the type of NAACCR Pathology Report format and
 * inserts additional exchange properties denoting report metadata.
 * Each property begins with "naaccr" prefix.
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
        String rawHL7Msg = new String(Base64.getDecoder().decode(exchangeBody.getBytes(StandardCharsets.UTF_8)));

        processHL7Message(exchange, rawHL7Msg);

        //set target kafka topic
        exchange.setProperty("dataStoreUri", 
                parseSimpleExpression("${properties:lfh.connect.datastore.uri}", exchange)
                .replaceAll("<topicName>", "NAACCR"));
        
        
        logger.info("processing completed for "+exchange.getFromRouteId() +" from " + exchange.getFromEndpoint()+ " "+exchange.getProperty("messageType"));
    }

    /**
     * Process an incoming HL7 message
     * If the message is formatted as an NAACCR pathology report
     * the message is interpreted and metadata inserted into
     * the exchange.
     * @param exchange
     * @param message
     * @throws HL7Exception
     */
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

    /**
     * Process an NAACCR formatted message
     * Inserts attributes as metadata into the exchange
     * @param exchange
     * @param oruMsg
     * @throws HL7Exception
     */
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

            processComprehensiveReport(exchange, oruMsg.getPATIENT_RESULT());

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

    /**
     * Processes a comprehensive report
     * By definition, these reports have mutiple subreports
     * each subreport is processed and metadata inserted into 
     * the exchange.
     * @param exchange
     * @param results
     * @throws HL7Exception
     */
    private void processComprehensiveReport(Exchange exchange, ORU_R01_PATIENT_RESULT results) throws HL7Exception {
        //Comprehensive pathology report with multiple reports
        logger.info("detected comprehensive report format");

        //skip the first because that is the comprehensive marker
        for (int i = 1; i < results.getORDER_OBSERVATIONReps(); i++) {

            ORU_R01_ORDER_OBSERVATION obrContainer = results.getORDER_OBSERVATION(i);
            OBR obrMsg = obrContainer.getOBR();

            String reportTypeCode = obrMsg.getObr4_UniversalServiceIdentifier().getCe1_Identifier().getValue();
            String reportTypeCodeSystem = obrMsg.getObr4_UniversalServiceIdentifier().getCe3_NameOfCodingSystem().getValue();

            if ("LN".equals(reportTypeCodeSystem) && "60568-3".equals(reportTypeCode)) { //synoptic report format

                processSynopticReport(exchange, obrContainer);
    
            } else if ("LN".equals(reportTypeCodeSystem) && "11529-5".equals(reportTypeCode)) { //narrative report format
            
                processNarrativeReport(exchange, obrContainer);

            } else { //unknown (non-standard) report format
                logger.warn("unknown or non-standard report format, no further processing");
            }
        }
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
        exchange.setProperty("naaccrReportStyle","narrative");

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
                        //logger.info(key+"."+innerKey+" : "+val);

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

            logger.info("detected synoptic segmented report");
            exchange.setProperty("naaccrReportStyle","synopticSegmented");

            //the first 3 OBX segments contain the synoptic report descriptor 
            ORU_R01_SPECIMEN spmContainer =  obrContainer.getSPECIMEN(0);
            
            int obxCount = spmContainer.getOBXReps();

            if (obxCount >= 3) { //should have 3
                OBX obx0 = spmContainer.getOBX(0);
                OBX obx1 = spmContainer.getOBX(1);
                OBX obx2 = spmContainer.getOBX(2);
                processReportDescriptors(exchange, obx0, obx1, obx2);
            } else {//unexpected format
                logger.warn("Missing expected OBX report descriptors, no further processing.");
            }

        } else {
            //CAP eCC Synoptic report do not use SPM segements
            //they use itemized observations based on a template
            //known as cKey, these are proprietary to CAP
            exchange.setProperty("naaccrReportStyle","synopticItemized");
            logger.info("detected CAP eCC synoptic itemized report");

            //the first 3 OBX segments contain the synoptic report descriptor 
            int obxCount = obrContainer.getOBSERVATIONReps();

            if (obxCount >= 3) { //should have 3
                OBX obx0 = obrContainer.getOBSERVATION(0).getOBX();
                OBX obx1 = obrContainer.getOBSERVATION(1).getOBX();
                OBX obx2 = obrContainer.getOBSERVATION(2).getOBX();
                processReportDescriptors(exchange, obx0, obx1, obx2);

                //process itemized report attributes
                //Map<String, Object> items = new HashMap<>();
                Map<String, List<String>> items = new HashMap<>();

                for (int i = 3; i < obrContainer.getOBSERVATIONReps(); i++) {

                    OBX obx = obrContainer.getOBSERVATION(i).getOBX();

                    String itemId = obx.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
                    if (itemId.contains(".")) {
                        itemId = itemId.split("\\.")[0]; //chop off templateId suffix
                    }
                    String valueType = obx.getObx2_ValueType().getValue();
                    String itemType = obx.getObx3_ObservationIdentifier().getCe2_Text().getValue();
                    String itemValue = obx.getObx5_ObservationValue(0).encode();

                    if ("SECTION".equals(itemValue)) { //not interested in sections
                        continue;
                    }
 
                    try {
                        processCAPItem(exchange, itemId, valueType, itemType, itemValue);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            } else {
                //expected 3 OBX
                logger.warn("Missing expected OBX report descriptors, no further processing.");
            }
        }
    }

    /**
     * Interprets the CAP eCC Items based on their CAP cKey fields
     * Attributes are inserted into the exchange as metadata
     * @param exchange
     * @param itemId
     * @param valueType
     * @param itemType
     * @param itemValue
     */
    private void processCAPItem(Exchange exchange, String itemId, String valueType, String itemType, String itemValue) {

        //consider whether some of these mappings can be moved to configuration file
        if ("15906".equals(itemId)) { //Procedure Type
            //Example: 15907.100004300^Ampullectomy^CAPECC
            exchange.setProperty("naaccrProcedure", itemValue);
        } else if ("33456".equals(itemId)) { //Histological Type
            //Example: 33457.100004300^Arising from intra-ampullary papillary-tubular neoplasm (IAPN)^CAPECC
            String histoType = itemValue.split("\\^")[1];
            exchange.setProperty("naaccrPrimarySiteDescription", histoType);
        } else if ("52515".equals(itemId)) { //Histological Type
            //Example: 2245.100004300^Adenocarcinoma^CAPECC^81403^Adenocarcinoma, NOS^ICDO3
            String[] tokens = itemValue.split("\\^");
            if(tokens.length >= 5) {
                String histoType = tokens[1];
                String icdCode = tokens[3];
                exchange.setProperty("naaccrHistologicalType", histoType);
                exchange.setProperty("naaccrHistologicalIcdO3", icdCode);
            } else { //unexpected format
                logger.warn("unexpected format for CAP eCC Tumor Site (34390");
            }
        } else if ("34390".equals(itemId)) { //Tumor Site
            if ("CWE".equals(valueType)) { //multi-part
                //Example: 2234.100004300^Intra-ampullary^CAPECC^C24.1^Ampulla of Vater^ICDO3
                if (itemValue.contains("^")) {
                    String[] tokens = itemValue.split("\\^");
                    if (tokens.length >= 6) {
                        String description = tokens[1];
                        String site = tokens[4];
                        String icd03 = tokens[3];//SEER ICD-o-3 coding
                        exchange.setProperty("naaccrPrimarySiteFinding", description);
                        exchange.setProperty("naaccrPrimarySite", site);    
                        exchange.setProperty("naaccrHistologicTypeIcdO3", icd03);                   
                    } else { //unexpected format
                        logger.warn("unexpected format for CAP eCC Tumor Site (34390)");
                    }
                } else { //unexpected format
                    logger.warn("unexpected format for CAP eCC Tumor Site (34390)");
                }
            } else if ("TX".equals(valueType)) { //text block
                //Example: perforated
                String observation = itemValue;
                exchange.setProperty("naaccrPrimarySiteFinding", observation);
            }

        }

    }

    /**
     * Process the synoptic report descriptors
     * Each report should contain the Template Source,
     * Template Id, and Template version
     * @param exchange
     * @param obx0
     * @param obx1
     * @param obx2
     * @throws HL7Exception
     */
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

        String reportTemplateVersion = obx2.getObx5_ObservationValue()[0].encode();
        reportFieldDescriptor = obx2.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
        
        if ("60574-1".equals(reportFieldDescriptor)) {
            exchange.setProperty("naaccrReportVersion", reportTemplateVersion);
        }

    }

    /**
     * @param obrContainer
     * @return true - report has SPM segements; false - otherwise
     */
    private boolean hasSPMSegment(ORU_R01_ORDER_OBSERVATION obrContainer) {
        return obrContainer.getSPECIMENReps() >= 1;
    }

    /**
     * Supports recursive parsing of Camel simple/{@link SimpleBuilder} expressions.
     * Recursive parsing is useful when a property is used to specify a simple expression.
     * Note: Consider refactoring placing this utility method in a common utility class
     * lfh.connect.myprop=\${header.foo}
     *
     * @param simpleExpression The simple expression to parse.
     * @param exchange The current message {@link Exchange}
     * @return the parsed expression as a string.
     */
    private String parseSimpleExpression(String simpleExpression, Exchange exchange) {
        String parsedValue = SimpleBuilder
                .simple(simpleExpression)
                .evaluate(exchange, String.class);

        if (parsedValue != null && parsedValue.startsWith("${") && parsedValue.endsWith("}")) {
            return parseSimpleExpression(parsedValue, exchange);
        }
        return parsedValue;
    }
    

}
