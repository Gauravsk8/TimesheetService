package com.example.timesheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityDto {
    private String keycloakUserId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String employeeType;
    private String managerCode;
}