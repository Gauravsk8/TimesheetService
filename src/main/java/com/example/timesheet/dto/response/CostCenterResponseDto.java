package com.example.timesheet.dto.response;

import com.example.timesheet.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterResponseDto {

    private String costCenterCode;
    private String name;
    private String description;
    private String costCenterManagerCode;
    private boolean isActive;

}
