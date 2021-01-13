package com.linuxforhealth.connect.support;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultMessage;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;


public class FhirAttachmentText {

    private final Logger logger = LoggerFactory.getLogger(FhirAttachmentText.class);

    /**
     * Process DocumentReference and DiagnosticReport fhir resources, extracting attachments
     * into separate exchange messages.
     *
     * @param camelContext
     * @param exchange
     * @return List<Message> of fhir resource attachments
     */
    public List<Message> splitAttachments(CamelContext camelContext, Exchange exchange) {
        Resource r = exchange.getIn().getBody(Resource.class);
        List<Message> messageList = new ArrayList<Message>();
        switch (r.fhirType()) {
            case "DocumentReference" :
                DocumentReference docRef = (DocumentReference) r;
                if (docRef.hasContent()) {
                    docRef.getContent().forEach(content -> {
                        processAttachment(camelContext, messageList, content.getAttachment());
                    });
                }
                break;

            case "DiagnosticReport" :
                DiagnosticReport diagnosticReport = (DiagnosticReport) r;
                if (diagnosticReport.hasPresentedForm()) {
                    diagnosticReport.getPresentedForm().forEach(attachment -> {
                        processAttachment(camelContext, messageList, attachment);
                    });
                }
                break;
        }
        return messageList;
    }

    /**
     * Convert fhir resource attachments into exchange messages
     *
     * @param camelContext
     * @param messageList
     * @param attachment
     */
    private void processAttachment(CamelContext camelContext, List<Message> messageList, Attachment attachment) {
        if (attachment == null || attachment.getData() == null) return;
        DefaultMessage defaultMessage = new DefaultMessage(camelContext);
        defaultMessage.setHeader("contentType", attachment.getContentType());
        defaultMessage.setBody(attachment.getData(), String.class);
        messageList.add(defaultMessage);
    }

}
