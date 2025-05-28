package com.example.timesheet.dto.request;

import com.example.timesheet.enums.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.sql.Date;

@Data
public class DailyTimesheetRequestDto {

    @NotBlank(message = "employeeCode is required")
    private String employeeCode;

    @NotNull(message = "timesheetYear is required")
    @Min(value = 2000, message = "timesheetYear must be >= 2000")
    private Integer timesheetYear;

    @NotNull(message = "timesheetMonth is required")
    @Min(value = 1, message = "timesheetMonth must be between 1 and 12")
    @Max(value = 12, message = "timesheetMonth must be between 1 and 12")
    private Integer timesheetMonth;

    @NotNull(message = "workDate is required")
    private Date workDate;

    private String projectCode;

    @NotNull(message = "entryType is required")
    private EntryType entryType;

    @NotNull(message = "hoursSpent is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "hoursSpent must be greater than 0")
    @DecimalMax(value = "24.0", message = "hoursSpent cannot exceed 24")
    private Double hoursSpent;

    @Size(max = 500, message = "description must be less than 500 characters")
    private String description;
}
