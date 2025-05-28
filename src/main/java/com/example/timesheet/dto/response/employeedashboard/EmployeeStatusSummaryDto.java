package com.example.timesheet.dto.response.EmployeeDashboard;


import com.example.timesheet.enums.TimeSheetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatusSummaryDto {
    private TimeSheetStatus status;
    private long count;        // Number of weeks
    private double totalHours; // Total hours logged
}
