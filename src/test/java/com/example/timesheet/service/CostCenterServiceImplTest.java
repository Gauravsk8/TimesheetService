package com.example.timesheet.service;

import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.models.Clients;
import com.example.timesheet.models.CostCenter;
import com.example.timesheet.models.Project;
import com.example.timesheet.repository.CostCenterRepository;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.service.serviceimpl.CostCenterServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostCenterServiceImplTest {

    /* ───────── constants to avoid duplicate literals ───────── */
    private static final String CC_CODE = "CC001";
    private static final String CC_NAME = "Engineering";
    private static final String CCM_CODE = "EMP01";
    private static final String PROJ_CODE = "PRJ01";

    /* ───────── mocks & SUT ───────── */
    @Mock private CostCenterRepository costCenterRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IdentityServiceClient identityServiceClient; // required by constructor
    @InjectMocks private CostCenterServiceImpl costCenterService;

    /* ───────── reusable fixtures ───────── */
    private CostCenter costCenter;
    private CostCenterDto costCenterDto;
    private Project project;

    @BeforeEach
    void setUp() {
        costCenter = CostCenter.builder()
                .costCenterCode(CC_CODE)
                .name(CC_NAME)
                .description("Eng. Dept.")
                .costCenterManagerCode(CCM_CODE)
                .isActive(true)
                .build();

        costCenterDto = new CostCenterDto();
        costCenterDto.setCostCenterCode(CC_CODE);
        costCenterDto.setName(CC_NAME);
        costCenterDto.setDescription("Eng. Dept.");
        costCenterDto.setCostCenterManagerCode(CCM_CODE);

        Clients client = new Clients();
        client.setName("ACME Inc.");

        project = Project.builder()
                .projectCode(PROJ_CODE)
                .title("Apollo")
                .description("Next-gen platform")
                .startDate(Timestamp.valueOf(LocalDateTime.now()))
                .endDate(Timestamp.valueOf(LocalDateTime.now().plusDays(30)))
                .clients(client)
                .costCenter(costCenter)
                .projectManagerCode("EMP99")
                .isActive(true)
                .build();
    }

    /* ───────────────────────── createCostCenter ───────────────────────── */
    @Test
    void createCostCenter_returnsMessageWithCode() {
        when(costCenterRepository.save(any(CostCenter.class))).thenReturn(costCenter);

        String msg = costCenterService.createCostCenter(costCenterDto);

        verify(costCenterRepository).save(any(CostCenter.class));
        assertThat(msg).contains(String.format(MessageConstants.COST_CENTER_CREATED, CC_CODE));
    }

    /* ───────────────────────── getAllCostCenters ───────────────────────── */
    @Nested
    class GetAllCostCenters {

        @Test
        void returnsPagedResponse_whenPageNotEmpty() {
            Page<CostCenter> page = new PageImpl<>(
                    List.of(costCenter),
                    PageRequest.of(0, 10),
                    1
            );

            when(costCenterRepository.findAll(
                    org.mockito.Mockito.<Specification<CostCenter>>any(),
                    org.mockito.Mockito.any(Pageable.class)))
                    .thenReturn(page);

            var response = costCenterService.getAllCostCenters(0, 10, List.of(), List.of());

            assertThat(response.getContent()).singleElement()
                    .extracting(CostCenterResponseDto::getCostCenterCode)
                    .isEqualTo(CC_CODE);
        }

        @Test
        void throwsNotFound_whenNoActiveCostCenters() {
            when(costCenterRepository.findAll(
                    org.mockito.Mockito.<Specification<CostCenter>>any(),
                    org.mockito.Mockito.any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThatThrownBy(() ->
                    costCenterService.getAllCostCenters(0, 10, List.of(), List.of()))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────────── getCostCenterByCode ───────────────────────── */
    @Nested
    class GetCostCenterByCode {

        @Test
        void returnsDto_ifActive() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue(CC_CODE))
                    .thenReturn(Optional.of(costCenter));

            CostCenterResponseDto dto = costCenterService.getCostCenterByCode(CC_CODE);

            assertThat(dto.getName()).isEqualTo(CC_NAME);
        }

        @Test
        void throwsNotFound_ifMissing() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    costCenterService.getCostCenterByCode("BAD"))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    /* ───────────────────────── updateCostCenter ───────────────────────── */
    @Nested
    class UpdateCostCenter {

        @Test
        void updatesFieldsAndReturnsMessage() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue(CC_CODE))
                    .thenReturn(Optional.of(costCenter));
            when(costCenterRepository.save(any(CostCenter.class)))
                    .thenReturn(costCenter);

            String msg = costCenterService.updateCostCenter(CC_CODE, costCenterDto);

            verify(costCenterRepository).save(argThat(c ->
                    CC_NAME.equals(c.getName()) && CCM_CODE.equals(c.getCostCenterManagerCode())));
            assertThat(msg).contains(String.format(MessageConstants.COST_CENTER_UPDATED, CC_CODE));
        }

        @Test
        void throwsNotFound_ifMissing() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue(CC_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    costCenterService.updateCostCenter(CC_CODE, costCenterDto))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    /* ───────────────────────── updateCostCenterStatus ───────────────────────── */
    @Nested
    class UpdateCostCenterStatus {

        @Test
        void togglesStatusAndReturnsMessage() {
            costCenter.setActive(false);
            when(costCenterRepository.findById(CC_CODE)).thenReturn(Optional.of(costCenter));
            when(costCenterRepository.save(any(CostCenter.class))).thenReturn(costCenter);

            String msg = costCenterService.updateCostCenterStatus(CC_CODE, true);

            verify(costCenterRepository).save(argThat(CostCenter::isActive));
            assertThat(msg).contains(String.format(MessageConstants.COST_CENTER_STATUS_UPDATED, CC_CODE));
        }

        @Test
        void throwsNotFound_ifMissing() {
            when(costCenterRepository.findById(CC_CODE)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    costCenterService.updateCostCenterStatus(CC_CODE, true))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────────── getAllCostCentersUnderManager ───────────────────────── */
    @Test
    void getAllCostCentersUnderManager_returnsMappedDtos() {
        when(costCenterRepository.findByCostCenterManagerCodeIgnoreCaseAndIsActiveTrue(CCM_CODE))
                .thenReturn(List.of(costCenter));

        List<CostCenterResponseDto> list =
                costCenterService.getAllCostCentersUnderManager(CCM_CODE);

        assertThat(list).singleElement()
                .extracting(CostCenterResponseDto::getCostCenterManagerCode)
                .isEqualTo(CCM_CODE);
    }

    /* ───────────────────────── getProjectsByCostCenterCode ───────────────────────── */
    @Test
    void getProjectsByCostCenterCode_returnsProjectsMapped() {
        when(projectRepository.findByCostCenter_CostCenterCodeIgnoreCaseAndIsActiveTrue(CC_CODE))
                .thenReturn(List.of(project));

        List<ProjectResponseDto> list =
                costCenterService.getProjectsByCostCenterCode(CC_CODE);

        assertThat(list).singleElement()
                .extracting(ProjectResponseDto::getProjectCode)
                .isEqualTo(PROJ_CODE);
    }

    /* ───────────────────────── Bean-validation for CostCenterDto ───────────────────────── */
    @Test
    void costCenterDto_validation_violatesConstraints() {
        CostCenterDto invalid = new CostCenterDto();
        invalid.setName("Name_that_is_way_too_long_for_20_chars");
        invalid.setDescription(
                "Desc that is definitely beyond the fifty-character limit we set in annotation");
        invalid.setCostCenterManagerCode("TOO_LONG_CODE");

        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            Validator validator = f.getValidator();
            Set<?> violations = validator.validate(invalid);
            assertThat(violations).hasSize(3);
        }
    }
}
