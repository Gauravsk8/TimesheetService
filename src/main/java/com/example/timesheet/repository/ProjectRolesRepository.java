package com.example.timesheet.repository;


import com.example.timesheet.models.ProjectRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRolesRepository extends JpaRepository<ProjectRoles, Long>, JpaSpecificationExecutor<ProjectRoles> {

    boolean existsByRoleName(String roleName);

}
