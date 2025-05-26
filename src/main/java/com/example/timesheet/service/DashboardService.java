package com.example.timesheet.service;

import com.example.timesheet.dto.response.CCManagerDashboard.CCManagerDashboardDto;
import com.example.timesheet.dto.response.EmployeeDashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.ProjectManagerDashboard.ProjectManagerDashboardDTO;

public interface DashboardService {
    ManagerDashboardDto getEmployeesTimesheetUnderManager(String managerCode, int year, int month);

    EmployeeDashboardDto getEmployeeDashboard(String employeeCode, int year, int month);
    ProjectManagerDashboardDTO getPmDashboard(String managerCode);

    CCManagerDashboardDto getCCManagerDashboard(String managerCode, Integer year, Integer month);
}
