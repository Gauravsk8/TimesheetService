package com.example.timesheet.dto.response;


import lombok.Builder;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Builder
public class ProjectEmployeeDto {
    private String employeeCode;
    private String firstName;
    private String lastName;
    private Timestamp startDate;
    private Timestamp endDate;
    private boolean isActive;
}

