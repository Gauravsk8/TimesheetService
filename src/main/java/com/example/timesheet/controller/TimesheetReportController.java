package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.common.constants.AuthorizationConstants;
import com.example.timesheet.service.TimesheetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class TimesheetReportController {

    private final TimesheetReportService timesheetReportService;
    @GetMapping("/timesheets/report")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.MANAGER_COM, scope = AuthorizationConstants.COM_MANAGER_GET)
    public ResponseEntity<String> downloadReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return timesheetReportService.generateReport(year, month, projectCode, startDate, endDate);
    }


}
