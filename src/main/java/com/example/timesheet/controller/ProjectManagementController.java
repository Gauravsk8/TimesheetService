package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.common.constants.AuthorizationConstants;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.dto.request.AssignEmployeesDto;
import com.example.timesheet.dto.request.ProjectRolesRequestDto;
import com.example.timesheet.dto.request.ProjectDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.dto.response.ProjectEmployeeDto;
import com.example.timesheet.dto.response.ProjectWithEmployeesDto;
import com.example.timesheet.service.ProjectManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;



import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class ProjectManagementController {

    private final ProjectManagementService projectManagementService;

    @PostMapping("/projects")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_ADD)
    public ResponseEntity<String> createProjects(@Valid @RequestBody ProjectDto projectCreateRequest) {
        try {
            String response = projectManagementService.createProject(projectCreateRequest);
            return ResponseEntity.ok(response);
        } catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/projects/roles")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_ADD)
    public ResponseEntity<String> createRolesInProject(@Valid @RequestBody ProjectRolesRequestDto dto){
        String response = projectManagementService.createRolesInProject(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/projects/roles/{roleId}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_ADD)
    public ResponseEntity<String> updateRolesInProject(@Valid @RequestBody ProjectRolesRequestDto dto, @PathVariable Long roleID){
        String response = projectManagementService.updateRolesInProject(dto, roleID);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/projects/roles/{roleId}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_ADD)
    public ResponseEntity<String> getRolesInProject(@PathVariable Long roleID){
        String response = projectManagementService.getRolesInProject(roleID);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/projects/roles/{roleId}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_ADD)
    public ResponseEntity<String> deleteRolesInProject(@PathVariable Long roleID){
        String response = projectManagementService.deleteRolesInProject(roleID);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/projects/roles/")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<List<String>> getAllRoleNames() {
        List<String> roleNames = projectManagementService.getAllRoleNames();
        return ResponseEntity.ok(roleNames);
    }

    @GetMapping("/projects")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<PagedResponse<ProjectResponseDto>> getAllProjects(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(projectManagementService.getAllProjects(offset, limit, filters, sorts));
    }

    @GetMapping("/projects/{projectCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<ProjectResponseDto> getProject(@PathVariable String projectCode) {
        try {
            return ResponseEntity.ok(projectManagementService.getProjectByCode(projectCode));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/projects/{projectCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.PROJECT_UPDATE)
    public ResponseEntity<String> updateProject(@PathVariable String projectCode, @Valid @RequestBody ProjectDto dto) {
        try {
            return ResponseEntity.ok(projectManagementService.updateProject(projectCode, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/projects/{projectCode}/status")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.PROJECT_UPDATE)
    public ResponseEntity<String> updatecostCenterStatus(
            @PathVariable String projectCode,
            @RequestParam boolean active) {

        String response = projectManagementService.updateProjectStatus(projectCode, active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("projects/{projectCode}/employees")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.PROJECT_EMPLOYEE_ASSIGN)
    public ResponseEntity<String> assignEmployees(@RequestBody AssignEmployeesDto dto, @PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.assignEmployeesToProject(dto, projectCode));
    }

    @GetMapping("/projects/{projectCode}/employees")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.PROJECT_EMPLOYEE_GET)
    public ResponseEntity<List<ProjectEmployeeDto>> getAssignedEmployees(@PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.getEmployeesByProject(projectCode));
    }

    @PutMapping("/projects/{projectCode}/employees/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.PROJECT_EMPLOYEE_UPDATE)
    public ResponseEntity<String> updateEmployeeSDED(
            @PathVariable String projectCode,
            @PathVariable String employeeCode,
            @RequestBody AssignEmployeesDto.EmployeeAssignment dto) {
        return ResponseEntity.ok(projectManagementService.updateEmployee(projectCode, employeeCode, dto));
    }

    @PutMapping("/projects/{projectCode}/employees/{employeeCode}/status")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.PROJECT_EMPLOYEE_UPDATE)
    public ResponseEntity<String> updateEmployeeStatus(
            @PathVariable String projectCode,
            @PathVariable String employeeCode,
            @RequestParam boolean active) {
        return ResponseEntity.ok(projectManagementService.updateEmployeeStatus(projectCode, employeeCode, active));
    }

    @GetMapping("/employees/{employeeCode}/projects")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_EMPLOYEE, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<List<ProjectDto>> getProjectsByEmployee(@PathVariable String employeeCode) {
        return ResponseEntity.ok(projectManagementService.getProjectsByEmployeeCode(employeeCode));
    }

    @GetMapping("/projects/{projectCode}/employees/unassigned")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_PM, scope = AuthorizationConstants.EMPLOYEE_GET)
    public ResponseEntity<List<Map<String, String>>> getUnassignedUsers(@PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.getUnassignedUsersForProject(projectCode));
    }

    @GetMapping("/managers/{managerCode}/projects")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_PM, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<List<ProjectWithEmployeesDto>> getProjectsUnderManager(@PathVariable String managerCode) {
        return ResponseEntity.ok(projectManagementService.getProjectsWithEmployeesUnderManager(managerCode));
    }
}
