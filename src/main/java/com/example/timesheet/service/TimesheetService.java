package com.example.timesheet.service;

import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.dto.request.DailyTimesheetDto;
import com.example.timesheet.dto.request.ManagerApprovalRequestDto;
import com.example.timesheet.dto.request.TimesheetSummaryDto;
import com.example.timesheet.dto.response.*;
import com.example.timesheet.dto.response.EmployeeDashboard.EmployeeDashboardDto;
import com.example.timesheet.dto.response.ManagerDashboard.ManagerDashboardDto;
import com.example.timesheet.dto.response.ProjectManagerDashboard.ProjectManagerDashboardDTO;
import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.exceptions.TimeSheetException;

import java.sql.Date;
import java.util.List;

public interface TimesheetService {
    String saveDailyEntry(DailyTimesheetDto dtos) throws TimeSheetException;
    String submitTimesheetSummary(TimesheetSummaryDto dto) throws TimeSheetException;
    String approveOrRejectWeekly(ManagerApprovalRequestDto dto) throws TimeSheetException;
    PagedResponse<ManagerApprovalRequestDto> getEmployeesTimesheetUnderManager(
            String managerCode, int year, int month, int offset, int limit,
            List<FilterRequest> filters, List<SortRequest> sorts);

    PagedResponse<ManagerApprovalRequestDto> getEmployeesTimesheet(
            int year, int month, int offset, int limit,
            List<FilterRequest> filters, List<SortRequest> sorts);

    DailyTimesheetResponseWithStatus getDailyEntries(String employeeCode, Date weekStart) throws TimeSheetException;
    void saveTimesheetSummary(TimesheetSummaryDto dto);
    String approveAllUnderManagerForWeek(ManagerApprovalRequestDto approvalRequest) throws TimeSheetException;
    List<TimesheetMatrixRowResponseDto> getEmployeeTimesheet(String employeeCode, Integer year, Integer month);
    TimeSheetStatus getWeeklyStatus(String employeeCode, Date weekStart);
}