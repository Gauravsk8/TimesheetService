package com.example.timesheet.dto.response;


import com.example.timesheet.enums.EntryType;
import com.example.timesheet.enums.TimeSheetStatus;
import lombok.Data;

import java.sql.Date;

@Data
public class DailyTimeSheetResponseDto {
    private String employeeCode;
    private Integer timesheetYear;
    private Integer timesheetMonth;
    private Date workDate;
    private String projectCode;
    private EntryType entryType;
    private Double hoursSpent;
    private String description;
    private TimeSheetStatus status;
    private Boolean modifiedByManager;

}
