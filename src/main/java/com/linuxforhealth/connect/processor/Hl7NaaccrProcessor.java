/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.processor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

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
 * Sets LinuxForHealth Metadata fields using Camel {@link Exchange} properties.
 * Fields set include:
 * <ul>
 * <li>dataFormat</li>
 * <li>messageType</li>
 * <li>routeId</li>
 * <li>uuid</li>
 * <li>routeUri</li>
 * <li>timestamp</li>
 * </ul>
 */
public final class Hl7NaaccrProcessor implements Processor {

    private final String routePropertyNamespace;

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

        System.out.println("HL7 NAACCR process():" + exchange.getFromRouteId() + " " + exchange.getFromEndpoint() + " "
                + exchange.getProperty("messageType"));
        String exchangeBody = exchange.getIn().getBody(String.class);
        // System.out.println(exchangeBody);

        exchange.setProperty("routeId", exchange.getFromRouteId());
        exchange.setProperty("uuid", UUID.randomUUID());
        exchange.setProperty("timestamp", Instant.now().getEpochSecond());

        String routeUri = URLDecoder.decode(exchange.getFromEndpoint().getEndpointUri(), StandardCharsets.UTF_8.name());

        String topicName = exchange.getProperty("dataFormat") + "_" + exchange.getProperty("messageType");

        // String result =
        // Base64.getEncoder().encodeToString(exchangeBody.getBytes(StandardCharsets.UTF_8));
        exchange.setProperty("messageType", "NAACCR-XML");

        String rawHL7Msg = new String(Base64.getDecoder().decode(exchangeBody.getBytes(StandardCharsets.UTF_8)));
        System.out.println("Nl7Naaccr - " + rawHL7Msg);

        processHL7Message(rawHL7Msg);




        exchange.setProperty("dataStoreUri", "kafka:NAACCRv2-XML?brokers=localhost:9094");// parseSimpleExpression("${properties:lfh.connect.datastore.uri}",
                                                                                          // exchange).replaceAll("<topicName>",
                                                                                          // topicName));

        // create new NAACCR XML format for patient

        exchange.getOut().setBody("<NaaccrData><Patient><Item naaccrId=\"patientIdNumber\">000001</Item></Patient>");

