package com.example.timesheet.dto.response.ProjectManagerDashboard;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectManagerDashboardDTO {

    private int totalActiveProjects;

    private List<ProjectHoursDTO> projectHours;

    private List<ProjectEmployeeCountDTO> employeeDistribution;

    private List<ProjectStatusSummaryDTO> timesheetStatusSummary;

    private List<MonthlyHoursDTO> monthlyHoursTrend;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectHoursDTO {
        private String projectCode;
        private String title;
        private Double totalHours;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectEmployeeCountDTO {
        private String projectCode;
        private int employeeCount;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectStatusSummaryDTO {
        private String projectCode;
        private String status;
        private long count;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyHoursDTO {
        private String month; // Format: YYYY-MM
        private Double totalHours;
    }
}
