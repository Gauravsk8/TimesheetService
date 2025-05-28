package com.example.timesheet.repository;

import com.example.timesheet.models.Clients;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ClientsRepository extends JpaRepository<Clients, Long>, JpaSpecificationExecutor<Clients> {

    List<Clients> findByIsActiveTrue();


    Optional<Clients> findByIdAndIsActiveTrue(Long id);
}
