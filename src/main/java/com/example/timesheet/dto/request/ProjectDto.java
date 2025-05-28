package com.example.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ProjectDto {

    private String projectCode; // Optional: required only for updates

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start date is required")
    private Timestamp startDate;

    @NotNull(message = "End date is required")
    private Timestamp endDate;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotBlank(message = "Cost Center Code is required")
    private String costCenterCode;

    @NotBlank(message = "Project Manager Code is required")
    private String projectManagerCode;

    // Optional: Assuming allocatedHours is a string like "40.5"
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "Allocated hours must be a valid number")
    private String allocatedHours;
}
