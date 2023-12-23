package com.example.services;

import com.example.models.Vacuum;
import com.example.models.Vacuum;
import com.example.repositories.VacuumRepository;
import com.example.repositories.VacuumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VacuumService implements MyService<Vacuum, Long>{

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
}
