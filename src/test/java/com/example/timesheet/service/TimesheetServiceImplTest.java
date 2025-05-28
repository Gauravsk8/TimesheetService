package com.example.timesheet.service;

import com.example.timesheet.Repository.*;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.dto.request.*;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.EntryType;
import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.keys.TimesheetSummaryId;
import com.example.timesheet.models.*;
import com.example.timesheet.service.Serviceimpl.TimesheetServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import org.springframework.data.domain.Sort.Direction;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    // ──────────────────  mocks & SUT ──────────────────
    @Mock private DailyTimeSheetRepository   dailyRepo;
    @Mock private TimesheetSummaryRepository summaryRepo;
    @Mock private ProjectRepository          projectRepo;
    @Mock private ProjectEmployeeRepository  projectEmpRepo;
    @Mock private IdentityServiceClient      identityClient;
    @InjectMocks private TimesheetServiceImpl service;

    // ────────────────── reusable data ──────────────────
    private Date       monday;
    private Date       sunday;
    private Project    project;
    private DailyTimeSheet dayEntity;
    private TimesheetSummary summaryDraft;

    private TimesheetSummary makeSummary(String emp, Date weekStart,
                                         double hours, TimeSheetStatus st) {
        TimesheetSummaryId id = new TimesheetSummaryId(emp, 2025, 5, weekStart);
        TimesheetSummary ts  = new TimesheetSummary();
        ts.setId(id);
        ts.setTotalHours(hours);
        ts.setStatus(st);
        return ts;
    }

    @BeforeEach
    void setUp() {
        LocalDate mon = LocalDate.of(2025, 5, 19);   // Monday
        monday  = Date.valueOf(mon);
        sunday  = Date.valueOf(mon.plusDays(6));

        project = Project.builder()
                .projectCode("P01")
                .title("Apollo")
                .isActive(true)
                .build();

        dayEntity = new DailyTimeSheet();
        dayEntity.setEmployeeCode("EMP1");
        dayEntity.setTimesheetYear(2025);
        dayEntity.setTimesheetMonth(5);
        dayEntity.setWorkDate(monday);
        dayEntity.setEntryType(EntryType.PROJECT);
        dayEntity.setProjectCode("P01");
        dayEntity.setHoursSpent(4.0);

        TimesheetSummaryId sid = new TimesheetSummaryId("EMP1", 2025, 5, monday);
        summaryDraft = new TimesheetSummary();
        summaryDraft.setId(sid);
        summaryDraft.setStatus(TimeSheetStatus.DRAFT);
        summaryDraft.setTotalHours(4.0);

    }

    // ─────────────────────────────── saveDailyEntry ───────────────────────────────
    @Nested class SaveDailyEntry {

        @Test
        void createsNewEntry_andPersistsSummary() {
            // repo responses
            when(projectEmpRepo.existsById(new ProjectEmployeeId("P01", "EMP1"))).thenReturn(true);
            when(projectRepo.findById("P01")).thenReturn(Optional.of(project));
            when(dailyRepo.findByEmployeeCodeAndTimesheetYearAndTimesheetMonthAndWorkDateAndEntryTypeAndProjectCode(
                    any(), anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            DailyTimesheetRequestDto d = new DailyTimesheetRequestDto();
            d.setEmployeeCode("EMP1"); d.setTimesheetYear(2025); d.setTimesheetMonth(5);
            d.setWorkDate(monday); d.setEntryType(EntryType.PROJECT); d.setProjectCode("P01");
            d.setHoursSpent(6.0); d.setDescription("Dev work");

            DailyTimesheetDto dto = new DailyTimesheetDto();
            dto.setDailyEntry(List.of(d));
            dto.setEmployeeCode("EMP1");
            dto.setTimesheetYear(2025); dto.setTimesheetMonth(5); dto.setWeekStart(monday);

            String msg = service.saveDailyEntry(dto);

            verify(dailyRepo).save(any(DailyTimeSheet.class));
            verify(summaryRepo).save(any(TimesheetSummary.class));
            assertThat(msg).isEqualTo(MessageConstants.DAILY_TIMESHEET_SAVED);
        }

        @Test
        void throws_whenAssignmentMissing() {
            when(projectEmpRepo.existsById(any(ProjectEmployeeId.class))).thenReturn(false);

            DailyTimesheetRequestDto bad = new DailyTimesheetRequestDto();
            bad.setEmployeeCode("EMP1"); bad.setTimesheetYear(2025); bad.setTimesheetMonth(5);
            bad.setWorkDate(monday); bad.setEntryType(EntryType.PROJECT); bad.setProjectCode("BAD");
            bad.setHoursSpent(2.0);

            DailyTimesheetDto wrap = new DailyTimesheetDto();
            wrap.setDailyEntry(List.of(bad));
            wrap.setEmployeeCode("EMP1");
            wrap.setTimesheetYear(2025); wrap.setTimesheetMonth(5); wrap.setWeekStart(monday);

            assertThatThrownBy(() -> service.saveDailyEntry(wrap))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    // ─────────────────────────────── submitTimesheetSummary ───────────────────────────────
    @Nested class SubmitTimesheetSummary {

        @Test
        void changesStatusFromDraftToSubmitted() {
            when(summaryRepo.findById(any(TimesheetSummaryId.class)))
                    .thenReturn(Optional.of(summaryDraft));
            when(summaryRepo.save(any())).then(i -> i.getArgument(0));

            TimesheetSummaryDto dto = new TimesheetSummaryDto();
            dto.setEmployeeCode("EMP1"); dto.setTimesheetYear(2025);
            dto.setTimesheetMonth(5); dto.setWeekStart(monday);

            String out = service.submitTimesheetSummary(dto);

            verify(summaryRepo).save(argThat(s ->
                    ((TimesheetSummary) s).getStatus() == TimeSheetStatus.SUBMITTED));
            assertThat(out).contains("submitted").contains("EMP1");
        }

        @Test
        void refuses_whenNotDraft() {
            summaryDraft.setStatus(TimeSheetStatus.APPROVED);
            when(summaryRepo.findById(any())).thenReturn(Optional.of(summaryDraft));

            TimesheetSummaryDto dto = new TimesheetSummaryDto();
            dto.setEmployeeCode("EMP1"); dto.setTimesheetYear(2025);
            dto.setTimesheetMonth(5); dto.setWeekStart(monday);

            assertThatThrownBy(() -> service.submitTimesheetSummary(dto))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    // ─────────────────────────────── approveOrRejectWeekly ───────────────────────────────
    @Nested class ApproveOrRejectWeekly {

        @Test
        void approvesAndUpdatesDailySheets() {
            when(summaryRepo.findById(any())).thenReturn(Optional.of(summaryDraft));
            when(dailyRepo.findByEmployeeCodeAndWorkDateBetween("EMP1", monday, sunday))
                    .thenReturn(List.of(dayEntity));

            // incoming modification (change hours)
            DailyTimesheetRequestDto mod = new DailyTimesheetRequestDto();
            mod.setEmployeeCode("EMP1"); mod.setWorkDate(monday); mod.setEntryType(EntryType.PROJECT);
            mod.setProjectCode("P01"); mod.setHoursSpent(8.0); mod.setTimesheetYear(2025); mod.setTimesheetMonth(5);

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setEmployeeCode("EMP1");
            req.setTimesheetYear(2025); req.setTimesheetMonth(5); req.setWeekStart(monday);
            req.setDailyTimeSheetRequests(List.of(mod));
            req.setApprove(true); req.setManagerCode("MGR1"); req.setComment("Looks good");

            String msg = service.approveOrRejectWeekly(req);

            verify(dailyRepo, atLeastOnce()).save(any(DailyTimeSheet.class));
            verify(summaryRepo).save(argThat(s ->
                    ((TimesheetSummary)s).getStatus()==TimeSheetStatus.APPROVED));
            assertThat(msg).contains("approved").contains("MGR1");
        }
    }

    // ─────────────────────────────── approveAllUnderManagerForWeek ───────────────────────────────
    @Nested class ApproveAllUnderManager {

        @Test
        void bulkApproves_andReturnsCount() {
            UserIdentityDto emp = new UserIdentityDto();
            emp.setEmployeeCode("EMP1");
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.ok(List.of(emp)));
            when(summaryRepo.findByIdEmployeeCodeInAndIdWeekStartAndIdTimesheetYearAndIdTimesheetMonth(
                    any(), eq(monday), eq(2025), eq(5)))
                    .thenReturn(List.of(summaryDraft));

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setManagerCode("MGR1"); req.setWeekStart(monday);
            req.setTimesheetYear(2025); req.setTimesheetMonth(5); req.setComment("auto");

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
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.status(500).build());

            ManagerApprovalRequestDto req = new ManagerApprovalRequestDto();
            req.setManagerCode("MGR1"); req.setWeekStart(monday);
            req.setTimesheetYear(2025); req.setTimesheetMonth(5);

            assertThatThrownBy(() -> service.approveAllUnderManagerForWeek(req))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FAILED_TO_FETCH_DETAILS);
        }
    }

    // ─────────────────────────────── getDailyEntries ───────────────────────────────
    @Test
    void getDailyEntries_returnsDtosWithStatus() {
        when(summaryRepo.findByIdEmployeeCodeAndIdWeekStart("EMP1", monday))
                .thenReturn(Optional.of(summaryDraft));
        when(dailyRepo.findByEmployeeCodeAndWorkDateBetween("EMP1", monday, sunday))
                .thenReturn(List.of(dayEntity));

        var resp = service.getDailyEntries("EMP1", monday);

        assertThat(resp.getStatus()).isEqualTo(TimeSheetStatus.DRAFT);
        assertThat(resp.getDailyTimeSheetResponseDtos()).hasSize(1);
    }

    // ─────────────────────────────── getWeeklyStatus ───────────────────────────────
    @Test
    void getWeeklyStatus_returnsStatus() {
        when(summaryRepo.findByIdEmployeeCodeAndIdWeekStart("EMP1", monday))
                .thenReturn(Optional.of(summaryDraft));

        assertThat(service.getWeeklyStatus("EMP1", monday))
                .isEqualTo(TimeSheetStatus.DRAFT);
    }

    // ─────────────────────────────── getEmployeeTimesheet ───────────────────────────────
    @Test
    void getEmployeeTimesheet_buildsMatrix() {
        DailyTimeSheet tuesday = new DailyTimeSheet();
        tuesday.setEmployeeCode("EMP1"); tuesday.setTimesheetYear(2025); tuesday.setTimesheetMonth(5);
        tuesday.setWorkDate(Date.valueOf(LocalDate.of(2025,5,20)));
        tuesday.setEntryType(EntryType.PROJECT);
        tuesday.setHoursSpent(2.0);

        when(dailyRepo.findByEmployeeCodeAndTimesheetYearAndTimesheetMonth("EMP1", 2025, 5))
                .thenReturn(List.of(dayEntity, tuesday));
        when(projectRepo.findById("P01")).thenReturn(Optional.of(project));

        var matrix = service.getEmployeeTimesheet("EMP1", 2025, 5);

        assertThat(matrix).hasSize(2);            // one row for project, one for GENERAL
        assertThat(matrix.get(0).getWeeklyHours()).isNotEmpty();
    }

    // ─────────────────────────────── Bean-Validation quick checks ───────────────────────────────
    @Test
    void validation_dailyRequest_violationOnBadHours() {
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            Validator v = f.getValidator();
            DailyTimesheetRequestDto bad = new DailyTimesheetRequestDto();
            bad.setEmployeeCode("EMP1"); bad.setTimesheetYear(2025); bad.setTimesheetMonth(5);
            bad.setWorkDate(monday); bad.setEntryType(EntryType.PROJECT);
            bad.setHoursSpent(30.0);                       // > 24  => violation
            assertThat(v.validate(bad)).hasSize(1);
        }
    }



    @Nested
    class GetEmployeesTimesheetUnderManager {

        @Test
        void returnsSortedApprovalDtos() {
            // identity-service returns EMP2, EMP1 (intentionally unsorted)
            UserIdentityDto u1 = new UserIdentityDto(); u1.setEmployeeCode("EMP2");
            UserIdentityDto u2 = new UserIdentityDto(); u2.setEmployeeCode("EMP1");
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.ok(List.of(u1, u2)));

            // Mock the page response from repository
            Page<TimesheetSummary> mockPage = new PageImpl<>(
                    List.of(
                            makeSummary("EMP1", monday, 40, TimeSheetStatus.APPROVED),
                            makeSummary("EMP2", sunday, 15, TimeSheetStatus.SUBMITTED)
                    ),
                    PageRequest.of(0, 10),
                    2 // total elements
            );

            when(summaryRepo.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(mockPage);

            // Call the method with pagination parameters
            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager("MGR1", 2025, 5, 0, 10,
                            Collections.emptyList(), Collections.emptyList());

            // Verify the content is sorted and mapped correctly
            List<ManagerApprovalRequestDto> content = out.getContent();
            assertThat(content).extracting(ManagerApprovalRequestDto::getEmployeeCode)
                    .containsExactly("EMP1", "EMP2");  // sorted
            assertThat(content.get(0).isApprove()).isTrue();      // APPROVED → approve=true
            assertThat(content.get(1).isApprove()).isFalse();     // SUBMITTED → approve=false

            // Verify pagination info
            assertThat(out.getPage()).isEqualTo(0);
            assertThat(out.getSize()).isEqualTo(10);
            assertThat(out.getTotalElements()).isEqualTo(2);
            assertThat(out.getTotalPages()).isEqualTo(1);
            assertThat(out.isLast()).isTrue();
        }

        @Test
        void returnsEmptyWhenNoEmployeesUnderManager() {
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.ok(Collections.emptyList()));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager("MGR1", 2025, 5, 0, 10,
                            Collections.emptyList(), Collections.emptyList());

            assertThat(out.getContent()).isEmpty();
            assertThat(out.getTotalElements()).isEqualTo(0);
            assertThat(out.getTotalPages()).isEqualTo(0);
        }

        @Test
        void throwsWhenNoTimesheetsFound() {
            UserIdentityDto u1 = new UserIdentityDto(); u1.setEmployeeCode("EMP1");
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.ok(List.of(u1)));

            when(summaryRepo.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThatThrownBy(() ->
                    service.getEmployeesTimesheetUnderManager("MGR1", 2025, 5, 0, 10,
                            Collections.emptyList(), Collections.emptyList()))
                    .isInstanceOf(TimeSheetException.class)
                    .hasMessage(ErrorMessage.NO_TIMESHEET_SUMMARIES_FOUND);
        }

        @Test
        void appliesPaginationCorrectly() {
            // Setup 3 employees
            UserIdentityDto u1 = new UserIdentityDto(); u1.setEmployeeCode("EMP1");
            UserIdentityDto u2 = new UserIdentityDto(); u2.setEmployeeCode("EMP2");
            UserIdentityDto u3 = new UserIdentityDto(); u3.setEmployeeCode("EMP3");
            when(identityClient.getEmployeesUnderManager("MGR1"))
                    .thenReturn(ResponseEntity.ok(List.of(u1, u2, u3)));

            // Mock second page (page 1) with 1 item
            Page<TimesheetSummary> mockPage = new PageImpl<>(
                    List.of(makeSummary("EMP3", sunday, 20, TimeSheetStatus.SUBMITTED)),
                    PageRequest.of(1, 2), // page 1, size 2
                    3 // total elements
            );

            when(summaryRepo.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(mockPage);

            // Request second page (offset 2, limit 2)
            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheetUnderManager("MGR1", 2025, 5, 2, 2,
                            Collections.emptyList(), Collections.emptyList());

            assertThat(out.getContent()).hasSize(1);
            assertThat(out.getContent().get(0).getEmployeeCode()).isEqualTo("EMP3");
            assertThat(out.getPage()).isEqualTo(1);
            assertThat(out.getSize()).isEqualTo(2);
            assertThat(out.getTotalElements()).isEqualTo(3);
            assertThat(out.getTotalPages()).isEqualTo(2);
            assertThat(out.isLast()).isTrue();
        }
    }

    @Nested
    class GetEmployeesTimesheetAll {

        private final int year  = 2025;
        private final int month = 5;
        private final int offset = 0;
        private final int limit  = 10;

        private final List<FilterRequest> noFilters = List.of();

        private final List<SortRequest> employeeCodeAsc;

        public GetEmployeesTimesheetAll() {
            SortRequest sortRequest = new SortRequest();
            sortRequest.setField("employeeCode");
            sortRequest.setDirection("asc");  // use lowercase as per your class comment
            employeeCodeAsc = List.of(sortRequest);
        }

        @BeforeEach
        void mockIdentityUsers() {
            Map<String,String> m1 = Map.of("employeeCode", "EMP9");
            Map<String,String> m2 = Map.of("employeeCode", "EMP3");
            when(identityClient.getAllUsersList())
                    .thenReturn(ResponseEntity.ok(List.of(m1, m2)));
        }

        @Test
        void buildsPagedResponseAndSortsByEmployeeCode() {
            when(summaryRepo.findAll(any(Specification.class), any(Pageable.class)))
                    .thenAnswer(inv -> {
                        TimesheetSummary s1 = makeSummary("EMP3", monday, 10, TimeSheetStatus.DRAFT);
                        TimesheetSummary s2 = makeSummary("EMP9", sunday, 20, TimeSheetStatus.APPROVED);

                        Page<TimesheetSummary> page =
                                new PageImpl<>(List.of(s1, s2),
                                        PageRequest.of(offset / limit, limit, Sort.by("id.employeeCode").ascending()),
                                        2);
                        return page;
                    });

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheet(year, month, offset, limit, noFilters, employeeCodeAsc);

            assertThat(out.getTotalElements()).isEqualTo(2);
            assertThat(out.getContent())
                    .extracting(ManagerApprovalRequestDto::getEmployeeCode)
                    .containsExactly("EMP3", "EMP9");
            assertThat(out.getContent().get(0).isApprove()).isFalse();
            assertThat(out.getContent().get(1).getHours()).isEqualTo(20);
        }

        @Test
        void emptyWhenIdentityReturnsNone() {
            when(identityClient.getAllUsersList())
                    .thenReturn(ResponseEntity.ok(List.of()));

            PagedResponse<ManagerApprovalRequestDto> out =
                    service.getEmployeesTimesheet(year, month, offset, limit, noFilters, employeeCodeAsc);

            assertThat(out.getTotalElements()).isZero();
            assertThat(out.getContent()).isEmpty();
            verifyNoInteractions(summaryRepo);
        }
    }



}
