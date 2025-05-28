package com.example.timesheet.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEmployeeId implements Serializable {

    @Column(name = "projectCode", nullable = false)
    private String projectCode;

    @Column(name = "employeeCode", nullable = false)
    private String employeeCode;
}

