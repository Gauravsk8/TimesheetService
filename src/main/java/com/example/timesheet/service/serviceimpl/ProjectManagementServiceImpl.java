package com.example.timesheet.service.serviceimpl;

import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterSpecificationBuilder;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.repository.ProjectEmployeeRepository;
import com.example.timesheet.repository.ClientsRepository;
import com.example.timesheet.repository.CostCenterRepository;
import com.example.timesheet.repository.ProjectRolesRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.dto.request.AssignEmployeesDto;
import com.example.timesheet.dto.request.ProjectRolesRequestDto;
import com.example.timesheet.dto.request.ProjectDto;
import com.example.timesheet.dto.response.UserIdentityDto;
import com.example.timesheet.dto.response.ProjectEmployeeDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.dto.response.ProjectWithEmployeesDto;
import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.models.Project;
import com.example.timesheet.models.Clients;
import com.example.timesheet.models.CostCenter;
import com.example.timesheet.models.ProjectEmployee;
import com.example.timesheet.models.ProjectRoles;
import com.example.timesheet.service.ProjectManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectManagementServiceImpl implements ProjectManagementService {
    private final ClientsRepository clientsRepository;
    private final CostCenterRepository costCenterRepository;
    private final ProjectRepository projectRepository;
    private final IdentityServiceClient identityServiceClient;
    private final ProjectEmployeeRepository projectEmployeeRepository;
    private final ProjectRolesRepository rolesInProjectRepository;

    @Override
    public String createProject(ProjectDto dto) {

        Clients client = clientsRepository.findByIdAndIsActiveTrue(dto.getClientId())
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.CLIENT_NOT_FOUND, dto.getClientId())
                ));

        CostCenter costCenter = costCenterRepository.findByCostCenterCodeAndIsActiveTrue(dto.getCostCenterCode())
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.COST_CENTER_NOT_FOUND, dto.getCostCenterCode())
                ));


        Project project = new Project();
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setClients(client);
        project.setCostCenter(costCenter);
        project.setProjectManagerCode(dto.getProjectManagerCode());
        project.setAllocated_hours(dto.getAllocatedHours());

        projectRepository.save(project);

        return MessageConstants.PROJECT_CREATED + dto.getTitle();
    }

    @Override
    public String createRolesInProject(ProjectRolesRequestDto dto) {
        boolean exists = rolesInProjectRepository.existsByRoleName(dto.getRoleName());
        if (exists){
            throw new TimeSheetException(ErrorCode.CONFLICT_ERROR, ErrorMessage.PROJECT_ROLE_ALREADY_CREATED);
        }
        ProjectRoles newRole = new ProjectRoles();
        newRole.setRoleName(dto.getRoleName());
        rolesInProjectRepository.save(newRole);
        return MessageConstants.PROJECT_ROLE_CREATED;
    }
    @Override
    public String updateRolesInProject(ProjectRolesRequestDto dto, Long roleId) {
        ProjectRoles roles = rolesInProjectRepository.findById(roleId).orElseThrow(() -> new TimeSheetException(
                ErrorCode.NOT_FOUND_ERROR,
                ErrorMessage.PROJECT_ROLE_NOT_FOUND));

        roles.setRoleName(dto.getRoleName());
        rolesInProjectRepository.save(roles);
        return MessageConstants.PROJECT_ROLE_UPDATED;
    }
    @Override
    public String getRolesInProject(Long roleId) {
        ProjectRoles roles = rolesInProjectRepository.findById(roleId).orElseThrow(() -> new TimeSheetException(
                ErrorCode.NOT_FOUND_ERROR,
                ErrorMessage.PROJECT_ROLE_NOT_FOUND));
        return roles.toString();
    }
    @Override
    public String deleteRolesInProject(Long roleId) {
        rolesInProjectRepository.deleteById(roleId);
        return MessageConstants.DELETED_PROJECT_ROLE;
    }
    @Override
    public List<String> getAllRoleNames() {
        return rolesInProjectRepository.findAll()
                .stream()
                .map(ProjectRoles::getRoleName)
                .collect(Collectors.toList());
    }
    @Override
    public PagedResponse<ProjectResponseDto> getAllProjects(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        int safeOffset = offset == null || offset < 0 ? 0 : offset;
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;
        int page = safeOffset / safeLimit;

        Pageable pageable = PageRequest.of(page, safeLimit, SortUtil.getSort(sorts));

        Specification<Project> filterSpec = new FilterSpecificationBuilder<Project>().build(filters);
        Specification<Project> isActiveSpec = (root, query, cb) -> cb.isTrue(root.get("isActive"));
        Specification<Project> finalSpec = Specification.where(isActiveSpec).and(filterSpec);

        Page<Project> projectPage = projectRepository.findAll(finalSpec, pageable);

        if (projectPage.isEmpty()) {
            throw new TimeSheetException(
                    ErrorCode.NOT_FOUND_ERROR,
                    ErrorMessage.NO_ACTIVE_PROJECTS_FOUND
            );
        }

        List<ProjectResponseDto> content = projectPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements()
        );
    }




    @Override
    public ProjectResponseDto getProjectByCode(String code) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, code)
                ));
        return mapToDto(project);
    }

    @Override
    public String updateProject(String code, ProjectDto dto) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, code)
                ));

        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setProjectManagerCode(dto.getProjectManagerCode());
        project.setAllocated_hours(dto.getAllocatedHours());

        if (!project.getClients().getId().equals(dto.getClientId())) {
            Clients client = clientsRepository.findByIdAndIsActiveTrue(dto.getClientId())
                    .orElseThrow(() -> new TimeSheetException(
                            ErrorCode.NOT_FOUND_ERROR,
                            String.format(ErrorMessage.CLIENT_NOT_FOUND, dto.getClientId())
                    ));
            project.setClients(client);
        }

        if (!project.getCostCenter().getCostCenterCode().equalsIgnoreCase(dto.getCostCenterCode())) {
            CostCenter costCenter = costCenterRepository.findByCostCenterCodeAndIsActiveTrue(dto.getCostCenterCode())
                    .orElseThrow(() -> new TimeSheetException(
                            ErrorCode.NOT_FOUND_ERROR,
                            String.format(ErrorMessage.COST_CENTER_NOT_FOUND, dto.getCostCenterCode())
                    ));
            project.setCostCenter(costCenter);
        }

        projectRepository.save(project);
        return MessageConstants.PROJECT_UPDATE + project.getTitle();
    }

    @Override
    public String updateProjectStatus(String projectCode, boolean active) throws TimeSheetException {
        Project project = projectRepository.findById(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode)));


        project.setActive(active);
        Project savedProject = projectRepository.save(project);
        return String.format(MessageConstants.PROJECT_STATUS_UPDATED, savedProject.getTitle());
    }

    @Override
    public String assignEmployeesToProject(AssignEmployeesDto dto, String projectCode) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR, // Assuming this is the error code
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode) // Assuming you have this error message in your errorMessage class
                ));
        List<ProjectEmployee> assignments = dto.getEmployees().stream()
                .filter(emp -> {
                    ProjectEmployeeId id = new ProjectEmployeeId(projectCode, emp.getEmployeeCode());
                    return !projectEmployeeRepository.existsByIdAndIsActiveTrue(id); // avoid duplicates
                })
                .map(emp -> {
                    ProjectEmployee pe = new ProjectEmployee();
                    ResponseEntity<UserIdentityDto> user;
                    try {
                        user = identityServiceClient.getUserByemployeeCode(emp.getEmployeeCode());
                    } catch (Exception e) {
                        throw new TimeSheetException(ErrorCode.NOT_FOUND_ERROR, ErrorMessage.USER_NOT_FOUND + e.getMessage());
                    }
                    String employeeKeycloakId = user.getBody().getKeycloakUserId();
                    pe.setId(new ProjectEmployeeId(projectCode, emp.getEmployeeCode()));
                    pe.setProject(project);
                    pe.setStartDate(project.getStartDate());
                    pe.setEndDate(project.getEndDate());
                    pe.setRole_in_project(emp.getRole_in_project());
                    return pe;
                }).toList();

        projectEmployeeRepository.saveAll(assignments);
        return assignments.isEmpty()
                ? MessageConstants.EMPLOYEE_ALREADY_ASSIGNED
                : assignments.size() + MessageConstants.EMPLOYEE_ASSIGNED;
    }

    @Override
    public List<ProjectEmployeeDto> getEmployeesByProject(String projectCode) {
        List<ProjectEmployee> entities = projectEmployeeRepository.findByProject_ProjectCodeIgnoreCaseAndIsActiveTrue(projectCode);

        return entities.stream().map(pe -> {
            ResponseEntity<UserIdentityDto> user = identityServiceClient.getUserByemployeeCode(pe.getId().getEmployeeCode());

            return ProjectEmployeeDto.builder()
                    .employeeCode(pe.getId().getEmployeeCode())
                    .firstName(user.getBody().getFirstName())
                    .lastName(user.getBody().getLastName())
                    .startDate(pe.getStartDate())
                    .endDate(pe.getEndDate())
                    .isActive(pe.isActive())
                    .build();
        }).toList();
    }

    public void removeEmployeeFromProject(String projectCode, String employeeCode) {
        ProjectEmployeeId id = new ProjectEmployeeId(projectCode, employeeCode);

        if (!projectEmployeeRepository.existsByIdAndIsActiveTrue(id)) {
            throw new TimeSheetException(
                    ErrorCode.NOT_FOUND_ERROR,
                    String.format(ErrorMessage.ASSIGNMENT_NOT_FOUND, projectCode, employeeCode)
            );
        }

        projectEmployeeRepository.deleteById(id);
    }

    @Override
    public ProjectWithEmployeesDto getProjectWithEmployees(String projectCode) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR, // Assuming this is the error code
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode) // Assuming you have this error message in your errorMessage class
                ));

        List<ProjectEmployeeDto> employees = getEmployeesByProject(projectCode);

        return ProjectWithEmployeesDto.builder()
                .projectCode(project.getProjectCode())
                .title(project.getTitle())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .costCenterCode(project.getCostCenter().getCostCenterCode())
                .clientName(project.getClients().getName())
                .projectManagerCode(project.getProjectManagerCode())
                .employees(employees)
                .build();

    }
    public void deleteProject(String projectCode) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR, // Assuming this is the error code
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode) // Assuming you have this error message in your errorMessage class
                ));

        projectRepository.delete(project);
    }

    @Override
    public String updateEmployeeStatus(String projectCode, String employeeCode, boolean newStatus) {
        ProjectEmployeeId id = new ProjectEmployeeId(projectCode, employeeCode);

        ProjectEmployee projectEmployee = projectEmployeeRepository.findById(id)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.ASSIGNMENT_NOT_FOUND, projectCode, employeeCode)
                ));

        projectEmployee.setActive(newStatus);
        projectEmployeeRepository.save(projectEmployee);

        return String.format(MessageConstants.PROJECT_EMPLOYEE_STATUS_UPDATED, employeeCode, projectCode);
    }

    @Override
    public String updateEmployee(String projectCode, String employeeCode, AssignEmployeesDto.EmployeeAssignment dto) {
        ProjectEmployeeId id = new ProjectEmployeeId(projectCode, employeeCode);

        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR, // Assuming this is the error code
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode) // Assuming you have this error message in your errorMessage class
                ));

        ProjectEmployee projectEmployee = projectEmployeeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.ASSIGNMENT_NOT_FOUND, projectCode, employeeCode)
                ));
        validateEmployeeDates(dto, project.getStartDate(), project.getEndDate(), projectCode);
        projectEmployee.setStartDate(dto.getStartDate());
        projectEmployee.setEndDate(dto.getEndDate());
        projectEmployee.setRole_in_project(dto.getRole_in_project());
        projectEmployeeRepository.save(projectEmployee);
        return String.format(MessageConstants.PROJECT_EMPLOYEE_STATUS_UPDATED, employeeCode, projectCode);
    }

    @Override
    public List<ProjectDto> getProjectsByEmployeeCode(String employeeCode) {
        List<ProjectEmployee> assignments = projectEmployeeRepository.findByIdEmployeeCodeIgnoreCaseAndIsActiveTrue(employeeCode);

        return assignments.stream()
                .map(pe -> {
                    Project project = pe.getProject();

                    ProjectDto dto = new ProjectDto();
                    dto.setProjectCode(project.getProjectCode());
                    dto.setTitle(project.getTitle());
                    dto.setDescription(project.getDescription());
                    dto.setStartDate(project.getStartDate());
                    dto.setEndDate(project.getEndDate());
                    dto.setProjectManagerCode(project.getProjectManagerCode());
                    dto.setAllocatedHours(project.getAllocated_hours());
                    dto.setCostCenterCode(project.getCostCenter().getCostCenterCode());
                    return dto;
                })
                .toList();
    }


    private ProjectResponseDto mapToResponse(Project project) {
        return ProjectResponseDto.builder()
                .projectCode(project.getProjectCode())
                .title(project.getTitle())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .clientName(project.getClients().getName())
                .costCenterCode(project.getCostCenter().getCostCenterCode())
                .projectManagerCode(project.getProjectManagerCode())
                .allocatedHours(project.getAllocated_hours())
                .build();
    }
    @Override
    public List<Map<String, String>> getUnassignedUsersForProject(String projectCode) {
        Project project = projectRepository.findByProjectCodeAndIsActiveTrue(projectCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.PROJECT_NOT_FOUND, projectCode)
                ));

        Set<String> assignedEmployeeCodes = projectEmployeeRepository
                .findByProject_ProjectCodeAndIsActiveTrue(projectCode).stream()
                .map(pe -> pe.getId().getEmployeeCode())
                .collect(Collectors.toSet());

        List<Map<String, String>> allUsers = identityServiceClient.getAllUsersList().getBody();

        return allUsers.stream()
                .filter(user -> !assignedEmployeeCodes.contains(user.get("employeeCode")))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectWithEmployeesDto> getProjectsWithEmployeesUnderManager(String projectManagerCode) {
        List<Project> projects = projectRepository.findByProjectManagerCodeIgnoreCaseAndIsActiveTrue(projectManagerCode);

        return projects.stream().map(project -> {

            List<ProjectEmployeeDto> employeeDtos = project.getProjectEmployees().stream().map(pe -> {
                String empCode = pe.getId().getEmployeeCode();

                // Call identity service for user details
                ResponseEntity<UserIdentityDto> userResponse = identityServiceClient.getUserByemployeeCode(empCode);
                UserIdentityDto user = userResponse.getBody();

                return ProjectEmployeeDto.builder()
                        .employeeCode(empCode)
                        .firstName(user != null ? user.getFirstName() : null)
                        .lastName(user != null ? user.getLastName() : null)
                        .startDate(pe.getStartDate())
                        .endDate(pe.getEndDate())
                        .isActive(pe.isActive())
                        .build();
            }).collect(Collectors.toList());

            return ProjectWithEmployeesDto.builder()
                    .projectCode(project.getProjectCode())
                    .title(project.getTitle())
                    .description(project.getDescription())
                    .startDate(project.getStartDate())
                    .endDate(project.getEndDate())
                    .projectManagerCode(project.getProjectManagerCode())
                    .employees(employeeDtos)
                    .build();

        }).collect(Collectors.toList());
    }


    // Mapping function from Project to ProjectResponseDto
    private ProjectResponseDto mapToDto(Project project) {
        return ProjectResponseDto.builder()
                .projectCode(project.getProjectCode())
                .title(project.getTitle())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .clientName(project.getClients().getName())
                .costCenterCode(project.getCostCenter().getCostCenterCode())
                .projectManagerCode(project.getProjectManagerCode())
                .allocatedHours(project.getAllocated_hours())
                .isActive(project.isActive())
                .build();
    }


    private void validateEmployeeDates(AssignEmployeesDto.EmployeeAssignment emp,
                                       Timestamp projectStart,
                                       Timestamp projectEnd,
                                       String projectCode) {

        Timestamp empStart = emp.getStartDate();
        Timestamp empEnd = emp.getEndDate(); // may be null

        if (empStart == null) {
            throw new TimeSheetException(ErrorCode.VALIDATION_ERROR,
                    String.format(ErrorMessage.START_DATE_REQUIRED, emp.getEmployeeCode()));
        }

        if (empStart.before(projectStart)) {
            throw new TimeSheetException(ErrorCode.VALIDATION_ERROR,
                    String.format(ErrorMessage.EMP_START_BEFORE_PROJECT,
                            emp.getEmployeeCode(), projectCode));
        }

        if (projectEnd != null && empEnd != null && empEnd.after(projectEnd)) {
            throw new TimeSheetException(ErrorCode.VALIDATION_ERROR,
                    String.format(ErrorMessage.EMP_END_AFTER_PROJECT,
                            emp.getEmployeeCode(), projectCode));
        }

        if (empEnd != null && empEnd.before(empStart)) {
            throw new TimeSheetException(ErrorCode.VALIDATION_ERROR,
                    String.format(ErrorMessage.END_BEFORE_START, emp.getEmployeeCode()));
        }
    }

}