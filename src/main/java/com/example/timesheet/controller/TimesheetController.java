package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.common.constants.AuthorizationConstants;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.DailyTimesheetDto;
import com.example.timesheet.dto.request.ManagerApprovalRequestDto;
import com.example.timesheet.dto.request.TimesheetSummaryDto;
import com.example.timesheet.dto.response.TimesheetMatrixRowResponseDto;
import com.example.timesheet.dto.response.DailyTimesheetResponseWithStatus;
import com.example.timesheet.service.TimesheetService;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    @PostMapping("/timesheets/daily")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_EMPLOYEE, scope = AuthorizationConstants.TIMESHEET_ADD)
    public ResponseEntity<String> saveDailyEntry(@RequestBody DailyTimesheetDto dto) {
        String response = timesheetService.saveDailyEntry(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/timesheets/weekly")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_EMPLOYEE, scope = AuthorizationConstants.TIMESHEET_ADD)
    public ResponseEntity<String> submitWeeklyTimesheet(@RequestBody TimesheetSummaryDto dto) {
        String response = timesheetService.submitTimesheetSummary(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/timesheets")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_RM, scope = AuthorizationConstants.APPROVE_ADD)
    public ResponseEntity<List<String>> managerApproval(@RequestBody List<ManagerApprovalRequestDto> dtoList) {
        List<String> responses = dtoList.stream()
                .map(timesheetService::approveOrRejectWeekly)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/timesheets/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_RMEMP, scope = AuthorizationConstants.TIMESHEET_GET)
    public ResponseEntity<List<TimesheetMatrixRowResponseDto>> getTimesheet(@PathVariable String employeeCode,
                                                                            @RequestParam Integer year, @RequestParam Integer month) {
        List<TimesheetMatrixRowResponseDto> summaries = timesheetService.getEmployeeTimesheet(employeeCode, year, month);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/timesheets/{employeeCode}/{weekStart}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_RMEMP, scope = AuthorizationConstants.TIMESHEET_GET)
    public ResponseEntity<DailyTimesheetResponseWithStatus> getDailyEntries(
            @PathVariable String employeeCode,
            @PathVariable Date weekStart) {
        DailyTimesheetResponseWithStatus dailyEntries = timesheetService.getDailyEntries(employeeCode, weekStart);
        return ResponseEntity.ok(dailyEntries);
    }

    @GetMapping("/timesheets/managers/{managerCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_RM, scope = AuthorizationConstants.TIMESHEET_GET)
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
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN, scope = AuthorizationConstants.TIMESHEET_GET)
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
