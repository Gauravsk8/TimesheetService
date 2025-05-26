package com.example.timesheet.dto.response.ManagerDashboard;


import com.example.timesheet.enums.TimeSheetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardSummaryDto {
    private TimeSheetStatus status;
    private long count;        // number of employees with this status
    private double totalHours; // total hours logged with this status
}
