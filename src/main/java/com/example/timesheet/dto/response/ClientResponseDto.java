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
public class ClientResponseDto {

    private Long id;             // optional - if you need it for update/view operations
    private String name;
    private String contactPerson;
    private String contactEmail;
    private String address;
    private boolean isActive;
}
