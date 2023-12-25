package com.example.models.enums;

import lombok.Getter;


@Getter
public enum VacuumAction {
    START(Status.STOPPED, Status.RUNNING),
    STOP(Status.RUNNING, Status.STOPPED),
    DISCHARGE(Status.STOPPED, Status.DISCHARGING);

    private final Status requiredStatus;
    private final Status newStatus;

    VacuumAction(Status requiredStatus, Status newStatus) {
        this.requiredStatus = requiredStatus;
        this.newStatus = newStatus;
    }
}
