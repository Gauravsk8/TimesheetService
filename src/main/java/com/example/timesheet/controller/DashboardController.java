package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.dto.response.CCManagerDashboard.CCManagerDashboardDto;
import com.example.timesheet.dto.response.EmployeeDashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.ProjectManagerDashboard.ProjectManagerDashboardDTO;
import com.example.timesheet.service.DashboardService;
import com.example.timesheet.service.Serviceimpl.DashboardServiceImpl;
import com.example.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    //ManagerDashBoard
    @GetMapping("/manager-dashboard/{managerCode}")
    @RequiresKeycloakAuthorization(resource = "tms:rm", scope = "tms:dashboard:get")
    public ResponseEntity<ManagerDashboardDto> getManagerDashboard(
            @PathVariable String managerCode,
            @RequestParam int year,
            @RequestParam int month) {

        ManagerDashboardDto dashboard = dashboardService.getEmployeesTimesheetUnderManager(managerCode, year, month);
        return ResponseEntity.ok(dashboard);
    }

    //Employee Dashboard
    @GetMapping("/employee-dashboard/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = "tms:employee", scope = "tms:dashboard:get")
    public ResponseEntity<EmployeeDashboardDto> getEmployeeDashboard(
            @PathVariable String employeeCode,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeCode, year, month));
    }

    //ProjectManager Dashboard
    @GetMapping("/project-manager-dashboard/{projectManagerCode}")
    @RequiresKeycloakAuthorization(resource = "tms:pm", scope = "tms:dashboard:get")
    public ProjectManagerDashboardDTO getDashboard(@PathVariable String projectManagerCode) {
        return dashboardService.getPmDashboard(projectManagerCode);
    }

    @GetMapping("/ccm-dashboard/{managerCode}")
    @RequiresKeycloakAuthorization(resource = "tms:ccm", scope = "tms:dashboard:get")
    public ResponseEntity<CCManagerDashboardDto> getDashboard(
            @PathVariable String managerCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        CCManagerDashboardDto dashboard = dashboardService.getCCManagerDashboard(managerCode, year, month);
        return ResponseEntity.ok(dashboard);
    }



}

