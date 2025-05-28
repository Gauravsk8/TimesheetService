package com.example.timesheet.dto.response.managerdashboard;

import com.example.timesheet.dto.request.WeeklyTimeSheetEntryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponseDto {
    private String employeeCode;
    private String fullName;
    private String email;
    private List<WeeklyTimeSheetEntryDto> weeklySummaries;
}
