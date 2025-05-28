package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.common.constants.AuthorizationConstants;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.service.CostCenterService;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tms")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    @PostMapping("/cost-centers")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCM, scope = AuthorizationConstants.COSTCENTER_ADD)
    public ResponseEntity<String> createCostCenter(@Valid @RequestBody CostCenterDto dto) {
        return ResponseEntity.ok(costCenterService.createCostCenter(dto));
    }

    @GetMapping("/cost-centers")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.COSTCENTER_GET)
    public ResponseEntity<PagedResponse<CostCenterResponseDto>> getAllCostCenters(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(name = "sort", required = false) String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);
        return ResponseEntity.ok(costCenterService.getAllCostCenters(offset, limit, filters, sorts));
    }

    @GetMapping("/cost-centers/all")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.COSTCENTER_GET)
    public ResponseEntity<List<CostCenterResponseDto>> getAllCostCenters() {
        return ResponseEntity.ok(costCenterService.getAllCostCentersProjects());
    }

    @GetMapping("/cost-centers/manager/{costCenterManagerCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_CCM, scope = AuthorizationConstants.COSTCENTER_GET)
    public ResponseEntity<List<CostCenterResponseDto>> getAllCostCentersUnderManager(@PathVariable String costCenterManagerCode) {
        return ResponseEntity.ok(costCenterService.getAllCostCentersUnderManager(costCenterManagerCode));
    }

    @GetMapping("/cost-centers/{costCenterCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCM, scope = AuthorizationConstants.COSTCENTER_GET)
    public ResponseEntity<CostCenterResponseDto> getCostCenterByCode(@PathVariable String costCenterCode) {
        try {
            return ResponseEntity.ok(costCenterService.getCostCenterByCode(costCenterCode));
        } catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }

    @PutMapping("/cost-centers/{costCenterCode}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCM, scope = AuthorizationConstants.COSTCENTER_UPDATE)
    public ResponseEntity<String> updateCostCenter(@PathVariable String costCenterCode, @Valid @RequestBody CostCenterDto dto) {
        try {
            return ResponseEntity.ok(costCenterService.updateCostCenter(costCenterCode, dto));
        } catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }

    @PutMapping("/cost-centers/{costCenterCode}/status")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCM, scope = AuthorizationConstants.COSTCENTER_UPDATE)
    public ResponseEntity<String> updateCostCenterStatus(@PathVariable String costCenterCode, @RequestParam boolean active) {
        String response = costCenterService.updateCostCenterStatus(costCenterCode, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cost-centers/{costCenterCode}/projects")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCM, scope = AuthorizationConstants.PROJECT_GET)
    public ResponseEntity<List<ProjectResponseDto>> getProjectsByCostCenter(@PathVariable String costCenterCode) {
        return ResponseEntity.ok(costCenterService.getProjectsByCostCenterCode(costCenterCode));
    }
}
