package com.example.timesheet.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Date;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetSummaryId implements Serializable{

    @Column(name = "employeeCode", nullable = false)
    private String employeeCode;

    @Column(name = "timesheet_year", nullable = false)
    private Integer timesheetYear;

    @Column(name = "timesheet_month", nullable = false)
    private Integer timesheetMonth;

    @Column(name = "week_start", nullable = false)
    private Date weekStart;
}
