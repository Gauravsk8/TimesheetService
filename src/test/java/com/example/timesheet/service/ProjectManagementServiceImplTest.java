package com.example.timesheet.service;

import com.example.timesheet.Repository.*;
import com.example.timesheet.client.IdentityServiceClient;
import org.springframework.data.jpa.domain.Specification;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.request.*;
import com.example.timesheet.dto.response.ProjectEmployeeDto;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.models.*;
import com.example.timesheet.service.Serviceimpl.ProjectManagementServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ProjectManagementServiceImplTest {

    // ────────── mocks & SUT ──────────
    @Mock private ClientsRepository        clientsRepo;
    @Mock private CostCenterRepository     ccRepo;
    @Mock private ProjectRepository        projRepo;
    @Mock private IdentityServiceClient    idClient;
    @Mock private ProjectEmployeeRepository peRepo;
    @Mock private ProjectRolesRepository   roleRepo;
    @InjectMocks private ProjectManagementServiceImpl service;

    // ────────── reusable domain objects ──────────
    private Clients     client;
    private CostCenter  costCenter;
    private Project     project;

    private final Timestamp start = Timestamp.valueOf(LocalDateTime.of(2025,  6, 1, 9, 0));
    private final Timestamp end   = Timestamp.valueOf(LocalDateTime.of(2025, 12,31,18,0));

    @BeforeEach
    void setUp() {
        client = new Clients();          client.setId(1L);          client.setName("Acme");
        costCenter = new CostCenter();   costCenter.setCostCenterCode("CC01"); costCenter.setActive(true);

        project = new Project();
        project.setProjectCode("PRJ01");
        project.setTitle("Apollo");
        project.setDescription("Platform");
        project.setStartDate(start);
        project.setEndDate(end);
        project.setClients(client);
        project.setCostCenter(costCenter);
        project.setProjectManagerCode("MGR1");
        project.setAllocated_hours("1000");
        project.setActive(true);
    }

    // ───────────────────────── createProject ─────────────────────────
    @Nested class CreateProject {

        @Test
        void happyPath_persistsProject() {
            when(clientsRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(client));
            when(ccRepo.findByCostCenterCodeAndIsActiveTrue("CC01")).thenReturn(Optional.of(costCenter));

            ProjectDto dto = new ProjectDto();
            dto.setTitle("Apollo"); dto.setDescription("Platform");
            dto.setStartDate(start); dto.setEndDate(end);
            dto.setClientId(1L); dto.setCostCenterCode("CC01");
            dto.setProjectManagerCode("MGR1"); dto.setAllocatedHours("1000");

            String msg = service.createProject(dto);

            verify(projRepo).save(any(Project.class));
            assertThat(msg).contains(MessageConstants.PROJECT_CREATED);
        }

        @Test
        void throws_whenClientMissing() {
            when(clientsRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

            ProjectDto dto = new ProjectDto();
            dto.setTitle("Bad"); dto.setStartDate(start); dto.setEndDate(end);
            dto.setClientId(1L); dto.setCostCenterCode("CC01"); dto.setProjectManagerCode("MGR1");

            assertThatThrownBy(() -> service.createProject(dto))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    // ───────────────────────── createRolesInProject ─────────────────────────
    @Nested class CreateRoles {

        @Test
        void createsRole_whenNotExisting() {
            when(roleRepo.existsByRoleName("Dev")).thenReturn(false);

            ProjectRolesRequestDto dto = new ProjectRolesRequestDto();
            dto.setRoleName("Dev");

            assertThat(service.createRolesInProject(dto))
                    .contains("success");
            verify(roleRepo).save(any(ProjectRoles.class));
        }

        @Test
        void throws_whenDuplicateRole() {
            when(roleRepo.existsByRoleName("Dev")).thenReturn(true);

            ProjectRolesRequestDto dto = new ProjectRolesRequestDto();
            dto.setRoleName("Dev");

            assertThatThrownBy(() -> service.createRolesInProject(dto))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT_ERROR);
        }
    }

    // ───────────────────────── getAllProjects ─────────────────────────
    @Nested class GetAllProjects {

        @Test
        void returnsPagedResponse() {
            Page<Project> page = new PageImpl<>(List.of(project), PageRequest.of(0,10), 1);
            when(projRepo.findAll(Mockito.<Specification<Project>>any(),   // disambiguates the overload
                    Mockito.any(Pageable.class))).thenReturn(page);

            var resp = service.getAllProjects(0,10, emptyList(), emptyList());

            assertThat(resp.getContent()).singleElement()
                    .extracting("projectCode").isEqualTo("PRJ01");
        }

        @Test
        void throws_whenNoActiveProjects() {
            when(projRepo.findAll(
                    Mockito.<Specification<Project>>any(),
                    Mockito.any(Pageable.class)))
                    .thenReturn(Page.<Project>empty());

            assertThatThrownBy(() ->
                    service.getAllProjects(0, 10, emptyList(), emptyList()))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);   // optional: check the code you expect
        }

    }

    // ───────────────────────── updateProjectStatus ─────────────────────────
    @Nested class UpdateProjectStatus {

        @Test
        void togglesActiveFlag() {
            when(projRepo.findById("PRJ01")).thenReturn(Optional.of(project));
            when(projRepo.save(any())).thenReturn(project);

            String msg = service.updateProjectStatus("PRJ01", false);

            verify(projRepo).save(argThat(p -> !p.isActive()));
            assertThat(msg).isEqualTo(String.format(MessageConstants.PROJECT_STATUS_UPDATED, project.getTitle()));
        }

    }

    // ───────────────────────── assignEmployeesToProject ─────────────────────────
    @Nested class AssignEmployees {

        @Test
        void assignsDistinctEmployees_andCallsIdentityService() {
            when(projRepo.findByProjectCodeAndIsActiveTrue("PRJ01")).thenReturn(Optional.of(project));
            when(peRepo.existsByIdAndIsActiveTrue(any())).thenReturn(false);

            UserIdentityDto id = new UserIdentityDto(); id.setEmployeeCode("EMP1"); id.setKeycloakUserId("kc-1");
            when(idClient.getUserByemployeeCode("EMP1")).thenReturn(ResponseEntity.ok(id));

            AssignEmployeesDto.EmployeeAssignment ea = new AssignEmployeesDto.EmployeeAssignment();
            ea.setEmployeeCode("EMP1"); ea.setStartDate(start); ea.setEndDate(end); ea.setRole_in_project("Dev");

            AssignEmployeesDto dto = new AssignEmployeesDto();
            dto.setEmployees(List.of(ea));

            String msg = service.assignEmployeesToProject(dto, "PRJ01");

            verify(peRepo).saveAll(anyList());
            assertThat(msg).contains("1").contains(MessageConstants.EMPLOYEE_ASSIGNED.trim());
        }

        @Test
        void skipsDuplicates_andReturnsAlreadyAssignedMsg() {
            when(projRepo.findByProjectCodeAndIsActiveTrue("PRJ01")).thenReturn(Optional.of(project));
            when(peRepo.existsByIdAndIsActiveTrue(any())).thenReturn(true);

            AssignEmployeesDto.EmployeeAssignment ea = new AssignEmployeesDto.EmployeeAssignment();
            ea.setEmployeeCode("EMP1");
            ea.setStartDate(start);
            ea.setEndDate(end);
            ea.setRole_in_project("Dev");

            AssignEmployeesDto dto = new AssignEmployeesDto();
            dto.setEmployees(List.of(ea));

            String msg = service.assignEmployeesToProject(dto, "PRJ01");

            verify(peRepo).saveAll(argThat(iterable ->
                    iterable instanceof Collection && ((Collection<?>) iterable).isEmpty()
            ));
            assertThat(msg).isEqualTo(MessageConstants.EMPLOYEE_ALREADY_ASSIGNED);
        }
    }

    // ───────────────────────── getEmployeesByProject ─────────────────────────
    @Test
    void getEmployeesByProject_mapsEntitiesToDtos() {
        ProjectEmployee pe = new ProjectEmployee();
        pe.setId(new ProjectEmployeeId("PRJ01", "EMP1"));
        pe.setStartDate(start); pe.setEndDate(end); pe.setActive(true);
        pe.setProject(project);

        when(peRepo.findByProject_ProjectCodeIgnoreCaseAndIsActiveTrue("PRJ01"))
                .thenReturn(List.of(pe));

        UserIdentityDto id = new UserIdentityDto();
        id.setEmployeeCode("EMP1"); id.setFirstName("John"); id.setLastName("Doe");
        when(idClient.getUserByemployeeCode("EMP1")).thenReturn(ResponseEntity.ok(id));

        List<ProjectEmployeeDto> list = service.getEmployeesByProject("PRJ01");

        assertThat(list).singleElement().extracting(ProjectEmployeeDto::getFirstName).isEqualTo("John");
    }

    // ───────────────────────── updateEmployee_dateValidation ─────────────────────────
    @Nested class UpdateEmployeeDates {

        @Test
        void throws_whenStartBeforeProject() {
            when(projRepo.findByProjectCodeAndIsActiveTrue("PRJ01")).thenReturn(Optional.of(project));

            ProjectEmployee pe = new ProjectEmployee();
            pe.setId(new ProjectEmployeeId("PRJ01", "EMP1")); pe.setActive(true);
            when(peRepo.findByIdAndIsActiveTrue(any())).thenReturn(Optional.of(pe));

            AssignEmployeesDto.EmployeeAssignment ea = new AssignEmployeesDto.EmployeeAssignment();
            ea.setStartDate(Timestamp.valueOf(start.toLocalDateTime().minusDays(10)));
            ea.setEndDate(end); ea.setRole_in_project("Dev");

            assertThatThrownBy(() ->
                    service.updateEmployee("PRJ01", "EMP1", ea))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    // ───────────────────────── getUnassignedUsersForProject ─────────────────────────
    @Test
    void unassignedUsers_filtersAssigned() {
        when(projRepo.findByProjectCodeAndIsActiveTrue("PRJ01")).thenReturn(Optional.of(project));
        when(peRepo.findByProject_ProjectCodeAndIsActiveTrue("PRJ01"))
                .thenReturn(List.of());

        Map<String,String> u1 = Map.of("employeeCode","EMP1");
        Map<String,String> u2 = Map.of("employeeCode","EMP2");
        when(idClient.getAllUsersList()).thenReturn(ResponseEntity.ok(List.of(u1,u2)));

        List<Map<String,String>> list = service.getUnassignedUsersForProject("PRJ01");

        assertThat(list).hasSize(2);
    }

    // ───────────────────────── Bean-Validation smoke tests ─────────────────────────
    @Test
    void validation_projectDto_hoursPatternViolation() {
        try (ValidatorFactory vf = Validation.buildDefaultValidatorFactory()) {
            Validator v = vf.getValidator();
            ProjectDto dto = new ProjectDto();
            dto.setTitle("Ok"); dto.setStartDate(start); dto.setEndDate(end);
            dto.setClientId(1L); dto.setCostCenterCode("CC01"); dto.setProjectManagerCode("MGR1");
            dto.setAllocatedHours("not-a-number");
            assertThat(v.validate(dto)).hasSize(1);
        }
    }
}
