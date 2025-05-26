package com.example.timesheet.models;


import com.example.timesheet.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Table
@Entity
@Getter
@Setter

@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Project extends Audit {

    @Id
    @GeneratedValue(generator = "pr-code-gen")
    @GenericGenerator(
            name = "pr-code-gen",
            strategy = "com.example.timesheet.generator.ProjectCodeGenerator"
    )
    @Column(nullable = false, updatable = false, unique = true)
    private String projectCode;

    @Column
    private String title;

    @Column
    private String description;

    @Column
    private Timestamp startDate;

    @Column
    private Timestamp endDate;

    @ManyToOne
    @JoinColumn(name="clients_id")
    private Clients clients;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costCenterCode", nullable = false)
    private CostCenter costCenter;


    @Column(name = "ProjectManagerCode")
    private String projectManagerCode;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectEmployee> projectEmployees = new HashSet<>();

    @Column
    private String allocated_hours;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

}