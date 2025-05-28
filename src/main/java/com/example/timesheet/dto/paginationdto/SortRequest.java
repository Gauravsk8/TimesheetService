package com.example.timesheet.dto.pagenationDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortRequest {
    private String field;
    private String direction; // "asc" or "desc"

}
