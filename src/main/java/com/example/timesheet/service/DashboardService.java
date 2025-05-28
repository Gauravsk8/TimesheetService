package com.example.timesheet.service;


import com.example.timesheet.dto.response.ccmanagerdashboard.CCManagerDashboardDto;
import com.example.timesheet.dto.response.employeedashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.managerdashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.projectmanagerdashboard.ProjectManagerDashboardDTO;

public interface DashboardService {
    ManagerDashboardDto getEmployeesTimesheetUnderManager(String managerCode, int year, int month);

    EmployeeDashboardDto getEmployeeDashboard(String employeeCode, int year, int month);
    ProjectManagerDashboardDTO getPmDashboard(String managerCode);

    CCManagerDashboardDto getCCManagerDashboard(String managerCode, Integer year, Integer month);
}
