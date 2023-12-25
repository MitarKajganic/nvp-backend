package com.example.repositories;

import com.example.models.entities.ErrorMessage;
import org.springframework.data.repository.CrudRepository;

public interface ErrorMessageRepository extends CrudRepository<ErrorMessage, Long> {

}
