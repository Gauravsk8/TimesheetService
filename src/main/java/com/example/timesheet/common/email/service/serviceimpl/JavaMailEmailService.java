package com.example.timesheet.common.email.service.serviceimpl;


import com.example.timesheet.common.email.EmailProvider;
import com.example.timesheet.common.email.EmailTemplateUtil;
import com.example.timesheet.common.email.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


import java.util.Map;

public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public JavaMailEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
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
