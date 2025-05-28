package com.example.timesheet.service;

import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;

import java.util.List;

public interface CostCenterService {
    String createCostCenter(CostCenterDto dto) throws TimeSheetException;
    PagedResponse<CostCenterResponseDto> getAllCostCenters(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts);

    CostCenterResponseDto getCostCenterByCode(String costCenterCode) throws TimeSheetException;
    String updateCostCenter(String costCenterCode, CostCenterDto dto) throws TimeSheetException;
    String updateCostCenterStatus(String costCenterCode, boolean newStatus) throws TimeSheetException;
    List<CostCenterResponseDto> getAllCostCentersUnderManager(String costCenterManagerCode);
    List<ProjectResponseDto> getProjectsByCostCenterCode(String costCenterCode);

    List<CostCenterResponseDto> getAllCostCentersProjects();

}