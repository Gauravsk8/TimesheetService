package com.example.timesheet.service;

import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.common.constants.MessageConstants;
import com.example.timesheet.dto.request.ClientDto;
import com.example.timesheet.dto.response.ClientResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.models.Clients;
import com.example.timesheet.repository.ClientsRepository;
import com.example.timesheet.service.serviceimpl.ClientServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    private static final String ACME = "ACME Inc.";

    @Mock private ClientsRepository clientsRepository;
    @InjectMocks private ClientServiceImpl clientService;

    private Clients clientEntity;
    private ClientDto clientDto;

    @BeforeEach
    void setUp() {
        clientEntity = new Clients();
        clientEntity.setId(1L);
        clientEntity.setName(ACME);
        clientEntity.setContactPerson("Alice");
        clientEntity.setContactEmail("alice@acme.test");
        clientEntity.setAddress("Silicon Valley");
        clientEntity.setActive(true);

        clientDto = new ClientDto(
                ACME,
                "Alice",
                "alice@acme.test",
                "Silicon Valley"
        );
    }

    @Test
    void createClient_savesEntityAndReturnsMessage() {
        when(clientsRepository.save(any(Clients.class))).thenReturn(clientEntity);

        String message = clientService.createClient(clientDto);

        verify(clientsRepository).save(any(Clients.class));
        assertThat(message).contains(String.format(MessageConstants.CLIENT_CREATED, "1"));
    }

    /* ───────────────────────── getAllClients ───────────────────────── */
    @Nested
    class GetAllClients {

        @Test
        void returnsPagedResponse_whenClientsFound() {
            Page<Clients> page = new PageImpl<>(
                    List.of(clientEntity),
                    PageRequest.of(0, 10),
                    1
            );

            when(clientsRepository.findAll(
                    org.mockito.Mockito.<Specification<Clients>>any(),
                    org.mockito.Mockito.any(Pageable.class)))
                    .thenReturn(page);

            var response = clientService.getAllClients(0, 10, List.of(), List.of());

            assertThat(response.getContent())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ClientResponseDto.class)
                    .extracting(ClientResponseDto::getId)
                    .isEqualTo(1L);
        }

        @Test
        void throwsNotFound_whenPageEmpty() {
            when(clientsRepository.findAll(
                    org.mockito.Mockito.<Specification<Clients>>any(),
                    org.mockito.Mockito.any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThatThrownBy(() -> clientService.getAllClients(0, 10, List.of(), List.of()))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────────── getClientById ───────────────────────── */
    @Nested
    class GetClientById {

        @Test
        void returnsDto_whenClientActive() {
            when(clientsRepository.findByIdAndIsActiveTrue(1L))
                    .thenReturn(Optional.of(clientEntity));

            var dtoOpt = clientService.getClientById(1L);

            assertThat(dtoOpt).isPresent();
            assertThat(dtoOpt.get().getName()).isEqualTo(ACME);
        }

        @Test
        void throwsNotFound_whenMissing() {
            when(clientsRepository.findByIdAndIsActiveTrue(anyLong()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.getClientById(99L))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────────── updateClient ───────────────────────── */
    @Nested
    class UpdateClient {

        @Test
        void updatesFieldsAndReturnsMessage() {
            when(clientsRepository.findByIdAndIsActiveTrue(1L))
                    .thenReturn(Optional.of(clientEntity));
            when(clientsRepository.save(any(Clients.class)))
                    .thenReturn(clientEntity);

            String msg = clientService.updateClient(1L, clientDto);

            assertThat(msg).isEqualTo("Client ACME Inc. updated");

            verify(clientsRepository).save(argThat(
                    c -> ACME.equals(c.getName()) && "Alice".equals(c.getContactPerson())
            ));
        }

        @Test
        void throwsNotFound_whenClientMissing() {
            when(clientsRepository.findByIdAndIsActiveTrue(1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClient(1L, clientDto))
                    .isInstanceOf(TimeSheetException.class);
        }
    }

    /* ───────────────────────── updateClientStatus ───────────────────────── */
    @Nested
    class UpdateClientStatus {

        @Test
        void togglesStatusAndReturnsMessage() {
            clientEntity.setActive(false);
            when(clientsRepository.findById(1L)).thenReturn(Optional.of(clientEntity));
            when(clientsRepository.save(any(Clients.class))).thenReturn(clientEntity);

            String msg = clientService.updateClientStatus(1L, true);

            verify(clientsRepository).save(argThat(Clients::isActive));
            assertThat(msg).isEqualTo("Client ACME Inc. status updated");
        }

        @Test
        void throwsNotFound_whenClientMissing() {
            when(clientsRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClientStatus(1L, true))
                    .isInstanceOf(TimeSheetException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /* ───────────────────────── bean-validation smoke test ───────────────────────── */
    @Test
    void clientDto_validation_onInvalidFields() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            ClientDto invalid = new ClientDto(
                    "This-name-is-WAY-too-long-for-the-max-size-of-20",
                    "AlsoTooLongForContactPerson",
                    "not-an-email",
                    "A very very very very very very very very long address"
            );

            Set<?> violations = validator.validate(invalid);
            assertThat(violations).hasSize(4); // name, contactPerson, email, address
        }
    }
}
