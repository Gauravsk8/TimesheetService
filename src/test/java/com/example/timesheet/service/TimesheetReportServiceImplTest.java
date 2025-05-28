package com.example.timesheet.service;

import com.example.timesheet.Repository.DailyTimeSheetRepository;
import com.example.timesheet.Repository.ProjectRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.enums.EntryType;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.models.DailyTimeSheet;
import com.example.timesheet.models.Project;
import com.example.timesheet.service.Serviceimpl.TimesheetReportServiceImpl;
import com.example.timesheet.utils.ExcelReportGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.sql.Date;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class TimesheetReportServiceImplTest {

    @Mock private DailyTimeSheetRepository dailyRepo;
    @Mock private ProjectRepository        projectRepo;
    @Mock private IdentityServiceClient    identityClient;
    @InjectMocks private TimesheetReportServiceImpl service;   // SUT

    private final LocalDate   startLd = LocalDate.of(2025,  5, 1);
    private final LocalDate   endLd   = LocalDate.of(2025,  5,31);
    private final Date        start   = Date.valueOf(startLd);
    private final Date        end     = Date.valueOf(endLd);

    private DailyTimeSheet mkEntry(String emp, String proj, LocalDate day, double h) {
        DailyTimeSheet d = new DailyTimeSheet();
        d.setEmployeeCode(emp);
        d.setProjectCode(proj);
        d.setWorkDate(Date.valueOf(day));
        d.setEntryType(EntryType.PROJECT);
        d.setHoursSpent(h);
        return d;
    }


    @Nested class MonthlyReport {

        @Test
        void errorPath_generatesExcelPerProjectEmployee() throws IOException {
            // Data
            List<DailyTimeSheet> entries = List.of(
                    mkEntry("EMP1", "PRJ1", LocalDate.of(2025,5,3), 4),
                    mkEntry("EMP2", "PRJ1", LocalDate.of(2025,5,4), 6)
            );
            when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                    .thenReturn(entries);
            when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                    anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(List.of());                       // no leave/holiday rows

            Project prj = new Project(); prj.setProjectCode("PRJ1");
            prj.setTitle("Apollo"); prj.setProjectManagerCode("MGR1");
            when(projectRepo.findById("PRJ1")).thenReturn(Optional.of(prj));

            UserIdentityDto mgr = new UserIdentityDto();
            mgr.setFirstName("Mary"); mgr.setLastName("Smith");
            when(identityClient.getUserByemployeeCode("MGR1"))
                    .thenReturn(ResponseEntity.ok(mgr));
            UserIdentityDto emp1 = new UserIdentityDto();
            emp1.setFirstName("John"); emp1.setLastName("Jones");
            when(identityClient.getUserByemployeeCode("EMP1"))
                    .thenReturn(ResponseEntity.ok(emp1));
            when(identityClient.getUserByemployeeCode("EMP2"))
                    .thenReturn(ResponseEntity.ok(emp1)); // reuse same object

            try (MockedStatic<ExcelReportGenerator> excelMock = mockStatic(ExcelReportGenerator.class)) {
                excelMock.when(() -> ExcelReportGenerator.generateExcel(
                                anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyList()))
                        .thenThrow(new IOException("disk full"));   // ← fail

                ResponseEntity<String> resp = service.generateReport(2025, 5, null);

                assertThat(resp.getStatusCode().is5xxServerError()).isTrue();
                assertThat(resp.getBody()).contains("Failed to generate report")
                        .contains("disk full");
            }
        }

        @Test
        void handlesIOException_andReturns500() throws IOException {
            when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                    .thenReturn(List.of(mkEntry("EMP1", "PRJ1", startLd, 4)));
            when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                    anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(List.of());

            Project project = new Project();
            project.setProjectCode("PRJ1");
            project.setTitle("Test Project");
            project.setProjectManagerCode("MGR1");
            when(projectRepo.findById("PRJ1")).thenReturn(Optional.of(project));

            UserIdentityDto mgr = new UserIdentityDto(); mgr.setFirstName("Mary"); mgr.setLastName("Smith");
            UserIdentityDto emp = new UserIdentityDto(); emp.setFirstName("John"); emp.setLastName("Jones");

            when(identityClient.getUserByemployeeCode("MGR1")).thenReturn(ResponseEntity.ok(mgr));
            when(identityClient.getUserByemployeeCode("EMP1")).thenReturn(ResponseEntity.ok(emp));

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


    @Nested class RangeReport {

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
        DailyTimeSheet work = mkEntry("EMP1", "PRJ1", LocalDate.of(2025,5,5), 4);
        DailyTimeSheet leave = mkEntry("EMP1", "PRJ1", LocalDate.of(2025,5,6), 0);
        leave.setEntryType(EntryType.LEAVE);

        when(dailyRepo.findByTimesheetYearAndTimesheetMonth(2025, 5))
                .thenReturn(List.of(work));
        when(dailyRepo.findByTimesheetYearAndTimesheetMonthAndEmployeeCodeInAndEntryTypeIn(
                eq(2025), eq(5), anyList(), anyList()))
                .thenReturn(List.of(leave));

        var map = service.getMonthlyTimesheetData(2025, 5, null);

        List<DailyTimeSheet> merged = map.get("PRJ1").get("EMP1");
        assertThat(merged).containsExactlyInAnyOrder(work, leave);
    }

    @Test
    void shouldReturnSuccessEvenWhenIdentityServiceFails() {
        // Only setup what's absolutely needed
        Project project = new Project();
        project.setProjectCode("PRJ1");
        project.setProjectManagerCode("BAD");

        when(projectRepo.findById("PRJ1"))
                .thenReturn(Optional.of(project));

        when(identityClient.getUserByemployeeCode("BAD"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found"));

        ResponseEntity<String> response = service.generateReport(2025, 5, "PRJ1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

}