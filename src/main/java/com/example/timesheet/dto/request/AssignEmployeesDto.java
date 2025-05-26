package com.example.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
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

        private Timestamp startDate;

        private Timestamp endDate;

        @NotBlank(message = "role in project")
        private String role_in_project;
    }
}

