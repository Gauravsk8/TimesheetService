package com.example.timesheet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ProjectTimesheetEntryRequest {
    private String projectCode;
    private Long totalHoursSpent;
}
