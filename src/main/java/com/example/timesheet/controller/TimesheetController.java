package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
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
import com.example.timesheet.service.TimesheetService;

import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    //Save or update daily entry
    @PostMapping("/timesheets/daily")
    @RequiresKeycloakAuthorization(resource = "tms:employee", scope = "tms:timesheet:add")
    public ResponseEntity<String> saveDailyEntry(@RequestBody DailyTimesheetDto dto) {
        String response = timesheetService.saveDailyEntry(dto);
        return ResponseEntity.ok(response);
    }

    //Submit weekly timesheet
    @PostMapping("/timesheets/weekly")
    @RequiresKeycloakAuthorization(resource = "tms:employee", scope = "tms:timesheet:add")
    public ResponseEntity<String> submitWeeklyTimesheet(@RequestBody TimesheetSummaryDto dto) {
        String response = timesheetService.submitTimesheetSummary(dto);
        return ResponseEntity.ok(response);
    }

    //Manager approve or reject weekly timesheet
    @PostMapping("/timesheets")
    @RequiresKeycloakAuthorization(resource = "tms:rm", scope = "tms:approve:add")
    public ResponseEntity<List<String>> managerApproval(@RequestBody List<ManagerApprovalRequestDto> dtoList) {
        List<String> responses = dtoList.stream()
                .map(timesheetService::approveOrRejectWeekly)
                .toList();
        return ResponseEntity.ok(responses);
    }


    //view timesheet
    @GetMapping("/timesheets/{employeeCode}")
     @RequiresKeycloakAuthorization(resource = "tms:rmemp", scope = "tms:timesheet:get")
    public ResponseEntity<List<TimesheetMatrixRowResponseDto>> getTimesheet(@PathVariable String employeeCode,
                                                                                     @RequestParam Integer year, @RequestParam Integer month) {
        List<TimesheetMatrixRowResponseDto> summaries = timesheetService.getEmployeeTimesheet(employeeCode, year, month);
        return ResponseEntity.ok(summaries);
    }


    //Get daily entries for week from startdate(weekStart date format yyyy-MM-dd)
    @GetMapping("/timesheets/{employeeCode}/{weekStart}")
    @RequiresKeycloakAuthorization(resource = "tms:rmemp", scope = "tms:timesheet:get")
    public ResponseEntity<DailyTimesheetResponseWithStatus> getDailyEntries(
            @PathVariable String employeeCode,
            @PathVariable Date weekStart) {
        DailyTimesheetResponseWithStatus dailyEntries = timesheetService.getDailyEntries(employeeCode, weekStart);
        return ResponseEntity.ok(dailyEntries);
    }

    @GetMapping("/timesheets/managers/{managerCode}")
    public ResponseEntity<PagedResponse<ManagerApprovalRequestDto>> getEmployeesTimesheetUnderManager(
            @PathVariable String managerCode,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int offset,
            @RequestParam int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(timesheetService.getEmployeesTimesheetUnderManager(managerCode, year, month, offset, limit, filters, sorts));
    }

    @GetMapping("/timesheets/users")
    public ResponseEntity<PagedResponse<ManagerApprovalRequestDto>> getEmployeesTimesheet(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int offset,
            @RequestParam int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(timesheetService.getEmployeesTimesheet(year, month, offset, limit, filters, sorts));
    }

}
