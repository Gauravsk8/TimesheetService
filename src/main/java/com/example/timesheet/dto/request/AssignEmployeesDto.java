package com.example.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class AssignEmployeesDto {

    private List<EmployeeAssignment> employees;

    @Data
    public static class EmployeeAssignment {

        @NotBlank(message = "employeeCode is required")
        private String employeeCode;

        @NotNull(message = "startDate is required")
        private Timestamp startDate;

        @NotNull(message = "endDate is required")
        private Timestamp endDate;

        @NotBlank(message = "role in project")
        private String role_in_project;
    }
}

