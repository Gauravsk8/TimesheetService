package com.example.timesheet.controller;


import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.service.CostCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    //Create cost-center
    @PostMapping("/cost-centers")
    @RequiresKeycloakAuthorization(resource = "tms:adminccm", scope = "tms:costcenter:add")
    public ResponseEntity<String> createCostCenter(@Valid @RequestBody CostCenterDto dto) {
        return ResponseEntity.ok(costCenterService.createCostCenter(dto));
    }

    //Get All cost-center
    @GetMapping("/cost-centers")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:costcenter:get")
    public ResponseEntity<PagedResponse<CostCenterResponseDto>> getAllCostCenters(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(costCenterService.getAllCostCenters(offset, limit, filters, sorts));
    }

    @GetMapping("/cost-centers/all")
    @RequiresKeycloakAuthorization(resource = "tms:adminccmpm", scope = "tms:costcenter:get")
    public ResponseEntity<List<CostCenterResponseDto>> getAllCostCenters() {
        return ResponseEntity.ok(costCenterService.getAllCostCentersProjects());
    }


    //Get cost-center under CCM
    @GetMapping("/cost-centers/manager/{costCenterManagerCode}")
    @RequiresKeycloakAuthorization(resource = "tms:ccm", scope = "tms:costcenter:get")
    public ResponseEntity<List<CostCenterResponseDto>> getAllCostCentersUnderManager(@PathVariable String costCenterManagerCode) {
        return ResponseEntity.ok(costCenterService.getAllCostCentersUnderManager(costCenterManagerCode));
    }

    //Get cost-center By Code
    @GetMapping("/cost-centers/{costCenterCode}")
    @RequiresKeycloakAuthorization(resource = "tms:adminccm", scope = "tms:costcenter:get")
    public ResponseEntity<CostCenterResponseDto> getCostCenterByCode(@PathVariable String costCenterCode) {
        try {
            return ResponseEntity.ok(costCenterService.getCostCenterByCode(costCenterCode));
        }  catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }

    //Update CC by code
    @PutMapping("/cost-centers/{costCenterCode}")
    @RequiresKeycloakAuthorization(resource = "tms:adminccm", scope = "tms:costcenter:update")
    public ResponseEntity<String> updateCostCenter(@PathVariable String costCenterCode,@Valid @RequestBody CostCenterDto dto) {
        try {
            return ResponseEntity.ok(costCenterService.updateCostCenter(costCenterCode, dto));
        }  catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }

    //Delete CostCenter
    @PutMapping("/cost-centers/{costCenterCode}/status")
    @RequiresKeycloakAuthorization(resource = "tms:adminccm", scope = "tms:costcenter:update")
    public ResponseEntity<String> updatecostCenterStatus(
            @PathVariable String costCenterCode,
            @RequestParam boolean active) {

        String response = costCenterService.updateCostCenterStatus(costCenterCode, active);
        return ResponseEntity.ok(response);
    }

    //Get Projects under CC
    @GetMapping("/cost-centers/{costCenterCode}/projects")
    @RequiresKeycloakAuthorization(resource = "tms:adminccm", scope = "tms:project:get")
    public ResponseEntity<List<ProjectResponseDto>> getProjectsByCostCenter(@PathVariable String costCenterCode) {
        List<ProjectResponseDto> projects = costCenterService.getProjectsByCostCenterCode(costCenterCode);
        return ResponseEntity.ok(projects);
    }

}
