package com.example.models.entities;

import com.example.models.enums.VacuumAction;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "error_messages")
public class ErrorMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime timestamp;

    private Long vacuumId;
    @Enumerated(EnumType.STRING)
    private VacuumAction action;
    private String message;

    @PrePersist
    public void prePersist() {
        timestamp = LocalDateTime.now();
    }
}
