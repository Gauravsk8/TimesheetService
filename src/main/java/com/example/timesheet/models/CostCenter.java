package com.example.timesheet.models;

import com.example.timesheet.common.audit.Audit;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "cost_centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenter extends Audit {

    @Id
    @GeneratedValue(generator = "cc-code-gen")
    @GenericGenerator(
            name = "cc-code-gen",
            strategy = "com.example.timesheet.generator.CostCenterCodeGenerator"
    )
    @Column(nullable = false, updatable = false, unique = true)
    private String costCenterCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "costCenterManagerCode")
    private String costCenterManagerCode;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;
}

