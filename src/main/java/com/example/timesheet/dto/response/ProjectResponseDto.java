package com.example.timesheet.dto.response;


import com.example.timesheet.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Builder
public class ProjectResponseDto {
    private String projectCode;
    private String title;
    private String description;
    private Timestamp startDate;
    private Timestamp endDate;
    private String clientName;
    private String costCenterCode;
    private String projectManagerCode;
    private String allocatedHours;
    private boolean isActive;
}

