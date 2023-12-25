package com.example.models;

import com.example.controllers.VacuumAction;
import lombok.Data;

@Data
public class ScheduledVacuumOperation {

    private Long vacuumId;
    private VacuumAction action;
    private String scheduledDateTime;

    public ScheduledVacuumOperation(Long vacuumId, VacuumAction action, String scheduledDateTime) {
        this.vacuumId = vacuumId;
        this.action = action;
        this.scheduledDateTime = scheduledDateTime;
    }

}
