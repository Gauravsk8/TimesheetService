package com.example.timesheet.models;

import com.example.timesheet.common.audit.Audit;
import com.example.timesheet.enums.Status;
import jakarta.persistence.*;
import lombok.*;

@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Clients extends Audit {

    @Id
    @Column(name = "client_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String contactPerson;

    private String contactEmail;

    private String address;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

}
