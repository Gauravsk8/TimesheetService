package com.example.timesheet.dto.response;


import com.example.timesheet.enums.Status;
import lombok.Builder;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Builder
public class ProjectEmployeeDto {
    private String employeeCode;
    private String firstName;
    private String lastName;
    private Timestamp startDate;  // New
    private Timestamp endDate;    // New
    private boolean isActive;
}

