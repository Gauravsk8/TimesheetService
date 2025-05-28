package com.example.timesheet.dto.response.managerdashboard;


import com.example.timesheet.enums.TimeSheetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardSummaryDto {
    private TimeSheetStatus status;
    private long count;
    private double totalHours;
}
