package com.example.controllers;

import com.example.models.entities.ErrorMessage;
import com.example.models.entities.Vacuum;
import com.example.services.ErrorMessageService;
import com.example.services.UserService;
import com.example.services.VacuumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/errors")
public class ErrorMessageController {

    private final ErrorMessageService errorMessageService;

    private final VacuumService vacuumService;

    private final UserService userService;

    @Autowired
    public ErrorMessageController(ErrorMessageService errorMessageService, VacuumService vacuumService, UserService userService) {
        this.errorMessageService = errorMessageService;
        this.vacuumService = vacuumService;
        this.userService = userService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErrorMessage> getAllErrorMessages() {
        Long userId = userService.findByEmail(loadEmail()).getId();

        List<Long> userVacuumIds = vacuumService.findAllByAddedBy(userId)
                .stream()
                .map(Vacuum::getId)
                .collect(Collectors.toList());


        return errorMessageService.findAll().stream()
                .filter(errorMessage -> userVacuumIds.contains(errorMessage.getVacuumId()))
                .collect(Collectors.toList());
    }

    private String loadEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            email = ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        if (email == null) {
            throw new IllegalStateException("User email not found in JWT");
        }

        return email;
    }
}
