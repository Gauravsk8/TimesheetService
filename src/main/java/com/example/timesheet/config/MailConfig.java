package com.example.timesheet.config;


import com.example.timesheet.common.email.EmailProvider;
import com.example.timesheet.common.email.service.serviceimpl.AwsSesEmailService;
import com.example.timesheet.common.email.service.serviceimpl.DynamicEmailService;
import com.example.timesheet.common.email.service.serviceimpl.JavaMailEmailService;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port

    ) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
       /* mailSender.setUsername(username);
        mailSender.setPassword(password);*/
        return mailSender;
    }

    @Bean
    @ConditionalOnProperty(name = "app.email.provider", havingValue = "AWS_SES")
    public SesClient sesClient() {
        return SesClient.builder()
                // Optionally configure region/credentials
                .build();
    }

    @Bean
    public JavaMailEmailService javaMailEmailService(JavaMailSender javaMailSender) {
        return new JavaMailEmailService(javaMailSender);
    }

    @Bean
    @ConditionalOnProperty(name = "app.email.provider", havingValue = "AWS_SES")
    public AwsSesEmailService awsSesEmailService(SesClient sesClient) {
        return new AwsSesEmailService(sesClient);
    }

    @Bean
    @Primary
    public DynamicEmailService dynamicEmailService(JavaMailEmailService javaMailEmailService,
                                                   @Value("${app.email.provider}") String provider,
                                                   @Autowired(required = false) @Nullable AwsSesEmailService awsSesEmailService) {
        // If awsSesEmailService is null, pass a dummy or handle accordingly
        AwsSesEmailService sesService = awsSesEmailService;
        DynamicEmailService service = new DynamicEmailService(javaMailEmailService, sesService);
        service.setProvider(EmailProvider.valueOf(provider));
        return service;
    }
}

