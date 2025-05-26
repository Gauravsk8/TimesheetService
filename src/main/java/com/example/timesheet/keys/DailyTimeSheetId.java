//package com.example.timesheet.keys;
//
//import com.example.timesheet.enums.EntryType;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//import java.sql.Date;
//
//@Embeddable
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class DailyTimeSheetId implements Serializable {
//
//    @Column(name = "employeeCode", nullable = false)
//    private String employeeCode;
//
//    @Column(name = "timesheet_year", nullable = false)
//    private Integer timesheetYear;
//
//    @Column(name = "timesheet_month", nullable = false)
//    private Integer timesheetMonth;
//
//    @Column(name = "workDate", nullable = false)
//    private Date workDate;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "entryType", nullable = true)
//    private EntryType entryType;
//
//
//}
//
