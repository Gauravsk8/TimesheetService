package com.example.timesheet.service;

import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.DailyTimesheetDto;
import com.example.timesheet.dto.request.DailyTimesheetRequestDto;
import com.example.timesheet.dto.request.ManagerApprovalRequestDto;
import com.example.timesheet.dto.request.TimesheetSummaryDto;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.EntryType;
import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.keys.TimesheetSummaryId;
import com.example.timesheet.models.DailyTimeSheet;
import com.example.timesheet.models.Project;
import com.example.timesheet.models.TimesheetSummary;
import com.example.timesheet.repository.DailyTimeSheetRepository;
import com.example.timesheet.repository.ProjectEmployeeRepository;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.repository.TimesheetSummaryRepository;
import com.example.timesheet.service.serviceimpl.TimesheetServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    /* ───────── duplicated-literal constants ───────── */
    private static final String PROJ_CODE = "P01";
    private static final String EMP1 = "EMP1";
    private static final String EMP2 = "EMP2";
    private static final String EMP3 = "EMP3";
    private static final String MGR1 = "MGR1";

    /* ───────── mocks & SUT ───────── */
    @Mock private DailyTimeSheetRepository dailyRepo;
    @Mock private TimesheetSummaryRepository summaryRepo;
    @Mock private ProjectRepository projectRepo;
    @Mock private ProjectEmployeeRepository projectEmpRepo;
    @Mock private IdentityServiceClient identityClient;
    @InjectMocks private TimesheetServiceImpl service;

    /* ───────── reusable data ───────── */
    private Date monday;
    private Date sunday;
    private Project project;
    private DailyTimeSheet dayEntity;
    private TimesheetSummary summaryDraft;

    private TimesheetSummary makeSummary(String ec, Date weekStart,
                                         double hours, TimeSheetStatus st) {
        TimesheetSummaryId id = new TimesheetSummaryId(ec, 2025, 5, weekStart);
        TimesheetSummary ts = new TimesheetSummary();
        ts.setId(id);
        ts.setTotalHours(hours);
        ts.setStatus(st);
        return ts;
    }

    @BeforeEach
    void setUp() {
        LocalDate mon = LocalDate.of(2025, 5, 19);
        monday = Date.valueOf(mon);
        sunday = Date.valueOf(mon.plusDays(6));

        project = Project.builder()
                .projectCode(PROJ_CODE)
                .title("Apollo")
                .isActive(true)
                .build();

        dayEntity = new DailyTimeSheet();
        dayEntity.setEmployeeCode(EMP1);
        dayEntity.setTimesheetYear(2025);
        dayEntity.setTimesheetMonth(5);
        dayEntity.setWorkDate(monday);
        dayEntity.setEntryType(EntryType.PROJECT);
        dayEntity.setProjectCode(PROJ_CODE);
        dayEntity.setHoursSpent(4.0);

        TimesheetSummaryId sid = new TimesheetSummaryId(EMP1, 2025, 5, monday);
        summaryDraft = new TimesheetSummary();
        summaryDraft.setId(sid);
        summaryDraft.setStatus(TimeSheetStatus.DRAFT);
        summaryDraft.setTotalHours(4.0);
    }

    /* ───────────────────── saveDailyEntry ───────────────────── */
    @Nested class SaveDailyEntry {

        @Test
        void createsNewEntry_andPersistsSummary() {
            when(projectEmpRepo.existsById(new ProjectEmployeeId(PROJ_CODE, EMP1))).thenReturn(true);
            when(projectRepo.findById(PROJ_CODE)).thenReturn(java.util.Optional.of(project));
            when(dailyRepo.findByEmployeeCodeAndTimesheetYearAndTimesheetMonthAndWorkDateAndEntryTypeAndProjectCode(
                    anyString(), anyInt(), anyInt(), any(), any(), anyString()))
                    .thenReturn(java.util.Optional.empty());

            DailyTimesheetRequestDto d = new DailyTimesheetRequestDto();
            d.setEmployeeCode(EMP1);
            d.setTimesheetYear(2025);
            d.setTimesheetMonth(5);
            d.setWorkDate(monday);
            d.setEntryType(EntryType.PROJECT);
            d.setProjectCode(PROJ_CODE);
            d.setHoursSpent(6.0);
            d.setDescription("Dev work");

            DailyTimesheetDto dto = new DailyTimesheetDto();
            dto.setDailyEntry(List.of(d));
            dto.setEmployeeCode(EMP1);
            dto.setTimesheetYear(2025);
            dto.setTimesheetMonth(5);
            dto.setWeekStart(monday);

            String msg = service.saveDailyEntry(dto);

            verify(dailyRepo).save(any(DailyTimeSheet.class));
            verify(summaryRepo).save(any(TimesheetSummary.class));
            assertThat(msg).isEqualTo(MessageConstants.DAILY_TIMESHEET_SAVED);
        }

        @Test
        void throws_whenAssignmentMissing() {
            when(projectEmpRepo.existsById(any(ProjectEmployeeId.class))).thenReturn(false);

            DailyTimesheetRequestDto bad = new DailyTimesheetRequestDto();
            bad.setEmployeeCode(EMP1);
            bad.setTimesheetYear(2025);
            bad.setTimesheetMonth(5);
            bad.setWorkDate(monday);
            bad.setEntryType(EntryType.PROJECT);
            bad.setProjectCode("BAD");
            bad.setHoursSpent(2.0);

            DailyTimesheetDto wrap = new DailyTimesheetDto();
            wrap.setDailyEntry(List.of(bad));
            wrap.setEmployeeCode(EMP1);
            wrap.setTimesheetYear(2025);
            wrap.setTimesheetMonth(5);
            wrap.setWeekStart(monday);

            assertThatThrownBy(() -> service.saveDailyEntry(wrap))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────── submitTimesheetSummary ───────────────────── */
    @Nested class SubmitTimesheetSummary {

        @Test
        void changesStatusFromDraftToSubmitted() {
            when(summaryRepo.findById(any(TimesheetSummaryId.class)))
                    .thenReturn(java.util.Optional.of(summaryDraft));
            when(summaryRepo.save(any())).then(i -> i.getArgument(0));

            TimesheetSummaryDto dto = new TimesheetSummaryDto();
            dto.setEmployeeCode(EMP1);
            dto.setTimesheetYear(2025);
            dto.setTimesheetMonth(5);
            dto.setWeekStart(monday);

            String out = service.submitTimesheetSummary(dto);

            verify(summaryRepo).save(argThat(s ->
                    ((TimesheetSummary) s).getStatus() == TimeSheetStatus.SUBMITTED));
            assertThat(out).contains("submitted").contains(EMP1);
        }

        @Test
        void refuses_whenNotDraft() {
            summaryDraft.setStatus(TimeSheetStatus.APPROVED);
            when(summaryRepo.findById(any())).thenReturn(java.util.Optional.of(summaryDraft));

            TimesheetSummaryDto dto = new TimesheetSummaryDto();
            dto.setEmployeeCode(EMP1);
            dto.setTimesheetYear(2025);
            dto.setTimesheetMonth(5);
            dto.setWeekStart(monday);

            assertThatThrownBy(() -> service.submitTimesheetSummary(dto))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    /* ───────────────────── approveOrRejectWeekly ───────────────────── */
    @Nested class ApproveOrRejectWeekly {

        @Test
        void approvesAndUpdatesDailySheets() {
            when(summaryRepo.findById(any())).thenReturn(java.util.Optional.of(summaryDraft));
            when(dailyRepo.findByEmployeeCodeAndWorkDateBetween(EMP1, monday, sunday))
                    .thenReturn(List.of(dayEntity));

            DailyTimesheetRequestDto mod = new DailyTimesheetRequestDto();
            mod.setEmployeeCode(EMP1);
            mod.setWorkDate(monday);
            mod.setEntryType(EntryType.PROJECT);
            mod.setProjectCode(PROJ_CODE);
            mod.setHoursSpent(8.0);
            mod.setTimesheetYear(2025);
            mod.setTimesheetMonth(5);

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setEmployeeCode(EMP1);
            req.setTimesheetYear(2025);
            req.setTimesheetMonth(5);
            req.setWeekStart(monday);
            req.setDailyTimeSheetRequests(List.of(mod));
            req.setApprove(true);
            req.setManagerCode(MGR1);
            req.setComment("Looks good");

            String msg = service.approveOrRejectWeekly(req);

            verify(dailyRepo, atLeastOnce()).save(any(DailyTimeSheet.class));
            verify(summaryRepo).save(argThat(
                    s -> ((TimesheetSummary) s).getStatus() == TimeSheetStatus.APPROVED));
            assertThat(msg).contains("approved").contains(MGR1);
        }
    }

    /* ───────────────────── approveAllUnderManagerForWeek ───────────────────── */
    @Nested class ApproveAllUnderManager {

        @Test
        void bulkApproves_andReturnsCount() {
            UserIdentityDto emp = new UserIdentityDto();
            emp.setEmployeeCode(EMP1);
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.ok(List.of(emp)));
            when(summaryRepo.findByIdEmployeeCodeInAndIdWeekStartAndIdTimesheetYearAndIdTimesheetMonth(
                    any(), eq(monday), eq(2025), eq(5)))
                    .thenReturn(List.of(summaryDraft));

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setManagerCode(MGR1);
            req.setWeekStart(monday);
            req.setTimesheetYear(2025);
            req.setTimesheetMonth(5);
            req.setComment("auto");

            String msg = service.approveAllUnderManagerForWeek(req);

            verify(summaryRepo).saveAll(argThat((Iterable<TimesheetSummary> it) -> {
                for (TimesheetSummary s : it) {
                    if (s.getStatus() != TimeSheetStatus.APPROVED) {
                        return false;
                    }
                }
                return true;
            }));

            assertThat(msg).isEqualTo(MessageConstants.APPROVED_ALL_TIMESHEETS_FOR_WEEK);
        }

        @Test
        void fails_whenIdentityServiceNot2xx() {
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.status(500).build());

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setManagerCode(MGR1);
            req.setWeekStart(monday);
            req.setTimesheetYear(2025);
            req.setTimesheetMonth(5);

            assertThatThrownBy(() -> service.approveAllUnderManagerForWeek(req))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FAILED_TO_FETCH_DETAILS);
        }
    }

    /* ───────────────────── getDailyEntries ───────────────────── */
    @Test
    void getDailyEntries_returnsDtosWithStatus() {
        when(summaryRepo.findByIdEmployeeCodeAndIdWeekStart(EMP1, monday))
                .thenReturn(java.util.Optional.of(summaryDraft));
        when(dailyRepo.findByEmployeeCodeAndWorkDateBetween(EMP1, monday, sunday))
                .thenReturn(List.of(dayEntity));

        var resp = service.getDailyEntries(EMP1, monday);

        assertThat(resp.getStatus()).isEqualTo(TimeSheetStatus.DRAFT);
        assertThat(resp.getDailyTimeSheetResponseDtos()).hasSize(1);
    }

    /* ───────────────────── getWeeklyStatus ───────────────────── */
    @Test
    void getWeeklyStatus_returnsStatus() {
        when(summaryRepo.findByIdEmployeeCodeAndIdWeekStart(EMP1, monday))
                .thenReturn(java.util.Optional.of(summaryDraft));

        assertThat(service.getWeeklyStatus(EMP1, monday))
                .isEqualTo(TimeSheetStatus.DRAFT);
    }

    /* ───────────────────── getEmployeeTimesheet ───────────────────── */
    @Test
    void getEmployeeTimesheet_buildsMatrix() {
        DailyTimeSheet tuesday = new DailyTimeSheet();
        tuesday.setEmployeeCode(EMP1);
        tuesday.setTimesheetYear(2025);
        tuesday.setTimesheetMonth(5);
        tuesday.setWorkDate(Date.valueOf(LocalDate.of(2025, 5, 20)));
        tuesday.setEntryType(EntryType.PROJECT);
        tuesday.setHoursSpent(2.0);

        when(dailyRepo.findByEmployeeCodeAndTimesheetYearAndTimesheetMonth(EMP1, 2025, 5))
                .thenReturn(List.of(dayEntity, tuesday));
        when(projectRepo.findById(PROJ_CODE)).thenReturn(java.util.Optional.of(project));

        var matrix = service.getEmployeeTimesheet(EMP1, 2025, 5);

        assertThat(matrix).hasSize(2);
        assertThat(matrix.get(0).getWeeklyHours()).isNotEmpty();
    }

    /* ───────────────────── Bean-validation smoke tests ───────────────────── */
    @Test
    void validation_dailyRequest_violationOnBadHours() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            Validator v = f.getValidator();
            DailyTimesheetRequestDto bad = new DailyTimesheetRequestDto();
            bad.setEmployeeCode(EMP1);
            bad.setTimesheetYear(2025);
            bad.setTimesheetMonth(5);
            bad.setWorkDate(monday);
            bad.setEntryType(EntryType.PROJECT);
            bad.setHoursSpent(30.0);

            assertThat(v.validate(bad)).hasSize(1);
        }
    }

    /* ───────────────────── getEmployeesTimesheetUnderManager ───────────────────── */
    @Nested
    class GetEmployeesTimesheetUnderManager {

        @Test
        void returnsSortedApprovalDtos() {
            UserIdentityDto u1 = new UserIdentityDto();
            u1.setEmployeeCode(EMP2);
            UserIdentityDto u2 = new UserIdentityDto();
            u2.setEmployeeCode(EMP1);
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.ok(List.of(u1, u2)));

            when(summaryRepo.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(
                            makeSummary(EMP1, monday, 40, TimeSheetStatus.APPROVED),
                            makeSummary(EMP2, sunday, 15, TimeSheetStatus.SUBMITTED)
                    ));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager(
                            MGR1, 2025, 5, 0, 10, emptyList(), emptyList());

            assertThat(out.getContent()).extracting(ManagerApprovalRequestDto::getEmployeeCode)
                    .containsExactly(EMP1, EMP2);
            assertThat(out.getContent().get(0).isApprove()).isTrue();
            assertThat(out.getContent().get(1).isApprove()).isFalse();
            assertThat(out.getTotalElements()).isEqualTo(2);
        }

        @Test
        void returnsEmptyWhenNoEmployeesUnderManager() {
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.ok(Collections.emptyList()));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager(
                            MGR1, 2025, 5, 0, 10, emptyList(), emptyList());

            assertThat(out.getContent()).isEmpty();
            verifyNoInteractions(summaryRepo);
        }

        @Test
        void throwsWhenNoTimesheetsFound() {
            UserIdentityDto u = new UserIdentityDto();
            u.setEmployeeCode(EMP1);
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.ok(List.of(u)));

            when(summaryRepo.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.getEmployeesTimesheetUnderManager(
                    MGR1, 2025, 5, 0, 10, emptyList(), emptyList()))
                    .isInstanceOf(TimeSheetException.class)
                    .hasMessageContaining(ErrorMessage.NO_TIMESHEET_SUMMARIES_FOUND);
        }

        @Test
        void appliesOffsetLimitCorrectly() {
            UserIdentityDto u1 = new UserIdentityDto();
            u1.setEmployeeCode(EMP1);
            UserIdentityDto u2 = new UserIdentityDto();
            u2.setEmployeeCode(EMP2);
            UserIdentityDto u3 = new UserIdentityDto();
            u3.setEmployeeCode(EMP3);
            when(identityClient.getEmployeesUnderManager(MGR1))
                    .thenReturn(ResponseEntity.ok(List.of(u1, u2, u3)));

            when(summaryRepo.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(
                            makeSummary(EMP3, sunday, 20, TimeSheetStatus.SUBMITTED)));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager(
                            MGR1, 2025, 5, 2, 1, emptyList(), emptyList());

            assertThat(out.getContent()).singleElement()
                    .extracting(ManagerApprovalRequestDto::getEmployeeCode)
                    .isEqualTo(EMP3);
            assertThat(out.getPage()).isEqualTo(2);
            assertThat(out.getSize()).isEqualTo(1);
            assertThat(out.getTotalElements()).isEqualTo(3);
            assertThat(out.isLast()).isTrue();
        }
    }

    /* ───────────────────── getEmployeesTimesheetAll ───────────────────── */
    @Nested
    class GetEmployeesTimesheetAll {

        private final int year = 2025;
        private final int month = 5;
        private final int offset = 0;
        private final int limit = 10;

        private final List<FilterRequest> noFilters = emptyList();

        private final List<SortRequest> empCodeAsc;

        {
            SortRequest sr = new SortRequest();
            sr.setField("employeeCode");
            sr.setDirection("asc");
            empCodeAsc = List.of(sr);
        }

        @BeforeEach
        void mockIdentityUsers() {
            java.util.Map<String, String> m1 = java.util.Map.of("employeeCode", EMP3);
            java.util.Map<String, String> m2 = java.util.Map.of("employeeCode", EMP1);
            when(identityClient.getAllUsersList())
                    .thenReturn(ResponseEntity.ok(List.of(m1, m2)));
        }

        @Test
        void buildsPagedResponseAndSorts() {
            when(summaryRepo.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(
                            makeSummary(EMP1, monday, 10, TimeSheetStatus.DRAFT),
                            makeSummary(EMP3, sunday, 20, TimeSheetStatus.APPROVED)
                    ));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheet(
                            year, month, offset, limit, noFilters, empCodeAsc);

            assertThat(out.getContent()).extracting(ManagerApprovalRequestDto::getEmployeeCode)
                    .containsExactly(EMP1, EMP3);
            assertThat(out.getContent().get(0).isApprove()).isFalse();
            assertThat(out.getContent().get(1).getHours()).isEqualTo(20);
        }

        @Test
        void emptyWhenIdentityReturnsNone() {
            when(identityClient.getAllUsersList())
                    .thenReturn(ResponseEntity.ok(Collections.emptyList()));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheet(
                            year, month, offset, limit, noFilters, empCodeAsc);

            assertThat(out.getContent()).isEmpty();
            verifyNoInteractions(summaryRepo);
        }
    }
}
