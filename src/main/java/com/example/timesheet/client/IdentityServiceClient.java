package com.example.timesheet.client;

import com.example.timesheet.config.FeignClientConfig;
import com.example.timesheet.dto.response.UserIdentityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "IdentityServiceClient",
        url = "${identity.service.url:http://localhost:8091}",
        configuration = FeignClientConfig.class
)
public interface IdentityServiceClient {

    @GetMapping("/ims/users/{employeeCode}")
    ResponseEntity<UserIdentityDto> getUserByemployeeCode(@PathVariable("employeeCode") String employeeCode);

    @GetMapping("/ims/users/{employeeCode}/manager-roles")
    ResponseEntity<Boolean> hasManagerRole(@PathVariable("employeeCode") String employeeCode, @RequestParam String roleName);

    @GetMapping("/ims/users/all")
    ResponseEntity<List<Map<String, String>>> getAllUsersList();

    @GetMapping("/ims/users/{employee_code}/manager")
    ResponseEntity<String> getManagerNameByEmployeeCode(
            @PathVariable String employeeCode
    );
    @GetMapping("/ims/users/manager/{managerCode}")
    ResponseEntity<List<UserIdentityDto>> getEmployeesUnderManager(@PathVariable String managerCode);
}
