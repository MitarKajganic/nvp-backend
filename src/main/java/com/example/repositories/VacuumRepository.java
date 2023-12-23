package com.example.repositories;

import com.example.models.User;
import com.example.models.Vacuum;
import org.springframework.data.repository.CrudRepository;

public interface VacuumRepository extends CrudRepository<Vacuum, Long> {

}