        System.out.println("NAACCR XML generated");

    }

    private void processHL7Message(String message) throws HL7Exception {

        HapiContext context = new DefaultHapiContext();
			
        context.setValidationContext(ValidationContextFactory.defaultValidation());	
        
        PipeParser parser = context.getPipeParser();
        
        Message parsedMsg = parser.parse(message);
        Terser terser = new Terser(parsedMsg);

        String type = terser.get("/.MSH-9-1") + "_" + terser.get("/.MSH-9-2");

        System.out.println("HL7NAACCRProcessor() - "+ type);

        if ( "ORU_R01".equals(type)) { //check MSH message type
					
            ORU_R01 oruMsg = (ORU_R01) parsedMsg;
            MSH msh = oruMsg.getMSH();
         
            EI subprotocolType = msh.getMsh21_MessageProfileIdentifier(0);
            
            String namespace = subprotocolType.getEi2_NamespaceID().getValue();
            
            String version = subprotocolType.getEi1_EntityIdentifier().getValue();

            System.out.println("Subprotocol: namespace:"+ namespace+ " version:"+version);

            if("NAACCR_CP".equals(namespace) || "VOL_V_50_ORU_R01".equals(version)) { //NAACCR report

                ORU_R01_ORDER_OBSERVATION obrContainer = oruMsg.getPATIENT_RESULT().getORDER_OBSERVATION();

                OBR obrMsg = obrContainer.getOBR();

                System.out.println("Report Id "+obrMsg.getObr1_SetIDOBR());

                String reportTypeCode = obrMsg.getObr4_UniversalServiceIdentifier().getCe1_Identifier().getValue();
                String reportTypeName = obrMsg.getObr4_UniversalServiceIdentifier().getCe2_Text().getValue();
                String reportTypeCodeSystem = obrMsg.getObr4_UniversalServiceIdentifier().getCe3_NameOfCodingSystem().getValue();

                System.out.println(reportTypeCode+" "+reportTypeName+" "+reportTypeCodeSystem);

                System.out.println(oruMsg.printStructure());

                if("LN".equals(reportTypeCodeSystem) && "60568-3".equals(reportTypeCode)) { //synoptic report format

                    System.out.println("Detected Synoptic Report");

                    //now figure out what kind of synoptic report
                    //first 3 OBX segments contain the synoptic report descriptor 
                    int obxCount = obrContainer.getOBSERVATIONReps();

                    //CAP eCC reports do not use SPM-style structure

                    if(obxCount >= 3) {
                        
                        OBX obx0 = obrContainer.getOBSERVATION(0).getOBX();
                        OBX obx1 = obrContainer.getOBSERVATION(1).getOBX();
                        OBX obx2 = obrContainer.getOBSERVATION(2).getOBX();

                        String reportTemplateSource = obx0.getObx5_ObservationValue()[0].encode();
                        String reportTemplateCode = obx0.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();

                        System.out.println(reportTemplateCode+" "+reportTemplateSource);


                    } else if(obrContainer.getSPECIMENReps() >= 1) { //SPM-style

                        System.out.println("SPM-style report");

                            //go through the SPMs
                        for(ORU_R01_SPECIMEN specimenContainer : obrContainer.getSPECIMENAll()) {

                                SPM spm = specimenContainer.getSPM();

                                obxCount = specimenContainer.getOBXReps();
                                
                                if(obxCount >= 3) {

                                    OBX obx0 = specimenContainer.getOBX(0);
                                    OBX obx1 = specimenContainer.getOBX(1);
                                    OBX obx2 = specimenContainer.getOBX(2);
            
                                    String reportTemplateSource = obx0.getObx5_ObservationValue()[0].encode();
                                    String reportTemplateCode = obx0.getObx3_ObservationIdentifier().getCe1_Identifier().getValue();
            
                                    System.out.println(reportTemplateCode+" "+reportTemplateSource);
             

                                }

                        }

                        
                    } else {
                        System.out.println("Missing OBX report descriptors ");
                    }





                } else if("LN".equals(reportTypeCodeSystem) && "11529-5".equals(reportTypeCode)) { //narrative report format

                    System.out.println("Detected Narrative Report");

                    //Detect if SPM-style format <OBR><SPM><OBX>

                    //need to perform NLP to extract information for cancer registry reporting


                } else if ("LN".equals(reportTypeCodeSystem) && "60567-5".equals(reportTypeCode)) { //comprehensive report
                //60567-5^Comprehensive pathology report with multiple reports


                } else { //unknown (non-standard) report format

                    //TODO decide if to treat this as a validation error 


                }
                





                   //PID pid = oruMessage.getPATIENT_RESULT().getPATIENT().getPID();


            //System.out.println(oruMessage.printStructure());

            //OBX obx1 = oruMsg.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION().getOBX();

           // SPM spm = oruMessage.getPATIENT_RESULT().getORDER_OBSERVATION().getSPECIMEN().getSPM();

            //System.out.println(spm.getSetIDSPM()+" "+spm.getSpm2_SpecimenID());//spm.getSpecimenCollectionDateTime().toString());

            /*
            for(OBX obx : oruMessage.getPATIENT_RESULT().getORDER_OBSERVATION().getSPECIMEN().getOBXAll()) {

                System.out.println("\t\t"+obx.getObx1_SetIDOBX()+" ");
                System.out.println("\t\t"+obx.getObx2_ValueType()+" ");
                System.out.println("\t\t"+obx.getObx3_ObservationIdentifier().toString());
                
                for(Varies v : obx.getObx5_ObservationValue()) {
                                    
                    System.out.println("\t\t\t"+v.toString());

                }


            }*/

        }

        } else { // not a lab report

            //TODO
            //terminate this route from proceeding


        }
    }


    

}
