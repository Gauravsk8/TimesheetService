package com.example.timesheet.dto.paginationdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortRequest {
    private String field;
    private String direction; // "asc" or "desc"

}
