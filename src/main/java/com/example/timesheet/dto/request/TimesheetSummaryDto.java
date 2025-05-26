package com.example.timesheet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Date;

@Data
public class TimesheetSummaryDto {
    @NotBlank(message = "Employee code must not be blank")
    private String employeeCode;

    @NotNull(message = "Timesheet year is required")
    @Min(value = 2000, message = "Year must be no earlier than 2000")
    private Integer timesheetYear;

    @NotNull(message = "Timesheet month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer timesheetMonth;

    @NotNull(message = "Week start date is required")
    private Date weekStart;
}
