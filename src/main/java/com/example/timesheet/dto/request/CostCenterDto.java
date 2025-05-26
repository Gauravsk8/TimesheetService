package com.example.timesheet.dto.request;


import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CostCenterDto {
    private String costCenterCode;

    @Size(max = 20, message = "name can't exceed 20 characters")
    private String name;

    @Size(max = 50, message = "Description can't exceed 50 characters")
    private String description;

    @Size(max = 10, message = "cost center manager code can't exceed 20 characters")
    private String costCenterManagerCode;
}

