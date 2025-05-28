package com.example.timesheet.dto.pagenationDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordUpdateRequestDto {

    @NotBlank(message = "New password must not be blank")
    private String newPassword;

}
