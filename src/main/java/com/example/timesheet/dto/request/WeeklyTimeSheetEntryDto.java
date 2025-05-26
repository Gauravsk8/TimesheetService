package com.example.timesheet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimeSheetEntryDto {
    private String weekStartDate;
    private String weekEndDate;
    private Double hours;
    private String status;
}