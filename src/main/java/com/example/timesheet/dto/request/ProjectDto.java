package com.example.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ProjectDto {
    private String projectCode;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Timestamp startDate;

    private Timestamp endDate;

    private Long clientId;

    @NotBlank(message = "Cost Center Code is required")
    private String costCenterCode;

    @NotBlank(message = "Project manager can not be null")
    private String projectManagerCode;

    private String allocatedHours;
}

