package com.example.timesheet.service.Serviceimpl;



import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.ErrorMessage;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.pagenationDto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterSpecificationBuilder;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.Repository.CostCenterRepository;
import com.example.timesheet.Repository.ProjectRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.models.CostCenter;
import com.example.timesheet.models.Project;
import com.example.timesheet.service.CostCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostCenterServiceImpl implements CostCenterService {
    private final CostCenterRepository costCenterRepository;
    private final ProjectRepository projectRepository;
    private final IdentityServiceClient identityServiceClient;

    private final String CostCenterManager = "CostCenterManager";

    @Override
    public String createCostCenter(CostCenterDto dto) throws TimeSheetException {
        CostCenter costCenter = CostCenter.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .costCenterManagerCode(dto.getCostCenterManagerCode())
                .build();

        CostCenter savedCostCenter = costCenterRepository.save(costCenter);
        return String.format(MessageConstants.COST_CENTER_CREATED, savedCostCenter.getCostCenterCode());
    }

    @Override
    public PagedResponse<CostCenterResponseDto> getAllCostCenters(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        int safeOffset = (offset == null) ? 0 : offset;
        int safeLimit = (limit == null || limit <= 0) ? 10 : limit;
        int page = safeOffset / safeLimit;

        Pageable pageable = PageRequest.of(page, safeLimit, SortUtil.getSort(sorts));

        Specification<CostCenter> dynamicSpec = new FilterSpecificationBuilder<CostCenter>().build(filters);
        Specification<CostCenter> isActiveSpec = (root, query, cb) -> cb.isTrue(root.get("isActive"));

        Specification<CostCenter> finalSpec = Specification.where(isActiveSpec).and(dynamicSpec);

        Page<CostCenter> costCenterPage = costCenterRepository.findAll(finalSpec, pageable);

        if (costCenterPage.isEmpty()) {
            throw new TimeSheetException(
                    ErrorCode.NOT_FOUND_ERROR,
                    ErrorMessage.NO_ACTIVE_COST_CENTERS_FOUND
            );
        }

        List<CostCenterResponseDto> content = costCenterPage.getContent().stream()
                .map(this::mapToCostCenterResponseDto)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                costCenterPage.getNumber(),
                costCenterPage.getSize(),
                costCenterPage.getTotalElements()
        );
    }

    @Override
    public CostCenterResponseDto getCostCenterByCode(String costCenterCode) throws TimeSheetException {
        CostCenter costCenter = costCenterRepository.findByCostCenterCodeAndIsActiveTrue(costCenterCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.COST_CENTER_NOT_FOUND, costCenterCode)
                ));
        return mapToCostCenterResponseDto(costCenter);
    }

    @Override
    public String updateCostCenter(String costCenterCode, CostCenterDto dto) throws TimeSheetException {
        CostCenter costCenter = costCenterRepository.findByCostCenterCodeAndIsActiveTrue(costCenterCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.COST_CENTER_NOT_FOUND, costCenterCode)
                ));

        costCenter.setName(dto.getName());
        costCenter.setDescription(dto.getDescription());
        costCenter.setCostCenterManagerCode(dto.getCostCenterManagerCode());

        CostCenter saveCostCenter = costCenterRepository.save(costCenter);
        return String.format(MessageConstants.COST_CENTER_UPDATED, saveCostCenter.getCostCenterCode());
    }

    @Override
    public String updateCostCenterStatus(String costCenterCode, boolean active) throws TimeSheetException {
        CostCenter costCenter = costCenterRepository.findById(costCenterCode)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.COST_CENTER_NOT_FOUND, costCenterCode)
                ));

        costCenter.setActive(active);
        CostCenter saveCostCenter = costCenterRepository.save(costCenter);
        return String.format(MessageConstants.COST_CENTER_STATUS_UPDATED, saveCostCenter.getCostCenterCode());
    }

    @Override
    public List<CostCenterResponseDto> getAllCostCentersUnderManager(String costCenterManagerCode) {
        return costCenterRepository.findByCostCenterManagerCodeIgnoreCaseAndIsActiveTrue
                        (costCenterManagerCode)
                .stream()
                .map(this::mapToCostCenterResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> getProjectsByCostCenterCode(String costCenterCode) {
        return projectRepository.findByCostCenter_CostCenterCodeIgnoreCaseAndIsActiveTrue(costCenterCode)
                .stream()
                .map(this::mapToProjectResponseDto)
                .collect(Collectors.toList());
    }

    private CostCenterResponseDto mapToCostCenterResponseDto(CostCenter costCenter) {
        return new CostCenterResponseDto(
                costCenter.getCostCenterCode(),
                costCenter.getName(),
                costCenter.getDescription(),
                costCenter.getCostCenterManagerCode(),
                costCenter.isActive()
        );
    }

    private ProjectResponseDto mapToProjectResponseDto(Project project) {
        return ProjectResponseDto.builder()
                .projectCode(project.getProjectCode())
                .title(project.getTitle())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .clientName(project.getClients().getName())
                .costCenterCode(project.getCostCenter().getCostCenterCode())
                .projectManagerCode(project.getProjectManagerCode())
                .isActive(project.isActive())
                .build();
    }

    @Override
    public List<CostCenterResponseDto> getAllCostCentersProjects() {
        List<CostCenter> activeCostCenters = costCenterRepository.findByIsActiveTrue();

        if (activeCostCenters.isEmpty()) {
            throw new TimeSheetException(
                    ErrorCode.NOT_FOUND_ERROR,
                    ErrorMessage.NO_ACTIVE_COST_CENTERS_FOUND
            );
        }

        return activeCostCenters.stream()
                .map(this::mapToCostCenterResponseDto)
                .collect(Collectors.toList());
    }

}