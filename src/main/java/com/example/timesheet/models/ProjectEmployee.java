package com.example.timesheet.models;

import com.example.timesheet.common.audit.Audit;
import com.example.timesheet.keys.ProjectEmployeeId;
import jakarta.persistence.*;
import lombok.*;


import java.sql.Timestamp;

@Entity
@Table(name = "project_employee")
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ProjectEmployee extends Audit {

    @Builder.Default
    @EmbeddedId
    private ProjectEmployeeId id = new ProjectEmployeeId();

    @ManyToOne
    @MapsId("projectCode")
    @JoinColumn(name = "project_code")  // Uses snake_case in DB
    private Project project;


    private String role_in_project;


    @Column(name = "start_date")
    private Timestamp startDate;  // When assignment begins

    @Column(name = "end_date")
    private Timestamp endDate;    // When assignment ends (nullable)

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

}
