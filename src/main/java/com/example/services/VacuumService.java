package com.example.services;

import com.example.models.enums.Status;
import com.example.models.entities.Vacuum;
import com.example.repositories.MyService;
import com.example.repositories.VacuumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VacuumService implements MyService<Vacuum, Long> {

    private final VacuumRepository vacuumRepository;

    @Autowired
    public VacuumService(VacuumRepository vacuumRepository) {
        this.vacuumRepository = vacuumRepository;
    }

    @Override
    public <S extends Vacuum> S save(S vacuum) {
        return vacuumRepository.save(vacuum);
    }

    @Override
    public Optional<Vacuum> findById(Long vacuumId) {
        return vacuumRepository.findById(vacuumId);
    }

    @Override
    public List<Vacuum> findAll() {
        return (List<Vacuum>) vacuumRepository.findAll();
    }

    @Override
    public void deleteById(Long vacuumId) {
        vacuumRepository.deleteById(vacuumId);
    }

    public List<Vacuum> findAllByNameContaining(String name) {
        return vacuumRepository.findAllByNameContaining(name);
    }

    public List<Vacuum> findAllByAddedBy(Long userId) {
        return vacuumRepository.findAllByAddedBy(userId);
    }

    public List<Vacuum> findAllByStatus(Status status) {
        return vacuumRepository.findAllByStatus(status);
    }

    public List<Vacuum> findAllByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return vacuumRepository.findAllByCreatedAtBetween(startDateTime, endDateTime);
    }
}
