package com.example.timesheet.dto.request;


import com.example.timesheet.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientDto {

    @Size(max = 20, message = "Name can't exceed 20 characters")
    private String name;

    @Size(max = 20, message = "contact Person can't exceed 20 characters")
    private String contactPerson;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Size(max = 50, message = "Address can't exceed 50 characters")
    private String address;
}
