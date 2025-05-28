package com.example.timesheet.service;


import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.dto.request.ClientDto;
import com.example.timesheet.dto.response.ClientResponseDto;
import com.example.timesheet.exceptions.TimeSheetException;

import java.util.List;
import java.util.Optional;

public interface ClientService {
    String createClient(ClientDto dto);
    PagedResponse<ClientResponseDto> getAllClients(
            Integer offset,
            Integer limit,
            List<FilterRequest> filters,
            List<SortRequest> sorts);
    Optional<ClientResponseDto> getClientById(Long id);
    String updateClient(Long id, ClientDto dto) throws TimeSheetException;
    String updateClientStatus(Long id, boolean active) throws TimeSheetException;
    List<ClientResponseDto> getAllClientsProject();
}