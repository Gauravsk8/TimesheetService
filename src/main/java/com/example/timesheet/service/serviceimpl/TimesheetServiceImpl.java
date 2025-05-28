package com.example.timesheet.service.serviceimpl;


import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.DailyTimesheetDto;
import com.example.timesheet.dto.request.DailyTimesheetRequestDto;
import com.example.timesheet.dto.request.TimesheetSummaryDto;
import com.example.timesheet.dto.request.ManagerApprovalRequestDto;
import com.example.timesheet.dto.request.WeeklyTimeSheetEntryDto;
import com.example.timesheet.dto.response.TimesheetMatrixRowResponseDto;
import com.example.timesheet.dto.response.DailyTimesheetResponseWithStatus;
import com.example.timesheet.dto.response.DailyTimeSheetResponseDto;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.EntryType;
import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.models.Project;
import com.example.timesheet.models.DailyTimeSheet;
import com.example.timesheet.models.TimesheetSummary;
import com.example.timesheet.keys.TimesheetSummaryId;
import com.example.timesheet.repository.TimesheetSummaryRepository;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.repository.DailyTimeSheetRepository;
import com.example.timesheet.repository.ProjectEmployeeRepository;

import com.example.timesheet.service.TimesheetService;
import com.example.timesheet.utils.FilterSpecificationBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.example.timesheet.common.constants.ErrorCode.NOT_FOUND_ERROR;

@Service
@RequiredArgsConstructor
public class TimesheetServiceImpl implements TimesheetService{

    private final DailyTimeSheetRepository dailyTimeSheetRepository;
    private final TimesheetSummaryRepository timesheetSummaryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectEmployeeRepository projectEmployeeRepository;
    private final IdentityServiceClient identityServiceClient;
    public static final String TIMESHEET_YEAR = "timesheetYear";
    public static final String TIMESHEET_MONTH = "timesheetMonth";
    public static final String EMPLOYEE_CODE = "employeeCode";
    public static final String WEEK_START = "weekStart";

    // Composite ID fields (prefixed with "id.")
    public static final String ID_TIMESHEET_YEAR = "id.timesheetYear";
    public static final String ID_TIMESHEET_MONTH = "id.timesheetMonth";
    public static final String ID_EMPLOYEE_CODE = "id.employeeCode";
    public static final String ID_WEEK_START = "id.weekStart";
    private static final DateTimeFormatter WEEK_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");


    //  Save or update a daily time entry
    @Transactional
    public String saveDailyEntry(DailyTimesheetDto dtos) {

        for (DailyTimesheetRequestDto dto : dtos.getDailyEntry()) {
            Project project = null;

            if (dto.getProjectCode() != null) {
                ProjectEmployeeId peid = new ProjectEmployeeId(dto.getProjectCode(), dto.getEmployeeCode());
                if (!projectEmployeeRepository.existsById(peid)) {
                    throw new TimeSheetException(
                            NOT_FOUND_ERROR,
                            String.format(ErrorMessage.ASSIGNMENT_NOT_FOUND, dto.getProjectCode(), dto.getEmployeeCode())
                    );
                }

                project = projectRepository.findById(dto.getProjectCode())
                        .orElseThrow(() -> new TimeSheetException(NOT_FOUND_ERROR,
                                String.format(ErrorMessage.PROJECT_NOT_FOUND, dto.getProjectCode())));
            }

            Optional<DailyTimeSheet> existing = dailyTimeSheetRepository
                    .findByEmployeeCodeAndTimesheetYearAndTimesheetMonthAndWorkDateAndEntryTypeAndProjectCode(
                            dto.getEmployeeCode(),
                            dto.getTimesheetYear(),
                            dto.getTimesheetMonth(),
                            dto.getWorkDate(),
                            dto.getEntryType(),
                            dto.getProjectCode()
                    );

            DailyTimeSheet daily;
            if (existing.isPresent()) {
                daily = existing.get();
                // You can either update or skip. Here, we'll update:
                daily.setHoursSpent(dto.getHoursSpent());
                daily.setDescription(dto.getDescription());
            } else {
                daily = new DailyTimeSheet();
                daily.setEmployeeCode(dto.getEmployeeCode());
                daily.setTimesheetYear(dto.getTimesheetYear());
                daily.setTimesheetMonth(dto.getTimesheetMonth());
                daily.setWorkDate(dto.getWorkDate());
                daily.setEntryType(dto.getEntryType());
                daily.setHoursSpent(dto.getHoursSpent());
                daily.setProjectCode(dto.getProjectCode());
                daily.setProject(project);
                daily.setDescription(dto.getDescription());
                daily.setModifiedByManager(false);
            }

            dailyTimeSheetRepository.save(daily);
        }

        TimesheetSummaryDto summaryDto = new TimesheetSummaryDto();
        summaryDto.setEmployeeCode(dtos.getEmployeeCode());
        summaryDto.setTimesheetMonth(dtos.getTimesheetMonth());
        summaryDto.setTimesheetYear(dtos.getTimesheetYear());
        summaryDto.setWeekStart(dtos.getWeekStart());

        saveTimesheetSummary(summaryDto);

        return MessageConstants.DAILY_TIMESHEET_SAVED;
    }




