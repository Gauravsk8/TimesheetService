package com.example.timesheet.dto.response.CCManagerDashboard;


import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CCManagerDashboardDto {

    private int activeProjectCount;

    private Map<String, Double> totalHoursPerProject;

    private Map<String, Long> employeeCountPerProject;

    private Map<String, Long> timesheetStatusSummary; 
}

