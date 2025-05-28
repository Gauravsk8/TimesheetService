package com.example.timesheet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import lombok.Data;

import java.sql.Date;
import java.util.List;

@Data
public class DailyTimesheetDto {

    @NotEmpty(message = "dailyEntry list must contain at least one entry")
    private List<@Valid DailyTimesheetRequestDto> dailyEntry;

    @NotBlank(message = "employeeCode is required")
    private String employeeCode;

    @NotNull(message = "timesheetYear is required")
    @Min(value = 2000, message = "timesheetYear must be >= 2000")
    private Integer timesheetYear;

    @NotNull(message = "timesheetMonth is required")
    @Min(value = 1, message = "timesheetMonth must be between 1 and 12")
    @Max(value = 12, message = "timesheetMonth must be between 1 and 12")
    private Integer timesheetMonth;

    @NotNull(message = "weekStart is required")
    private Date weekStart;
}