    //  Submit weekly timesheet
    @Transactional
    public String submitTimesheetSummary(TimesheetSummaryDto dto) {

        TimesheetSummaryId id = new TimesheetSummaryId(
                dto.getEmployeeCode(),
                dto.getTimesheetYear(),
                dto.getTimesheetMonth(),
                dto.getWeekStart()
        );

        TimesheetSummary summary = timesheetSummaryRepository.findById(id)
                .orElseThrow(() -> new TimeSheetException(
                        NOT_FOUND_ERROR,
                        String.format(ErrorMessage.TIMESHEET_SUMMARY_NOT_FOUND,
                                dto.getEmployeeCode(), dto.getWeekStart()))
                );

        if (summary.getStatus() != TimeSheetStatus.DRAFT) {
            throw new TimeSheetException(
                    NOT_FOUND_ERROR,
                    ErrorMessage.STATUS_NOT_FOUND);
        }
        summary.setStatus(TimeSheetStatus.SUBMITTED);
        summary.setSubmittedDate(new Timestamp(System.currentTimeMillis()));

        TimesheetSummary submitted = timesheetSummaryRepository.save(summary);

        LocalDate week = submitted.getId().getWeekStart().toLocalDate();
        String formattedWeekStart = week.format(WEEK_DATE_FORMATTER);

        return String.format(
                MessageConstants.SUBMITTED_TIMESHEET,
                submitted.getId().getEmployeeCode(),
                formattedWeekStart,
                submitted.getId().getTimesheetMonth(),
                submitted.getId().getTimesheetYear()
        );


    }


