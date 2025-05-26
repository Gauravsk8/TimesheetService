package com.example.timesheet.scheduler;

import com.example.timesheet.common.email.service.EmailService;
import com.example.timesheet.client.IdentityServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetReminder {

    private final IdentityServiceClient identityServiceClient;
    private final EmailService emailService;

    // Runs every Thursday at 10:15 AM
    @Scheduled(cron = "0 06 13 ? * MON")
    public void sendWeeklyTimesheetReminder() {
        log.info("Running weekly timesheet reminder job...");

        try {
            ResponseEntity<List<Map<String, String>>> response = identityServiceClient.getAllUsersList();

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, String>> users = response.getBody();

                for (Map<String, String> user : users) {
                    String email = user.get("email");
                    String firstName = user.get("firstName");
                    String employeeCode = user.get("employeeCode");

                    if (email != null && !email.isBlank()) {
                        String name = (firstName != null && !firstName.isBlank()) ? firstName : employeeCode;

                        Map<String, String> variables = Map.of("name", name);

                        String subject = "Weekly Timesheet Reminder";
                        String body = emailService.loadTemplate("WeeklyReminderTemplate.txt", variables);

                        emailService.sendEmail(email, subject, body);
                        log.info("Reminder email sent to {}", email);
                    }
                }
            } else {
                log.warn("Failed to fetch users for reminder: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error sending weekly reminder emails", e);
        }
    }
}
