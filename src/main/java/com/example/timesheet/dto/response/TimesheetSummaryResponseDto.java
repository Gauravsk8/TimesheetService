package com.example.timesheet.dto.response;

import com.example.timesheet.enums.TimeSheetStatus;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class TimesheetSummaryResponseDto {
    private String employeeCode;
    private Integer timesheetYear;
    private Integer timesheetMonth;
    private Date weekStart;
    private Double totalHours;
    private TimeSheetStatus status;
    private Timestamp submittedDate;
    private String approvedBy;
    private String managerComment;
}
