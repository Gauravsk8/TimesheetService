package com.example.timesheet.service;

import com.example.timesheet.repository.DailyTimeSheetRepository;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.EntryType;
import com.example.timesheet.models.DailyTimeSheet;
import com.example.timesheet.models.Project;
import com.example.timesheet.service.serviceimpl.TimesheetReportServiceImpl;
import com.example.timesheet.utils.ExcelReportGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class TimesheetReportServiceImplTest {

    private static final String EMPLOYEE_1 = "EMP1";
    private static final String EMPLOYEE_2 = "EMP2";
    private static final String PROJECT_1 = "PRJ1";
    private static final String MANAGER_1 = "MGR1";

    @Mock
    private DailyTimeSheetRepository dailyRepo;

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private IdentityServiceClient identityClient;

    @InjectMocks
    private TimesheetReportServiceImpl service;

    private final LocalDate startLd = LocalDate.of(2025, 5, 1);
    private final LocalDate endLd = LocalDate.of(2025, 5, 31);
    private final Date start = Date.valueOf(startLd);
    private final Date end = Date.valueOf(endLd);

    private DailyTimeSheet mkEntry(String emp, String proj, LocalDate day, double h) {
        DailyTimeSheet d = new DailyTimeSheet();
        d.setEmployeeCode(emp);
        d.setProjectCode(proj);
        d.setWorkDate(Date.valueOf(day));
        d.setEntryType(EntryType.PROJECT);
        d.setHoursSpent(h);
        return d;
    }

    @Nested
    class MonthlyReport {
        @Test
        void errorPath_generatesExcelPerProjectEmployee() throws IOException {
            // Data
            List<DailyTimeSheet> entries = List.of(
                    mkEntry(EMPLOYEE_1, PROJECT_1, LocalDate.of(2025, 5, 3), 4),
                    mkEntry(EMPLOYEE_2, PROJECT_1, LocalDate.of(2025, 5, 4), 6)
            );

            when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                    .thenReturn(entries);

            when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                    anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(List.of());

            Project prj = new Project();
            prj.setProjectCode(PROJECT_1);
            prj.setTitle("Apollo");
            prj.setProjectManagerCode(MANAGER_1);

            when(projectRepo.findById(PROJECT_1)).thenReturn(Optional.of(prj));

            UserIdentityDto mgr = new UserIdentityDto();
            mgr.setFirstName("Mary");
            mgr.setLastName("Smith");

            when(identityClient.getUserByemployeeCode(MANAGER_1))
                    .thenReturn(ResponseEntity.ok(mgr));

            UserIdentityDto emp1 = new UserIdentityDto();
            emp1.setFirstName("John");
            emp1.setLastName("Jones");

            when(identityClient.getUserByemployeeCode(EMPLOYEE_1))
                    .thenReturn(ResponseEntity.ok(emp1));

            when(identityClient.getUserByemployeeCode(EMPLOYEE_2))
                    .thenReturn(ResponseEntity.ok(emp1));

            try (MockedStatic<ExcelReportGenerator> excelMock = mockStatic(ExcelReportGenerator.class)) {
                excelMock.when(() -> ExcelReportGenerator.generateExcel(
                                anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyList()))
                        .thenThrow(new IOException("disk full"));

                ResponseEntity<String> resp = service.generateReport(2025, 5, null);

                assertThat(resp.getStatusCode().is5xxServerError()).isTrue();
                assertThat(resp.getBody()).contains("Failed to generate report")
                        .contains("disk full");
            }
        }

        @Test
        void handlesIOException_andReturns500() throws IOException {
            when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                    .thenReturn(List.of(mkEntry(EMPLOYEE_1, PROJECT_1, startLd, 4)));

            when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                    anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(List.of());

            Project project = new Project();
            project.setProjectCode(PROJECT_1);
            project.setTitle("Test Project");
            project.setProjectManagerCode(MANAGER_1);

            when(projectRepo.findById(PROJECT_1)).thenReturn(Optional.of(project));

            UserIdentityDto mgr = new UserIdentityDto();
            mgr.setFirstName("Mary");
            mgr.setLastName("Smith");

            UserIdentityDto emp = new UserIdentityDto();
            emp.setFirstName("John");
            emp.setLastName("Jones");

            when(identityClient.getUserByemployeeCode(MANAGER_1)).thenReturn(ResponseEntity.ok(mgr));
            when(identityClient.getUserByemployeeCode(EMPLOYEE_1)).thenReturn(ResponseEntity.ok(emp));

            try (MockedStatic<ExcelReportGenerator> excelMock = mockStatic(ExcelReportGenerator.class)) {
                excelMock.when(() -> ExcelReportGenerator.generateExcel(
                                anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyList()))
                        .thenThrow(new IOException("disk full"));

                var resp = service.generateReport(2025, 5, null);

                assertThat(resp.getStatusCode().is5xxServerError()).isTrue();
                assertThat(resp.getBody()).contains("Failed to generate report");
            }
        }
    }

    @Nested
    class RangeReport {
        @Test
        void returnsNoDataMessage_whenRepoEmpty() {
            when(dailyRepo.findByWorkDateBetween(start, end))
                    .thenReturn(List.of());

            var resp = service.generateReport(null, null, null, startLd, endLd);

            assertThat(resp.getBody()).contains("No data");
            verifyNoInteractions(projectRepo);
        }
    }

    @Test
    void getMonthlyTimesheetData_mergesLeaveEntries() {
        DailyTimeSheet work = mkEntry(EMPLOYEE_1, PROJECT_1, LocalDate.of(2025, 5, 5), 4);
        DailyTimeSheet leave = mkEntry(EMPLOYEE_1, PROJECT_1, LocalDate.of(2025, 5, 6), 0);
        leave.setEntryType(EntryType.LEAVE);

        when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                .thenReturn(List.of(work));

        when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                eq(2025), eq(5), anyList(), anyList()))
                .thenReturn(List.of(leave));

        var map = service.getMonthlyTimesheetData(2025, 5, null);

        List<DailyTimeSheet> merged = map.get(PROJECT_1).get(EMPLOYEE_1);
        assertThat(merged).containsExactlyInAnyOrder(work, leave);
    }

    @Test
    void shouldReturnSuccessEvenWhenIdentityServiceFails() {
        Project project = new Project();
        project.setProjectCode(PROJECT_1);
        project.setProjectManagerCode("BAD");

        when(projectRepo.findById(PROJECT_1))
                .thenReturn(Optional.of(project));

        when(identityClient.getUserByemployeeCode("BAD"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found"));

        ResponseEntity<String> response = service.generateReport(2025, 5, PROJECT_1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}