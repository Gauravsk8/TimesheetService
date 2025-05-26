package com.example.timesheet.service;


import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.dto.request.AssignEmployeesDto;
import com.example.timesheet.dto.request.ProjectRolesRequestDto;
import com.example.timesheet.dto.request.ProjectDto;
import com.example.timesheet.dto.response.ProjectEmployeeDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.dto.response.ProjectWithEmployeesDto;
import com.example.timesheet.exceptions.TimeSheetException;

import java.util.List;
import java.util.Map;

public interface ProjectManagementService {
    String createProject(ProjectDto dto) throws TimeSheetException;

    String createRolesInProject(ProjectRolesRequestDto createRoleInProjectRequestDto);
    List<String> getAllRoleNames();


    PagedResponse<ProjectResponseDto> getAllProjects(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts);

    ProjectResponseDto getProjectByCode(String code) throws TimeSheetException;
    String updateProject(String code, ProjectDto dto) throws TimeSheetException;
    String updateProjectStatus(String projectCode, boolean active);
    String assignEmployeesToProject(AssignEmployeesDto dto, String projectCode) throws TimeSheetException;
    List<ProjectEmployeeDto> getEmployeesByProject(String projectCode) throws TimeSheetException;
    ProjectWithEmployeesDto getProjectWithEmployees(String projectCode) throws TimeSheetException;
    String updateEmployeeStatus(String projectCode, String employeeCode, boolean newStatus) throws TimeSheetException;
    String updateEmployee(String projectCode, String employeeCode, AssignEmployeesDto.EmployeeAssignment dto) throws TimeSheetException;
    List<ProjectDto> getProjectsByEmployeeCode(String employeeCode);
    List<Map<String, String>> getUnassignedUsersForProject(String projectCode) throws TimeSheetException;
    List<ProjectWithEmployeesDto> getProjectsWithEmployeesUnderManager(String projectManagerCode);
}