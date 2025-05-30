package com.example.timesheet.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientDto {

    @Size(max = 20, message = "Name can't exceed 20 characters")
    @NotBlank(message = "name required")
    private String name;

    @Size(max = 20, message = "contact Person can't exceed 20 characters")
    @NotBlank(message = "contact person required")
    private String contactPerson;

    @Email(message = "Invalid email format")
    @NotBlank(message = "email required")
    private String contactEmail;

    @Size(max = 50, message = "Address can't exceed 50 characters")
    private String address;
}
