package com.example.timesheet;

import com.example.timesheet.common.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = "com.example.Timesheet.client")
@EnableConfigurationProperties(CorsProperties.class)
@SpringBootApplication(scanBasePackages = {
        "com.example.timesheet",
})
@EnableScheduling
public class TimesheetApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimesheetApplication.class, args);
    }

}
