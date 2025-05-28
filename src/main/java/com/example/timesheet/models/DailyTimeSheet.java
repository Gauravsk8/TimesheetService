package com.example.timesheet.models;

import com.example.timesheet.common.audit.Audit;
import com.example.timesheet.enums.EntryType;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
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
                                DailyTimeSheet.PROJECT_CODE
                        }
                )
        }
)
public class DailyTimeSheet extends Audit {

    public static final String PROJECT_CODE = "projectCode";

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

    @Column(name = PROJECT_CODE, nullable = true)
    private String projectCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = PROJECT_CODE, referencedColumnName = PROJECT_CODE, insertable = false, updatable = false)
    private Project project;

    @Column(nullable = false)
    private Double hoursSpent;

    @Column(length = 1000)
    private String description;

    @Column
    private Boolean modifiedByManager = false;
}
