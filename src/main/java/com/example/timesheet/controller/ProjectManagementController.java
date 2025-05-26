package com.example.timesheet.controller;


import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.dto.request.AssignEmployeesDto;
import com.example.timesheet.dto.request.ProjectRolesRequestDto;
import com.example.timesheet.dto.request.ProjectDto;
import com.example.timesheet.dto.response.*;
import com.example.timesheet.service.ProjectManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class ProjectManagementController {

    private final ProjectManagementService projectManagementService;


    //Create Project
    @PostMapping("/projects")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:add")
    public ResponseEntity<String> createProjects(@Valid  @RequestBody ProjectDto projectCreateRequest) {
        try {
            String response = projectManagementService.createProject(projectCreateRequest);
            return ResponseEntity.ok(response);
        } catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }


    //Create Project roles
    @PostMapping("/projects/roles")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:add")
    public ResponseEntity<String> createRolesInProject(@Valid @RequestBody ProjectRolesRequestDto dto){
        String response=projectManagementService.createRolesInProject(dto);
        return ResponseEntity.ok(response);
    }

    //Get all ProjectRoles
    @GetMapping("/projects/roles")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:get")
    public ResponseEntity<List<String>> getAllRoleNames() {
        List<String> roleNames = projectManagementService.getAllRoleNames();
        return ResponseEntity.ok(roleNames);
    }

    //Get All Project
    @GetMapping("/projects")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:get")
    public ResponseEntity<PagedResponse<ProjectResponseDto>> getAllProjects(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(projectManagementService.getAllProjects(offset, limit, filters, sorts));
    }

    //Get Project By Code
    @GetMapping("/projects/{projectCode}")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:get")
    public ResponseEntity<ProjectResponseDto> getProject(@PathVariable String projectCode) {
        try {
            return ResponseEntity.ok(projectManagementService.getProjectByCode(projectCode));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    //Update Project
    @PutMapping("/projects/{projectCode}")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:project:update")
    public ResponseEntity<String> updateProject(@PathVariable String projectCode,@Valid @RequestBody ProjectDto dto) {
        try {
            return ResponseEntity.ok(projectManagementService.updateProject(projectCode, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    //Delete Project
    @PutMapping("/projects/{projectCode}/status")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:project:update")
    public ResponseEntity<String> updatecostCenterStatus(
            @PathVariable String projectCode,
            @RequestParam boolean active) {

        String response = projectManagementService.updateProjectStatus(projectCode, active);
        return ResponseEntity.ok(response);
    }


    //Assign Employee to Project
    @PostMapping("projects/{projectCode}/employees")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:project:employee:assign")
    public ResponseEntity<String> assignEmployees(@RequestBody AssignEmployeesDto dto, @PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.assignEmployeesToProject(dto, projectCode));
    }


    //Assigned Employees Under Project
    @GetMapping("/projects/{projectCode}/employees")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:project:employee:get")
    public ResponseEntity<List<ProjectEmployeeDto>> getAssignedEmployees(@PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.getEmployeesByProject(projectCode));
    }


    //Update start date,  end date
    @PutMapping("/projects/{projectCode}/employees/{employeeCode}")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:project:employee:update")
    public ResponseEntity<String> updateEmployeeSDED(
            @PathVariable String projectCode,
            @PathVariable String employeeCode,
            @RequestBody AssignEmployeesDto.EmployeeAssignment dto) {
        return ResponseEntity.ok(projectManagementService.updateEmployee(projectCode, employeeCode, dto));
    }

    //Unassign employee from Project
    @PutMapping("/projects/{projectCode}/employees/{employeeCode}/status")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:project:employee:update")
    public ResponseEntity<String> updateEmployeeStatus(
            @PathVariable String projectCode,
            @PathVariable String employeeCode,
            @RequestParam boolean active) {
        return ResponseEntity.ok(projectManagementService.updateEmployeeStatus(projectCode, employeeCode, active));
    }

    //Projects assigned to Employee
    @GetMapping("/employees/{employeeCode}/projects")
    @RequiresKeycloakAuthorization(resource = "tms:employee", scope = "tms:project:get")
    public ResponseEntity<List<ProjectDto>> getProjectsByEmployee(@PathVariable String employeeCode) {
        return ResponseEntity.ok(projectManagementService.getProjectsByEmployeeCode(employeeCode));
    }

    //Get unassigned employees list
    @GetMapping("/projects/{projectCode}/employees/unassigned")
    @RequiresKeycloakAuthorization(resource = "tms:adminpm", scope = "tms:employee:get")
    public ResponseEntity<List<Map<String, String>>> getUnassignedUsers(@PathVariable String projectCode) {
        return ResponseEntity.ok(projectManagementService.getUnassignedUsersForProject(projectCode));
    }

    //Get Projects under Manager
    @GetMapping("/managers/{managerCode}/projects")
    @RequiresKeycloakAuthorization(resource = "tms:pm", scope = "tms:project:get")
    public ResponseEntity<List<ProjectWithEmployeesDto>> getProjectsUnderManager(@PathVariable String managerCode) {
        return ResponseEntity.ok(projectManagementService.getProjectsWithEmployeesUnderManager(managerCode));
    }
}
