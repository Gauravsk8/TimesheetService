package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.common.constants.AuthorizationConstants;
import com.example.timesheet.dto.response.ccmanagerdashboard.CCManagerDashboardDto;
import com.example.timesheet.dto.response.employeedashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.managerdashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.projectmanagerdashboard.ProjectManagerDashboardDTO;
import com.example.timesheet.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/manager-dashboard/{managerCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_RM, scope = AuthorizationConstants.DASHBOARD_GET)
    public ResponseEntity<ManagerDashboardDto> getManagerDashboard(
            @PathVariable String managerCode,
            @RequestParam int year,
            @RequestParam int month) {

        ManagerDashboardDto dashboard = dashboardService.getEmployeesTimesheetUnderManager(managerCode, year, month);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/employee-dashboard/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_EMPLOYEE, scope = AuthorizationConstants.DASHBOARD_GET)
    public ResponseEntity<EmployeeDashboardDto> getEmployeeDashboard(
            @PathVariable String employeeCode,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeCode, year, month));
    }

    @GetMapping("/project-manager-dashboard/{projectManagerCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_PM, scope = AuthorizationConstants.DASHBOARD_GET)
    public ProjectManagerDashboardDTO getDashboard(@PathVariable String projectManagerCode) {
        return dashboardService.getPmDashboard(projectManagerCode);
    }

    @GetMapping("/ccm-dashboard/{managerCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_CCM, scope = AuthorizationConstants.DASHBOARD_GET)
    public ResponseEntity<CCManagerDashboardDto> getDashboard(
            @PathVariable String managerCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        CCManagerDashboardDto dashboard = dashboardService.getCCManagerDashboard(managerCode, year, month);
        return ResponseEntity.ok(dashboard);
    }
}
