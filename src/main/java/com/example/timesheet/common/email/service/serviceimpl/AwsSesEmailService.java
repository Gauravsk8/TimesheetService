package com.example.timesheet.common.email.service.serviceimpl;

import com.example.timesheet.common.email.EmailProvider;
import com.example.timesheet.common.email.EmailTemplateUtil;
import com.example.timesheet.common.email.service.EmailService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.Map;

@Slf4j
public class AwsSesEmailService implements EmailService {

    private final SesClient sesClient;
    @Setter
    private String senderEmail = "default@yourdomain.com"; // can be overridden by setter

    public AwsSesEmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            Destination destination = Destination.builder().toAddresses(to).build();
            Message message = Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder().text(Content.builder().data(text).build()).build())
                    .build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(destination)
                    .message(message)
                    .source(senderEmail)
                    .build();

            sesClient.sendEmail(request);
        } catch (SesException e) {
            log.error("AWS SES: Failed to send email to {}", to, e);
        }
    }

    @Override
    public String loadTemplate(String templateName, Map<String, String> variables) {
        return EmailTemplateUtil.loadTemplate(templateName, variables);
    }

    @Override
    public void setProvider(EmailProvider provider) {
        // not used
    }

}
