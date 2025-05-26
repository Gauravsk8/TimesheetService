package com.example.timesheet.models;


import com.example.timesheet.common.audit.Audit;
import com.example.timesheet.enums.EntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "daily_time_sheet",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timesheet_business_key",
                        columnNames = {
                                "employeeCode",
                                "timesheet_year",
                                "timesheet_month",
                                "workDate",
                                "entryType",
                                "projectCode"
                        }
                )
        }
)
public class DailyTimeSheet extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employeeCode", nullable = false)
    private String employeeCode;

    @Column(name = "timesheet_year", nullable = false)
    private Integer timesheetYear;

    @Column(name = "timesheet_month", nullable = false)
    private Integer timesheetMonth;

    @Column(name = "workDate", nullable = false)
    private Date workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entryType")
    private EntryType entryType;

    @Column(name = "projectCode", nullable = true)
    private String projectCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "projectCode", referencedColumnName = "projectCode", insertable = false, updatable = false)
    private Project project;

    @Column(nullable = false)
    private Double hoursSpent;

    @Column(length = 1000)
    private String description;

    @Column
    private Boolean modifiedByManager = false;
}

