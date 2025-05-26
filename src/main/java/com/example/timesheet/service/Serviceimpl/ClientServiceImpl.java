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
import com.example.timesheet.Repository.ClientsRepository;
import com.example.timesheet.dto.request.ClientDto;
import com.example.timesheet.dto.response.ClientResponseDto;
import com.example.timesheet.models.Clients;
import com.example.timesheet.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientsRepository clientsRepository;

    @Override
    public String createClient(ClientDto dto) {
        Clients client = new Clients();
        client.setName(dto.getName());
        client.setContactPerson(dto.getContactPerson());
        client.setContactEmail(dto.getContactEmail());
        client.setAddress(dto.getAddress());
        client.isActive();
        Clients savedClient = clientsRepository.save(client);
        return String.format(MessageConstants.CLIENT_CREATED, savedClient.getId());
    }

    @Override
    public PagedResponse<ClientResponseDto> getAllClients(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts) {

        // Apply defaults if null
        int safeOffset = (offset == null) ? 0 : offset;
        int safeLimit = (limit == null || limit <= 0) ? 10 : limit;

        int page = safeOffset / safeLimit;

        Pageable pageable = PageRequest.of(page, safeLimit, SortUtil.getSort(sorts));

        Specification<Clients> spec = new FilterSpecificationBuilder<Clients>()
                .build(filters);

        Specification<Clients> isActiveSpec = (root, query, cb) ->
                cb.isTrue(root.get("isActive"));

        Specification<Clients> finalSpec = Specification.where(isActiveSpec).and(spec);

        Page<Clients> clientPage = clientsRepository.findAll(finalSpec, pageable);

        if (clientPage.isEmpty()) {
            throw new TimeSheetException(
                    ErrorCode.NOT_FOUND_ERROR,
                    ErrorMessage.NO_ACTIVE_CLIENTS_FOUND
            );
        }

        List<ClientResponseDto> content = clientPage.getContent().stream()
                .map(client -> new ClientResponseDto(
                        client.getId(),
                        client.getName(),
                        client.getContactPerson(),
                        client.getContactEmail(),
                        client.getAddress(),
                        client.isActive()
                )).toList();

        return new PagedResponse<>(
                content,
                clientPage.getNumber(),
                clientPage.getSize(),
                clientPage.getTotalElements()
        );
    }



    @Override
    public Optional<ClientResponseDto> getClientById(Long id) {
        Clients client = clientsRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.CLIENT_NOT_FOUND, id)));

        ClientResponseDto dto = new ClientResponseDto(
                client.getId(),
                client.getName(),
                client.getContactPerson(),
                client.getContactEmail(),
                client.getAddress(),
                client.isActive()
        );

        return Optional.of(dto);
    }


    @Override
    public String updateClient(Long id, ClientDto dto) throws TimeSheetException {
        Clients client = clientsRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.CLIENT_NOT_FOUND, id)));

        client.setName(dto.getName());
        client.setContactPerson(dto.getContactPerson());
        client.setContactEmail(dto.getContactEmail());
        client.setAddress(dto.getAddress());
        Clients savedClient = clientsRepository.save(client);
        return String.format(MessageConstants.CLIENT_UPDATED, savedClient.getName());
    }

    @Override
    public String updateClientStatus(Long id, boolean active) throws TimeSheetException {
        Clients client = clientsRepository.findById(id)
                .orElseThrow(() -> new TimeSheetException(
                        ErrorCode.NOT_FOUND_ERROR,
                        String.format(ErrorMessage.CLIENT_NOT_FOUND, id)));


        client.setActive(active);
        Clients savedClient = clientsRepository.save(client);
        return String.format(MessageConstants.CLIENT_STATUS_UPDATED, savedClient.getName());
    }
}
