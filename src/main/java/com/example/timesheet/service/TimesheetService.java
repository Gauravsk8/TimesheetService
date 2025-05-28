package com.example.timesheet.service;

import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.DailyTimesheetDto;
import com.example.timesheet.dto.request.ManagerApprovalRequestDto;
import com.example.timesheet.dto.request.TimesheetSummaryDto;
import com.example.timesheet.dto.response.DailyTimesheetResponseWithStatus;
import com.example.timesheet.dto.response.TimesheetMatrixRowResponseDto;

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