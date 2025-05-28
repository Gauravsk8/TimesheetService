package com.example.timesheet.models;

import com.example.timesheet.common.audit.Audit;
import com.example.timesheet.keys.ProjectEmployeeId;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MapsId;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "project_code")
    private Project project;


    private String role_in_project;


    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "end_date")
    private Timestamp endDate;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

}
