package com.example.timesheet.Repository;


import com.example.timesheet.keys.ProjectEmployeeId;
import com.example.timesheet.models.Project;
import com.example.timesheet.models.ProjectEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProjectEmployeeRepository extends JpaRepository<ProjectEmployee, ProjectEmployeeId>, JpaSpecificationExecutor<ProjectEmployee> {

    List<ProjectEmployee> findByIdEmployeeCodeIgnoreCaseAndIsActiveTrue(String employeeCode);

    List<ProjectEmployee> findByIdProjectCode(String projectCode);

    @Query("""
    SELECT p.projectCode, COUNT(pe.id)
    FROM ProjectEmployee pe
    JOIN pe.project p
    WHERE p.projectCode IN :projectCodes
    GROUP BY p.projectCode
""")
    List<Object[]> countEmployeesPerProject(@Param("projectCodes") Set<String> projectCodes);

    Long countByProject(Project project);

    List<ProjectEmployee> findByProject_ProjectCodeIgnoreCaseAndIsActiveTrue(String projectCode);

    boolean existsByIdAndIsActiveTrue(ProjectEmployeeId id);

    Optional<ProjectEmployee> findByIdAndIsActiveTrue(ProjectEmployeeId id); // FIXED HERE

    List<ProjectEmployee> findByProject_ProjectCodeAndIsActiveTrue(String projectCode);
}

