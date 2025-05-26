package com.example.timesheet.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class TimesheetMatrixRowResponseDto {
    private String rowKey;
    private Map<String, Double> weeklyHours; // Key: weekLabel, Value: hours

    // Constructors
    public TimesheetMatrixRowResponseDto() {}

    public TimesheetMatrixRowResponseDto(String rowKey, Map<String, Double> weeklyHours) {
        this.rowKey = rowKey;
        this.weeklyHours = weeklyHours;
    }

    // Getters & Setters
    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public Map<String, Double> getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Map<String, Double> weeklyHours) {
        this.weeklyHours = weeklyHours;
    }
}
