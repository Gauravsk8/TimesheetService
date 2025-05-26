package com.example.timesheet.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRolesRequestDto {

    @NotBlank(message = "role name is required")
    private String roleName;
}
