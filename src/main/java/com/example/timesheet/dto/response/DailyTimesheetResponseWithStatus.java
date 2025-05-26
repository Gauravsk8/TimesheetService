package com.example.timesheet.dto.response;


import com.example.timesheet.enums.TimeSheetStatus;
import lombok.Data;

import java.util.List;

@Data
public class DailyTimesheetResponseWithStatus {
    List<DailyTimeSheetResponseDto> dailyTimeSheetResponseDtos;
    TimeSheetStatus status;
    String managerComment;
}
