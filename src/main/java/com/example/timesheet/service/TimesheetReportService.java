package com.example.timesheet.service;


import com.example.timesheet.models.DailyTimeSheet;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TimesheetReportService {
    ResponseEntity<String> generateReport(Integer year, Integer month, String projectCode, LocalDate startDate, LocalDate endDate);
    ResponseEntity<String> generateReport(int year, int month, String projectCode);
    Map<String, Map<String, List<DailyTimeSheet>>> getMonthlyTimesheetData(int year, int month, String projectCode);


}
