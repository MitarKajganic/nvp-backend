package com.example.repositories;

import com.example.models.enums.Status;
import com.example.models.entities.Vacuum;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VacuumRepository extends CrudRepository<Vacuum, Long> {

    List<Vacuum> findAllByAddedBy(Long userId);

    List<Vacuum> findAllByNameContaining(String name);

    List<Vacuum> findAllByStatus(Status status);

    List<Vacuum> findAllByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

}
