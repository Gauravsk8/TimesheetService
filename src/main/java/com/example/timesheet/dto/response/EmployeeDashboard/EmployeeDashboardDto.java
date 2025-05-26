package com.example.timesheet.dto.response.EmployeeDashboard;


import com.example.timesheet.dto.response.TimesheetSummaryResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardDto {
    private String employeeCode;
    private int year;
    private int month;
    private List<TimesheetSummaryResponseDto> weeklySummaries;
    private List<EmployeeStatusSummaryDto> statusSummary;
}