    // Approve or reject timesheet weekly by manager
    @Transactional
    public String approveOrRejectWeekly(ManagerApprovalRequestDto dto) {
        TimesheetSummaryId id = new TimesheetSummaryId(
                dto.getEmployeeCode(),
                dto.getTimesheetYear(),
                dto.getTimesheetMonth(),
                dto.getWeekStart()
        );

        TimesheetSummary summary = timesheetSummaryRepository.findById(id)
                .orElseThrow(() -> new TimeSheetException(
                        NOT_FOUND_ERROR,
                        String.format(ErrorMessage.TIMESHEET_SUMMARY_NOT_FOUND,
                                dto.getEmployeeCode(), dto.getWeekStart()))
                );

        if (dto.getDailyTimeSheetRequests() != null && !dto.getDailyTimeSheetRequests().isEmpty()) {
            Date weekStart = dto.getWeekStart();
            Date weekEnd = Date.valueOf(weekStart.toLocalDate().plusDays(6));

            // Fetch all existing DailyTimeSheets for the employee in that week
            List<DailyTimeSheet> existingSheets = dailyTimeSheetRepository
                    .findByEmployeeCodeAndWorkDateBetween(dto.getEmployeeCode(), weekStart, weekEnd);

            for (DailyTimesheetRequestDto requestDto : dto.getDailyTimeSheetRequests()) {
                EntryType entryType = requestDto.getEntryType();
                boolean matched = false;

                for (DailyTimeSheet sheet : existingSheets) {
                    boolean dateMatch = sheet.getWorkDate().toLocalDate().isEqual(requestDto.getWorkDate().toLocalDate());
                    boolean typeMatch = sheet.getEntryType() != null && sheet.getEntryType().name().equalsIgnoreCase(entryType.name());

                    boolean projectMatch = entryType != EntryType.PROJECT || sheet.getProjectCode() != null && sheet.getProjectCode().trim()
                            .equals(requestDto.getProjectCode().trim());

                    if (dateMatch && typeMatch && projectMatch) {
                        matched = true;

                        boolean hoursChanged = !sheet.getHoursSpent().equals(requestDto.getHoursSpent());
                        boolean descriptionChanged = requestDto.getDescription() != null &&
                                !requestDto.getDescription().trim().equalsIgnoreCase(
                                        sheet.getDescription() != null ? sheet.getDescription().trim() : "");

                        if (hoursChanged || descriptionChanged) {
                            if (hoursChanged) {
                                sheet.setHoursSpent(requestDto.getHoursSpent());
                            }
                            if (descriptionChanged) {
                                sheet.setDescription(requestDto.getDescription());
                            }
                            sheet.setModifiedByManager(true);
                            dailyTimeSheetRepository.save(sheet);
                        }

                        break;
                    }
                }

                if (!matched) {
                    DailyTimeSheet newSheet = new DailyTimeSheet();
                    newSheet.setEmployeeCode(dto.getEmployeeCode());
                    newSheet.setWorkDate(requestDto.getWorkDate());
                    newSheet.setEntryType(requestDto.getEntryType());
                    if (requestDto.getEntryType() == EntryType.PROJECT) {
                        newSheet.setProjectCode(requestDto.getProjectCode());
                    }
                    newSheet.setTimesheetMonth(requestDto.getTimesheetMonth());
                    newSheet.setTimesheetYear(requestDto.getTimesheetYear());
                    newSheet.setDescription(requestDto.getDescription());
                    newSheet.setHoursSpent(requestDto.getHoursSpent());
                    newSheet.setModifiedByManager(true);
                    dailyTimeSheetRepository.save(newSheet);


                }
            }
        }
        //  Recalculate totalHours from request DTOs
        double totalHours = dto.getDailyTimeSheetRequests().stream()
                .mapToDouble(req -> req.getHoursSpent() != null ? req.getHoursSpent() : 0.0)
                .sum();
        summary.setTotalHours(totalHours);

        summary.setStatus(dto.isApprove() ? TimeSheetStatus.APPROVED : TimeSheetStatus.CORRECTION_REQUIRED);
        summary.setManagerComment(dto.getComment());
        summary.setApprovedBy(dto.getManagerCode());
        timesheetSummaryRepository.save(summary);

        LocalDate week = summary.getId().getWeekStart().toLocalDate();
        String formattedWeekStart = week.format(WEEK_DATE_FORMATTER);

        return dto.isApprove()
                ? String.format(MessageConstants.TIMESHEET_APPROVED_BY_MANAGER, dto.getEmployeeCode(), formattedWeekStart, dto.getManagerCode())
                : String.format(MessageConstants.TIMESHEET_REJECTED_BY_MANAGER, dto.getEmployeeCode(), formattedWeekStart, dto.getManagerCode());
    }



    //Employee Dashboard

