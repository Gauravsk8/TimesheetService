package com.example.timesheet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Data
public class ManagerApprovalRequestDto {

    @NotBlank(message = "employeeCode is required")
    private String employeeCode;

    @NotNull(message = "timesheetYear is required")
    @Min(value = 2000, message = "timesheetYear must be >= 2000")
    private Integer timesheetYear;

    @NotNull(message = "timesheetMonth is required")
    @Min(value = 1, message = "timesheetMonth must be between 1 and 12")
    @Max(value = 12, message = "timesheetMonth must be between 1 and 12")
    private Integer timesheetMonth;

    @NotNull(message = "weekStart date is required")
    private Date weekStart;

    private boolean approve;


    private String managerCode;

    @Size(max = 500, message = "comment must be less than 500 characters")
    private String comment;

    private Double hours;

    @NotNull(message = "dailyTimeSheetRequests cannot be null")
    @Size(min = 1, message = "At least one daily timesheet entry is required")
    private List<@Valid DailyTimesheetRequestDto> dailyTimeSheetRequests = new ArrayList<>();
}
