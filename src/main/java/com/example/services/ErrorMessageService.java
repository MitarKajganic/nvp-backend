package com.example.services;

import com.example.models.entities.ErrorMessage;
import com.example.repositories.ErrorMessageRepository;
import com.example.repositories.MyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ErrorMessageService implements MyService<ErrorMessage, Long> {

    private final ErrorMessageRepository errorMessageRepository;

    public ErrorMessageService(ErrorMessageRepository errorMessageRepository) {
        this.errorMessageRepository = errorMessageRepository;
    }

    @Override
    public <S extends ErrorMessage> S save(S errorMessage) {
        return errorMessageRepository.save(errorMessage);
    }

    @Override
    public Optional<ErrorMessage> findById(Long errorMessageId) {
        return errorMessageRepository.findById(errorMessageId);
    }

    @Override
    public List<ErrorMessage> findAll() {
        return (List<ErrorMessage>) errorMessageRepository.findAll();
    }

    @Override
    public void deleteById(Long errorMessageId) {
        errorMessageRepository.deleteById(errorMessageId);
    }
}
