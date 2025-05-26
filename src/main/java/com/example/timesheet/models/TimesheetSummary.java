package com.example.timesheet.models;

import com.example.timesheet.enums.TimeSheetStatus;
import com.example.timesheet.keys.TimesheetSummaryId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TimesheetSummary {

    @EmbeddedId
    private TimesheetSummaryId id;

    @Column(name = "total_hours")
    private Double totalHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeSheetStatus status;

    @Column(name = "submitted_date")
    private Timestamp submittedDate;

    @Column(name = "approved_by")
    private String approvedBy; // manager employeeCode

    @Column(length = 1000)
    private String managerComment;
}
