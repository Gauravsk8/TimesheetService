package com.example.timesheet.repository;

import com.example.timesheet.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, String>, JpaSpecificationExecutor<Project> {

    List<Project> findByProjectManagerCodeIgnoreCaseAndIsActiveTrue(String projectManagerCode);

    List<Project> findByCostCenter_CostCenterCodeIgnoreCaseAndIsActiveTrue(String costCenterCode);

    List<Project> findAllByCostCenter_CostCenterManagerCode(String managerCode);
    List<Project> findByIsActiveTrue();
    List<Project> findByProjectManagerCodeAndIsActiveTrue(String managerCode);


    Optional<Project> findByProjectCodeAndIsActiveTrue(String projectCode);
}

