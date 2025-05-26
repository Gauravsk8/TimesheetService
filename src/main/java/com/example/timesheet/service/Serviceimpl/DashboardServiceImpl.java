package com.example.timesheet.service.Serviceimpl;

import com.example.timesheet.Repository.DailyTimeSheetRepository;
import com.example.timesheet.Repository.ProjectEmployeeRepository;
import com.example.timesheet.Repository.ProjectRepository;
import com.example.timesheet.Repository.TimesheetSummaryRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.dto.request.WeeklyTimeSheetEntryDto;
import com.example.timesheet.dto.response.CCManagerDashboard.CCManagerDashboardDto;
import com.example.timesheet.dto.response.EmployeeDashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.EmployeeDashboard.EmployeeStatusSummaryDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardResponseDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardSummaryDto;
import com.example.timesheet.dto.response.ProjectManagerDashboard.ProjectManagerDashboardDTO;
import com.example.timesheet.dto.response.TimesheetSummaryResponseDto;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.models.DailyTimeSheet;
import com.example.timesheet.models.Project;
import com.example.timesheet.models.TimesheetSummary;
import com.example.timesheet.service.DashboardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DailyTimeSheetRepository dailyTimeSheetRepository;
    private final TimesheetSummaryRepository timesheetSummaryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectEmployeeRepository projectEmployeeRepository;
    private final IdentityServiceClient identityServiceClient;


    @Override
    public EmployeeDashboardDto getEmployeeDashboard(String employeeCode, int year, int month) {
        List<TimesheetSummary> summaries = timesheetSummaryRepository
                .findByIdEmployeeCodeAndIdTimesheetYearAndIdTimesheetMonth(employeeCode, year, month);

        List<TimesheetSummaryResponseDto> weeklySummaries = summaries.stream().map(summary -> {
                    TimesheetSummaryResponseDto dto = new TimesheetSummaryResponseDto();
                    dto.setEmployeeCode(summary.getId().getEmployeeCode());
                    dto.setTimesheetYear(summary.getId().getTimesheetYear());
                    dto.setTimesheetMonth(summary.getId().getTimesheetMonth());
                    dto.setWeekStart(summary.getId().getWeekStart());
                    dto.setTotalHours(summary.getTotalHours());
                    dto.setStatus(summary.getStatus());
                    dto.setSubmittedDate(summary.getSubmittedDate());
                    dto.setManagerComment(summary.getManagerComment());
                    dto.setApprovedBy(summary.getApprovedBy());
                    return dto;
                }).sorted(Comparator.comparing(TimesheetSummaryResponseDto::getWeekStart))
                .collect(Collectors.toList());

        Map<TimeSheetStatus, Long> statusCountMap = summaries.stream()
                .collect(Collectors.groupingBy(TimesheetSummary::getStatus, Collectors.counting()));

        Map<TimeSheetStatus, Double> statusHourMap = summaries.stream()
                .collect(Collectors.groupingBy(TimesheetSummary::getStatus,
                        Collectors.summingDouble(ts -> ts.getTotalHours() == null ? 0.0 : ts.getTotalHours())));

        List<EmployeeStatusSummaryDto> statusSummary = statusCountMap.entrySet().stream()
                .map(entry -> new EmployeeStatusSummaryDto(
                        entry.getKey(),
                        entry.getValue(),
                        statusHourMap.getOrDefault(entry.getKey(), 0.0)
                )).toList();

        return new EmployeeDashboardDto(employeeCode, year, month, weeklySummaries, statusSummary);
    }

    @Override
    public ProjectManagerDashboardDTO getPmDashboard(String managerCode) {

        List<Project> projects = projectRepository.findByProjectManagerCodeAndIsActiveTrue(managerCode);

        // Total Active Projects
        int totalProjects = projects.size();

        // Hours per project
        List<ProjectManagerDashboardDTO.ProjectHoursDTO> projectHours = dailyTimeSheetRepository.findByProjectCodeIn(
                        projects.stream().map(Project::getProjectCode).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.groupingBy(DailyTimeSheet::getProjectCode,
                        Collectors.summingDouble(DailyTimeSheet::getHoursSpent)))
                .entrySet().stream()
                .map(e -> new ProjectManagerDashboardDTO.ProjectHoursDTO(
                        e.getKey(),
                        projects.stream().filter(p -> p.getProjectCode().equals(e.getKey()))
                                .findFirst().map(Project::getTitle).orElse("N/A"),
                        e.getValue()))
                .collect(Collectors.toList());

        // Employee distribution
        List<ProjectManagerDashboardDTO.ProjectEmployeeCountDTO> employeeDist = projects.stream()
                .map(p -> new ProjectManagerDashboardDTO.ProjectEmployeeCountDTO(
                        p.getProjectCode(),
                        Optional.ofNullable(projectEmployeeRepository.countByProject(p)).orElse(0L).intValue()
                ))
                .collect(Collectors.toList());


        // Timesheet status summary
        List<Object[]> rawStatusSummary =
                timesheetSummaryRepository.countStatusByProjectCode(projects.stream()
                        .map(Project::getProjectCode)
                        .toList());

        List<ProjectManagerDashboardDTO.ProjectStatusSummaryDTO> statusSummary =
                rawStatusSummary.stream()
                        .map(row -> new ProjectManagerDashboardDTO.ProjectStatusSummaryDTO(
                                (String) row[0],             // projectCode
                                (String) row[1],             // status
                                ((Number) row[2]).longValue() // count
                        ))
                        .toList();

        // Monthly hours trend
        List<DailyTimeSheet> allSheets = dailyTimeSheetRepository.findByProjectCodeIn(
                projects.stream().map(Project::getProjectCode).collect(Collectors.toList()));

        Map<String, Double> monthlyTrend = allSheets.stream()
                .collect(Collectors.groupingBy(
                        sheet -> new SimpleDateFormat("yyyy-MM").format(sheet.getWorkDate()),
                        Collectors.summingDouble(DailyTimeSheet::getHoursSpent)));

        List<ProjectManagerDashboardDTO.MonthlyHoursDTO> monthlyHours = monthlyTrend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ProjectManagerDashboardDTO.MonthlyHoursDTO(e.getKey(), e.getValue()))
                .toList();

        return ProjectManagerDashboardDTO.builder()
                .totalActiveProjects(totalProjects)
                .projectHours(projectHours)
                .employeeDistribution(employeeDist)
                .timesheetStatusSummary(statusSummary)
                .monthlyHoursTrend(monthlyHours)
                .build();
    }

    @Override
    @Transactional
    public ManagerDashboardDto getEmployeesTimesheetUnderManager(String managerCode, int year, int month) {
        ResponseEntity<List<UserIdentityDto>> response = identityServiceClient.getEmployeesUnderManager(managerCode);
        List<UserIdentityDto> employees = Optional.ofNullable(response.getBody()).orElse(List.of());

        if (employees.isEmpty()) return new ManagerDashboardDto(List.of(), List.of());

        Map<String, UserIdentityDto> empMap = employees.stream()
                .collect(Collectors.toMap(UserIdentityDto::getEmployeeCode, e -> e));

        List<String> employeeCodes = new ArrayList<>(empMap.keySet());

        List<TimesheetSummary> summaries = timesheetSummaryRepository
                .findByIdEmployeeCodeInAndIdTimesheetYearAndIdTimesheetMonth(employeeCodes, year, month);

        Map<TimeSheetStatus, Long> statusCountMap = summaries.stream()
                .collect(Collectors.groupingBy(TimesheetSummary::getStatus, Collectors.counting()));

        Map<TimeSheetStatus, Double> statusHourMap = summaries.stream()
                .collect(Collectors.groupingBy(TimesheetSummary::getStatus, Collectors.summingDouble(ts -> ts.getTotalHours() == null ? 0 : ts.getTotalHours())));

        List<ManagerDashboardSummaryDto> statusSummary = statusCountMap.entrySet().stream()
                .map(entry -> new ManagerDashboardSummaryDto(
                        entry.getKey(),
                        entry.getValue(),
                        statusHourMap.getOrDefault(entry.getKey(), 0.0)
                ))
                .toList();

        Map<String, List<TimesheetSummary>> groupedByEmp = summaries.stream()
                .collect(Collectors.groupingBy(ts -> ts.getId().getEmployeeCode()));

        List<ManagerDashboardResponseDto> employeeDetails = groupedByEmp.entrySet().stream()
                .map(entry -> {
                    String empCode = entry.getKey();
                    UserIdentityDto user = empMap.get(empCode);

                    List<WeeklyTimeSheetEntryDto> weeklyEntries = entry.getValue().stream()
                            .map(ts -> {
                                Calendar cal = Calendar.getInstance();
                                java.sql.Date weekStart = ts.getId().getWeekStart();
                                cal.setTime(weekStart);
                                cal.add(Calendar.DATE, 6);
                                java.sql.Date weekEnd = new Date(cal.getTimeInMillis());

                                return new WeeklyTimeSheetEntryDto(
                                        weekStart.toString(),
                                        weekEnd.toString(),
                                        ts.getTotalHours() == null ? 0.0 : ts.getTotalHours(),
                                        ts.getStatus().name()
                                );
                            })
                            .sorted(Comparator.comparing(WeeklyTimeSheetEntryDto::getWeekStartDate))
                            .toList();

                    return new ManagerDashboardResponseDto(
                            empCode,
                            user.getFirstName() + " " + user.getLastName(),
                            user.getEmail(),
                            weeklyEntries
                    );
                })
                .sorted(Comparator.comparing(ManagerDashboardResponseDto::getEmployeeCode))
                .toList();

        return new ManagerDashboardDto(employeeDetails, statusSummary);
    }

    @Override
    public CCManagerDashboardDto getCCManagerDashboard(String managerCode, Integer year, Integer month) {
        // Fetch all projects for the cost center manager
        List<Project> projects = projectRepository.findAllByCostCenter_CostCenterManagerCode(managerCode);

        // Collect project codes
        Set<String> projectCodes = projects.stream()
                .map(Project::getProjectCode)
                .collect(Collectors.toSet());

        // Count of active projects
        int activeProjectCount = (int) projects.stream().filter(Project::isActive).count();

        // Fetch total hours per project
        List<Object[]> rawTotalHours = dailyTimeSheetRepository.findTotalHoursPerProject(projectCodes, year, month);
        Map<String, Double> totalHoursPerProject = rawTotalHours.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).doubleValue()
                ));

        List<Object[]> rawEmployeeCounts = projectEmployeeRepository.countEmployeesPerProject(projectCodes);
        Map<String, Long> employeeCountPerProject = rawEmployeeCounts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        List<Object[]> rawTimesheetStatus = timesheetSummaryRepository.countTimesheetStatusByManager(managerCode, year, month);
        Map<String, Long> timesheetStatusSummary = rawTimesheetStatus.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],   // status
                        row -> ((Number) row[1]).longValue()
                ));

        // Build and return the dashboard DTO
        return CCManagerDashboardDto.builder()
                .activeProjectCount(activeProjectCount)
                .totalHoursPerProject(totalHoursPerProject)
                .employeeCountPerProject(employeeCountPerProject)
                .timesheetStatusSummary(timesheetStatusSummary)
                .build();
    }


}