    // Get daily entries for employee for a given week
    public DailyTimesheetResponseWithStatus getDailyEntries(String employeeCode, Date weekStart) {
        Date weekEnd = Date.valueOf(weekStart.toLocalDate().plusDays(6));

        TimesheetSummary summary = timesheetSummaryRepository
                .findByIdEmployeeCodeAndIdWeekStart(employeeCode, weekStart)
                .orElseThrow(() -> new TimeSheetException(
                        NOT_FOUND_ERROR,
                        String.format(ErrorMessage.TIMESHEET_SUMMARY_NOT_FOUND, employeeCode, weekStart)
                ));

        List<DailyTimeSheetResponseDto> dailyTimeSheetResponseDtos = dailyTimeSheetRepository
                .findByEmployeeCodeAndWorkDateBetween(employeeCode, weekStart, weekEnd)
                .stream()
                .map(d -> {
                    DailyTimeSheetResponseDto dto = new DailyTimeSheetResponseDto();
                    dto.setEmployeeCode(d.getEmployeeCode());
                    dto.setTimesheetYear(d.getTimesheetYear());
                    dto.setTimesheetMonth(d.getTimesheetMonth());
                    dto.setWorkDate(d.getWorkDate());
                    dto.setEntryType(d.getEntryType());
                    dto.setProjectCode(d.getProjectCode());
                    dto.setDescription(d.getDescription());
                    dto.setHoursSpent(d.getHoursSpent());
                    dto.setModifiedByManager(d.getModifiedByManager());
                    dto.setStatus(summary.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        DailyTimesheetResponseWithStatus dailyTimeSheetResponseWithStatus = new DailyTimesheetResponseWithStatus();
        dailyTimeSheetResponseWithStatus.setDailyTimeSheetResponseDtos(dailyTimeSheetResponseDtos);
        dailyTimeSheetResponseWithStatus.setStatus(summary.getStatus());
        dailyTimeSheetResponseWithStatus.setManagerComment(summary.getManagerComment());
        return dailyTimeSheetResponseWithStatus;
    }




    @Transactional
    public void saveTimesheetSummary(TimesheetSummaryDto dto) {
        Date weekStart = dto.getWeekStart();
        Date weekEnd = Date.valueOf(weekStart.toLocalDate().plusDays(6));

        List<DailyTimeSheet> entries = dailyTimeSheetRepository
                .findByEmployeeCodeAndWorkDateBetween(dto.getEmployeeCode(), weekStart, weekEnd);

        double total = entries.stream().mapToDouble(DailyTimeSheet::getHoursSpent).sum();

        TimesheetSummaryId summaryId = new TimesheetSummaryId(
                dto.getEmployeeCode(),
                dto.getTimesheetYear(),
                dto.getTimesheetMonth(),
                dto.getWeekStart()
        );

        TimesheetSummary summary = new TimesheetSummary();
        summary.setId(summaryId);
        summary.setTotalHours(total);
        summary.setStatus(TimeSheetStatus.DRAFT);
        summary.setSubmittedDate(new Timestamp(System.currentTimeMillis()));

        timesheetSummaryRepository.save(summary);
    }

    @Override
    @Transactional
    public String approveAllUnderManagerForWeek(ManagerApprovalRequestDto approvalRequest) throws TimeSheetException {
        // 1. Get all employees under this manager
        ResponseEntity<List<UserIdentityDto>> response = identityServiceClient
                .getEmployeesUnderManager(approvalRequest.getManagerCode());

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new TimeSheetException(ErrorCode.FAILED_TO_FETCH_DETAILS, ErrorMessage.USERID_EXTRACTION_FAILED);
        } else {
            response.getBody();
        }

        List<String> employeeCodes = response.getBody().stream()
                .map(UserIdentityDto::getEmployeeCode)
                .toList();

        // 2. Find all timesheet summaries for these employees for the specified week
        List<TimesheetSummary> summaries = timesheetSummaryRepository
                .findByIdEmployeeCodeInAndIdWeekStartAndIdTimesheetYearAndIdTimesheetMonth(
                        employeeCodes,
                        approvalRequest.getWeekStart(),
                        approvalRequest.getTimesheetYear(),
                        approvalRequest.getTimesheetMonth()
                );

        // 3. Approve each timesheet
        summaries.forEach(summary -> {
            summary.setStatus(TimeSheetStatus.APPROVED);
            summary.setApprovedBy(approvalRequest.getManagerCode());
            summary.setManagerComment(approvalRequest.getComment());
        });

        timesheetSummaryRepository.saveAll(summaries);

        return MessageConstants.APPROVED_ALL_TIMESHEETS_FOR_WEEK;
    }


    private WeeklyTimeSheetEntryDto toWeeklyEntryDto(TimesheetSummary s) {
        LocalDate start = toLocalDate(s.getId().getWeekStart());
        String startStr = start.toString();
        String endStr = start.plusDays(6).toString();

        return new WeeklyTimeSheetEntryDto(
                startStr,
                endStr,
                s.getTotalHours(),
                s.getStatus().name()
        );
    }

    @Override
    public TimeSheetStatus getWeeklyStatus(String employeeCode, Date weekStart) {

        TimesheetSummary summary = timesheetSummaryRepository
                .findByIdEmployeeCodeAndIdWeekStart(employeeCode, weekStart)
                .orElseThrow(() -> new TimeSheetException(
                        NOT_FOUND_ERROR,
                        String.format(ErrorMessage.TIMESHEET_SUMMARY_NOT_FOUND, employeeCode, weekStart)
                ));
        return summary.getStatus();
    }

    @Override
    public List<TimesheetMatrixRowResponseDto> getEmployeeTimesheet(String employeeCode, Integer year, Integer month) {
        List<DailyTimeSheet> entries = dailyTimeSheetRepository
                .findByEmployeeCodeAndTimesheetYearAndTimesheetMonth(employeeCode, year, month);

        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (DailyTimeSheet entry : entries) {
            LocalDate workDate = entry.getWorkDate().toLocalDate();

            LocalDate startOfWeek = workDate.with(DayOfWeek.MONDAY);
            LocalDate endOfWeek = workDate.with(DayOfWeek.SUNDAY);
            String weekLabel = formatter.format(startOfWeek) + " - " + formatter.format(endOfWeek);

            String rowKey = entry.getEntryType() == EntryType.PROJECT
                    ? getProjectName(entry.getProjectCode())
                    : entry.getEntryType().name();

            matrix.computeIfAbsent(rowKey, k -> new LinkedHashMap<>());
            Map<String, Double> weekMap = matrix.get(rowKey);
            weekMap.put(weekLabel, weekMap.getOrDefault(weekLabel, 0.0) + entry.getHoursSpent());
        }

        return matrix.entrySet().stream()
                .map(entry -> new TimesheetMatrixRowResponseDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    public String getProjectName(String projectCode) {
        Optional<Project> project = projectRepository.findById(projectCode);
        return project.map(Project::getTitle) // assuming getName() returns the project name
                .orElse("Unknown Project"); // fallback if project not found
    }



    private LocalDate toLocalDate(Date date) {
        if (date == null){
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    @Transactional
    public PagedResponse<ManagerApprovalRequestDto> getEmployeesTimesheetUnderManager(
            String managerCode,
            int year,
            int month,
            int offset,
            int limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        if (offset < 0){
            offset = 0;
        }
        if (limit <= 0){
            limit = 10;
        }
        int page = offset / limit;

        List<Specification<TimesheetSummary>> extraSpecs = new ArrayList<>();

        if (filters != null) {
            for (Iterator<FilterRequest> it = filters.iterator(); it.hasNext(); ) {
                FilterRequest f = it.next();
                // In getEmployeesTimesheetUnderManager and getEmployeesTimesheet methods:
                switch (f.getField()) {
                    case "year", TIMESHEET_YEAR, ID_TIMESHEET_YEAR -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(TIMESHEET_YEAR),
                                        Integer.valueOf(f.getValue())));
                        it.remove();
                    }
                    case "month", TIMESHEET_MONTH, ID_TIMESHEET_MONTH -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(TIMESHEET_MONTH),
                                        Integer.valueOf(f.getValue())));
                        it.remove();
                    }
                    case EMPLOYEE_CODE, ID_EMPLOYEE_CODE -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(EMPLOYEE_CODE),
                                        f.getValue()));
                        it.remove();
                    }
                    case WEEK_START, ID_WEEK_START -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(WEEK_START),
                                        java.sql.Date.valueOf(f.getValue())));
                        it.remove();
                    }
                }
            }
        }

        // Get employees under manager
        List<String> employeeCodes = Optional.of(
                        identityServiceClient.getEmployeesUnderManager(managerCode).getBody())
                .orElse(Collections.emptyList())
                .stream()
                .map(UserIdentityDto::getEmployeeCode)
                .toList();

        if (employeeCodes.isEmpty()) {
            return new PagedResponse<>(List.of(), page, limit, 0);
        }

        // Calculate paged employees
        int fromIndex = Math.min(offset, employeeCodes.size());
        int toIndex = Math.min(fromIndex + limit, employeeCodes.size());
        List<String> pagedEmployeeCodes = employeeCodes.subList(fromIndex, toIndex);

        if (pagedEmployeeCodes.isEmpty()) {
            return new PagedResponse<>(List.of(), page, limit, employeeCodes.size());
        }

        // Final spec for paged employees
        Specification<TimesheetSummary> yearMonthSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("id").get(TIMESHEET_YEAR), year),
                cb.equal(root.get("id").get(TIMESHEET_MONTH), month)
        );

        Specification<TimesheetSummary> employeeSpec = (root, q, cb) ->
                root.get("id").get(EMPLOYEE_CODE).in(pagedEmployeeCodes);

        Specification<TimesheetSummary> dynamicSpec =
                new FilterSpecificationBuilder<TimesheetSummary>().build(filters);

        Specification<TimesheetSummary> finalSpec = Specification
                .where(yearMonthSpec)
                .and(employeeSpec)
                .and(dynamicSpec);

        for (Specification<TimesheetSummary> sp : extraSpecs) {
            finalSpec = finalSpec.and(sp);
        }

        List<TimesheetSummary> summaries =
                timesheetSummaryRepository.findAll(finalSpec, Sort.by(ID_EMPLOYEE_CODE, ID_WEEK_START));

        if (summaries.isEmpty()) {
            throw new TimeSheetException(
                    NOT_FOUND_ERROR,
                    ErrorMessage.NO_TIMESHEET_SUMMARIES_FOUND
            );
        }

        List<ManagerApprovalRequestDto> content = summaries.stream()
                .map(s -> {
                    ManagerApprovalRequestDto dto = new ManagerApprovalRequestDto();
                    dto.setEmployeeCode(s.getId().getEmployeeCode());
                    dto.setTimesheetYear(s.getId().getTimesheetYear());
                    dto.setTimesheetMonth(s.getId().getTimesheetMonth());
                    dto.setWeekStart(s.getId().getWeekStart());
                    dto.setHours(s.getTotalHours());
                    dto.setManagerCode(managerCode);
                    dto.setApprove(s.getStatus() == TimeSheetStatus.APPROVED);
                    return dto;
                })
                .toList();

        // totalElements = employeeCodes.size()
        return new PagedResponse<>(
                content,
                page,
                limit,
                employeeCodes.size()
        );
    }

    @Override
    @Transactional
    public PagedResponse<ManagerApprovalRequestDto> getEmployeesTimesheet(
            int year,
            int month,
            int offset,
            int limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        if (offset < 0){
            offset = 0;
        }
        if (limit <= 0){
            limit = 10;
        }
        int page = offset / limit;

        List<Specification<TimesheetSummary>> extraSpecs = new ArrayList<>();

        if (filters != null) {
            for (Iterator<FilterRequest> it = filters.iterator(); it.hasNext(); ) {
                FilterRequest f = it.next();
                switch (f.getField()) {
                    case "year", TIMESHEET_YEAR, ID_TIMESHEET_YEAR -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(TIMESHEET_YEAR),
                                        Integer.valueOf(f.getValue())));
                        it.remove();
                    }
                    case "month", TIMESHEET_MONTH, ID_TIMESHEET_MONTH -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(TIMESHEET_MONTH),
                                        Integer.valueOf(f.getValue())));
                        it.remove();
                    }
                    case EMPLOYEE_CODE, ID_EMPLOYEE_CODE -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(EMPLOYEE_CODE),
                                        f.getValue()));
                        it.remove();
                    }
                    case WEEK_START, ID_WEEK_START -> {
                        extraSpecs.add((root, q, cb) ->
                                cb.equal(root.get("id").get(WEEK_START),
                                        java.sql.Date.valueOf(f.getValue())));
                        it.remove();
                    }
                }
            }
        }

        List<String> employeeCodes = Optional.ofNullable(identityServiceClient.getAllUsersList().getBody())
                .orElse(Collections.emptyList())
                .stream()
                .map(m -> m.get("employeeCode"))
                .toList();

        if (employeeCodes.isEmpty()) {
            return new PagedResponse<>(List.of(), page, limit, 0);
        }

        int fromIndex = Math.min(offset, employeeCodes.size());
        int toIndex = Math.min(fromIndex + limit, employeeCodes.size());
        List<String> pagedEmployeeCodes = employeeCodes.subList(fromIndex, toIndex);

        if (pagedEmployeeCodes.isEmpty()) {
            return new PagedResponse<>(List.of(), page, limit, employeeCodes.size());
        }

        Specification<TimesheetSummary> yearMonthSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("id").get(TIMESHEET_YEAR), year),
                cb.equal(root.get("id").get(TIMESHEET_MONTH), month)
        );

        Specification<TimesheetSummary> employeeSpec = (root, q, cb) ->
                root.get("id").get(EMPLOYEE_CODE).in(pagedEmployeeCodes);

        Specification<TimesheetSummary> dynamicSpec =
                new FilterSpecificationBuilder<TimesheetSummary>().build(filters);

        Specification<TimesheetSummary> finalSpec = Specification
                .where(yearMonthSpec)
                .and(employeeSpec)
                .and(dynamicSpec);

        for (Specification<TimesheetSummary> sp : extraSpecs) {
            finalSpec = finalSpec.and(sp);
        }

        List<TimesheetSummary> summaries =
                timesheetSummaryRepository.findAll(finalSpec, Sort.by(ID_EMPLOYEE_CODE, ID_WEEK_START));

        if (summaries.isEmpty()) {
            throw new TimeSheetException(
                    NOT_FOUND_ERROR,
                    ErrorMessage.NO_TIMESHEET_SUMMARIES_FOUND
            );
        }

        List<ManagerApprovalRequestDto> content = summaries.stream()
                .map(s -> {
                    ManagerApprovalRequestDto dto = new ManagerApprovalRequestDto();
                    dto.setEmployeeCode(s.getId().getEmployeeCode());
                    dto.setTimesheetYear(s.getId().getTimesheetYear());
                    dto.setTimesheetMonth(s.getId().getTimesheetMonth());
                    dto.setWeekStart(s.getId().getWeekStart());
                    dto.setHours(s.getTotalHours());
                    dto.setApprove(s.getStatus() == TimeSheetStatus.APPROVED);
                    return dto;
                })
                .toList();

        return new PagedResponse<>(
                content,
                page,
                limit,
                employeeCodes.size()
        );
    }



}