package com.example.timesheet.dto.response.ManagerDashboard;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDto {
    private List<ManagerDashboardResponseDto> employeeDetails;
    private List<ManagerDashboardSummaryDto> statusSummary;
}

