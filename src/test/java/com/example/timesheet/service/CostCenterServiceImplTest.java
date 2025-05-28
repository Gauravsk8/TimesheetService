package com.example.timesheet.service;

import com.example.timesheet.Repository.CostCenterRepository;
import com.example.timesheet.Repository.ProjectRepository;
import com.example.timesheet.client.IdentityServiceClient;
import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.pagenationDto.FilterRequest;
import com.example.timesheet.dto.pagenationDto.SortRequest;
import com.example.timesheet.dto.request.CostCenterDto;
import com.example.timesheet.dto.response.CostCenterResponseDto;
import com.example.timesheet.dto.response.ProjectResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.models.Clients;
import com.example.timesheet.models.CostCenter;
import com.example.timesheet.models.Project;
import com.example.timesheet.service.Serviceimpl.CostCenterServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CostCenterServiceImplTest {

    @Mock  private CostCenterRepository costCenterRepository;
    @Mock  private ProjectRepository     projectRepository;
    @Mock  private IdentityServiceClient identityServiceClient;  // not used but required for ctor
    @InjectMocks private CostCenterServiceImpl costCenterService;

    // ---------- reusable test data ----------
    private CostCenter       costCenter;
    private CostCenterDto    costCenterDto;
    private Project          project;

    @BeforeEach
    void setUp() {
        costCenter = CostCenter.builder()
                .costCenterCode("CC001")
                .name("Engineering")
                .description("Eng. Dept.")
                .costCenterManagerCode("EMP01")
                .isActive(true)
                .build();

        costCenterDto = new CostCenterDto();
        costCenterDto.setCostCenterCode("CC001");
        costCenterDto.setName("Engineering");
        costCenterDto.setDescription("Eng. Dept.");
        costCenterDto.setCostCenterManagerCode("EMP01");

        Clients client = new Clients();
        client.setName("ACME Inc.");

        project = Project.builder()
                .projectCode("PRJ01")
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

    // -------------------------------------------------------------------------
    //  createCostCenter
    // -------------------------------------------------------------------------
    @Test
    void createCostCenter_returnsMessageWithCode() {
        when(costCenterRepository.save(any(CostCenter.class))).thenReturn(costCenter);

        String msg = costCenterService.createCostCenter(costCenterDto);

        verify(costCenterRepository).save(any(CostCenter.class));
        assertThat(msg)
                .contains(String.format(MessageConstants.COST_CENTER_CREATED,"CC001")); // “CostCenter created…”

    }

    // -------------------------------------------------------------------------
    //  getAllCostCenters
    // -------------------------------------------------------------------------
    @Nested class GetAllCostCenters {

        @Test
        void returnsPagedResponse_whenPageNotEmpty() {
            Page<CostCenter> page =
                    new PageImpl<>(List.of(costCenter), PageRequest.of(0, 10), 1);

            when(costCenterRepository.findAll(
                    Mockito.<Specification<CostCenter>>any(),
                    Mockito.any(Pageable.class)))
                    .thenReturn(page);


            var response = costCenterService.getAllCostCenters(0, 10, List.of(), List.of());

            assertThat(response.getContent()).singleElement()
                    .extracting(CostCenterResponseDto::getCostCenterCode)
                    .isEqualTo("CC001");
        }

        @Test
        void throwsNotFound_whenNoActiveCostCenters() {
            when(costCenterRepository.findAll(
                    any(Specification.class),
                    any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThatThrownBy(() ->
                    costCenterService.getAllCostCenters(0, 10, List.of(), List.of()))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    // -------------------------------------------------------------------------
    //  getCostCenterByCode
    // -------------------------------------------------------------------------
    @Nested class GetCostCenterByCode {

        @Test
        void returnsDto_ifActive() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue("CC001"))
                    .thenReturn(Optional.of(costCenter));

            CostCenterResponseDto dto = costCenterService.getCostCenterByCode("CC001");

            assertThat(dto.getName()).isEqualTo("Engineering");
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

    // -------------------------------------------------------------------------
    //  updateCostCenter
    // -------------------------------------------------------------------------
    @Nested class UpdateCostCenter {

        @Test
        void updatesFieldsAndReturnsMessage() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue("CC001"))
                    .thenReturn(Optional.of(costCenter));
            when(costCenterRepository.save(any(CostCenter.class)))
                    .thenReturn(costCenter);

            String msg = costCenterService.updateCostCenter("CC001", costCenterDto);

            verify(costCenterRepository).save(argThat(c ->
                    "Engineering".equals(c.getName()) &&
                            "EMP01".equals(c.getCostCenterManagerCode())));
            assertThat(msg).contains(String.format(MessageConstants.COST_CENTER_UPDATED,"CC001"));
        }

        @Test
        void throwsNotFound_ifMissing() {
            when(costCenterRepository.findByCostCenterCodeAndIsActiveTrue("CC001"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    costCenterService.updateCostCenter("CC001", costCenterDto))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    // -------------------------------------------------------------------------
    //  updateCostCenterStatus
    // -------------------------------------------------------------------------
    @Nested class UpdateCostCenterStatus {

        @Test
        void togglesStatusAndReturnsMessage() {
            costCenter.setActive(false);
            when(costCenterRepository.findById("CC001")).thenReturn(Optional.of(costCenter));
            when(costCenterRepository.save(any(CostCenter.class))).thenReturn(costCenter);

            String msg = costCenterService.updateCostCenterStatus("CC001", true);

            verify(costCenterRepository).save(argThat(CostCenter::isActive));
            assertThat(msg).contains(String.format(MessageConstants.COST_CENTER_STATUS_UPDATED,"CC001"));
        }

        @Test
        void throwsNotFound_ifMissing() {
            when(costCenterRepository.findById("CC001")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    costCenterService.updateCostCenterStatus("CC001", true))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    // -------------------------------------------------------------------------
    //  getAllCostCentersUnderManager
    // -------------------------------------------------------------------------
    @Test
    void getAllCostCentersUnderManager_returnsMappedDtos() {
        when(costCenterRepository.findByCostCenterManagerCodeIgnoreCaseAndIsActiveTrue("EMP01"))
                .thenReturn(List.of(costCenter));

        List<CostCenterResponseDto> list =
                costCenterService.getAllCostCentersUnderManager("EMP01");

        assertThat(list).hasSize(1)
                .first()
                .extracting(CostCenterResponseDto::getCostCenterManagerCode)
                .isEqualTo("EMP01");
    }

    // -------------------------------------------------------------------------
    //  getProjectsByCostCenterCode
    // -------------------------------------------------------------------------
    @Test
    void getProjectsByCostCenterCode_returnsProjectsMapped() {
        when(projectRepository.findByCostCenter_CostCenterCodeIgnoreCaseAndIsActiveTrue("CC001"))
                .thenReturn(List.of(project));

        List<ProjectResponseDto> list =
                costCenterService.getProjectsByCostCenterCode("CC001");

        assertThat(list).singleElement()
                .extracting(ProjectResponseDto::getProjectCode)
                .isEqualTo("PRJ01");
    }

    // -------------------------------------------------------------------------
    //  Bean-validation of CostCenterDto
    // -------------------------------------------------------------------------
    @Test
    void costCenterDto_validation_violatesConstraints() {
        CostCenterDto invalid = new CostCenterDto();
        invalid.setName("Name_that_is_way_too_long_for_20_chars");
        invalid.setDescription("Desc that is definitely beyond the fifty-character limit we set in annotation");
        invalid.setCostCenterManagerCode("TOO_LONG_CODE");

        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            Validator validator = f.getValidator();
            Set violations = validator.validate(invalid);
            assertThat(violations).hasSize(3);
        }
    }
}
