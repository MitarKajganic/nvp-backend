package com.example.controllers;

import com.example.models.entities.ErrorMessage;
import com.example.services.ErrorMessageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("/errors")
public class ErrorMessageController {

    private final ErrorMessageService errorMessageService;

    public ErrorMessageController(ErrorMessageService errorMessageService) {
        this.errorMessageService = errorMessageService;
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErrorMessage> getAllErrorMessages() {
        return errorMessageService.findAll();
    }

    @GetMapping(value = "/{errorMessageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getErrorMessageById(@PathVariable("errorMessageId") Long errorMessageId) {
        Optional<ErrorMessage> optionalErrorMessage = errorMessageService.findById(errorMessageId);
        if (optionalErrorMessage.isPresent()) {
            return ResponseEntity.ok(optionalErrorMessage.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createErrorMessage(@RequestBody @Validated ErrorMessage errorMessage) {
        return ResponseEntity.ok(errorMessageService.save(errorMessage));
    }
}
