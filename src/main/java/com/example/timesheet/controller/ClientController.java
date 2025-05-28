package com.example.timesheet.controller;

import com.example.timesheet.common.annotations.RequiresKeycloakAuthorization;
import com.example.timesheet.dto.paginationdto.FilterRequest;
import com.example.timesheet.dto.paginationdto.SortRequest;
import com.example.timesheet.dto.paginationdto.response.PagedResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.example.timesheet.utils.FilterUtil;
import com.example.timesheet.utils.SortUtil;
import com.example.timesheet.dto.request.ClientDto;
import com.example.timesheet.dto.response.ClientResponseDto;
import com.example.timesheet.service.ClientService;
import com.example.timesheet.common.constants.AuthorizationConstants;
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
public class ClientController {

    private final ClientService clientService;

    //Create Client
    @PostMapping("/clients")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN, scope = AuthorizationConstants.CLIENT_ADD)
    public ResponseEntity<String> createClient(@Valid @RequestBody ClientDto clientDto) {
        String response = clientService.createClient(clientDto);
        return ResponseEntity.ok(response);
    }

    //Get All Client
    @GetMapping("/clients")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.CLIENT_GET)
    public ResponseEntity<PagedResponse<ClientResponseDto>> getAllClients(
            @RequestParam int offset,
            @RequestParam int limit,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false, name = "sort") String sortParam) {

        List<FilterRequest> filters = FilterUtil.parseFilters(allParams);
        List<SortRequest> sorts = SortUtil.parseSort(sortParam);

        return ResponseEntity.ok(clientService.getAllClients(offset, limit, filters, sorts));
    }

    //Get Client By ID
    @GetMapping("/clients/{id}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN, scope = AuthorizationConstants.CLIENT_GET)
    public ResponseEntity<ClientResponseDto> getClientById(@PathVariable Long id) {
        return clientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Update Client
    @PutMapping("/clients/{id}")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN, scope = AuthorizationConstants.CLIENT_UPDATE)
    public ResponseEntity<String> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDto clientDto) {
        try {
            String updatedClient = clientService.updateClient(id, clientDto);
            return ResponseEntity.ok(updatedClient);
        } catch (TimeSheetException e) {
            throw new TimeSheetException(e.getErrorCode(), e.getMessage());
        }
    }
    
    //Delete Client
    @PutMapping("/clients/{id}/status")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN, scope = AuthorizationConstants.CLIENT_UPDATE)
    public ResponseEntity<String> updateClientStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        String response = clientService.updateClientStatus(id, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clients/all")
    @RequiresKeycloakAuthorization(resource = AuthorizationConstants.TMS_ADMIN_CCMPM, scope = AuthorizationConstants.CLIENT_GET)
    public ResponseEntity<List<ClientResponseDto>> getAllClients() {
        List<ClientResponseDto> response = clientService.getAllClientsProject();
        return ResponseEntity.ok(response);
    }

}

