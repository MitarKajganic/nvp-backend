package com.example.controllers;

import com.example.models.ScheduledVacuumOperation;
import com.example.models.entities.ErrorMessage;
import com.example.models.enums.Status;
import com.example.models.entities.User;
import com.example.models.entities.Vacuum;
import com.example.models.dto.VacuumDto;
import com.example.models.enums.VacuumAction;
import com.example.services.ErrorMessageService;
import com.example.services.UserService;
import com.example.services.VacuumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CrossOrigin
@RestController
@RequestMapping("/vacuums")
public class VacuumController {

    private final VacuumService vacuumService;

    private final UserService userService;

    private final TaskScheduler taskScheduler;

    private final ErrorMessageService errorMessageService;

    private final ConcurrentHashMap<Long, Boolean> pendingOperations = new ConcurrentHashMap<>();

    @Autowired
    public VacuumController(VacuumService vacuumService, UserService userService, TaskScheduler taskScheduler, ErrorMessageService errorMessageService) {
        this.vacuumService = vacuumService;
        this.userService = userService;
        this.taskScheduler = taskScheduler;
        this.errorMessageService = errorMessageService;
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Vacuum> getAllVacuums() {
        return vacuumService.findAll();
    }

    @GetMapping(value = "/{vacuumId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getVacuumById(@PathVariable("vacuumId") Long vacuumId) {
        Optional<Vacuum> optionalVacuum = vacuumService.findById(vacuumId);
        if (optionalVacuum.isPresent()) {
            return ResponseEntity.ok(optionalVacuum.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public List<Vacuum> search(@RequestParam(required = false) String name,
                               @RequestParam(required = false) List<String> statuses,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateFrom,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateTo) {

        Long userId = userService.findByEmail(loadEmail()).getId();

        Stream<Vacuum> vacuumStream = vacuumService.findAllByAddedBy(userId).stream();

        if (name != null && !name.isEmpty()) {
            vacuumStream = vacuumStream.filter(vacuum -> vacuum.getName().contains(name));
        }

        if (statuses != null && !statuses.isEmpty()) {
            Set<Status> statusSet = statuses.stream().map(Status::valueOf).collect(Collectors.toSet());
            vacuumStream = vacuumStream.filter(vacuum -> statusSet.contains(vacuum.getStatus()));
        }

        if (dateFrom != null && dateTo != null) {
            LocalDateTime startDateTime = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime endDateTime = dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            vacuumStream = vacuumStream.filter(vacuum ->
                    !vacuum.getCreatedAt().isBefore(startDateTime) && !vacuum.getCreatedAt().isAfter(endDateTime));
        }

        return vacuumStream.collect(Collectors.toList());
    }


    private void filterAndAddResults(Set<Vacuum> vacuums, List<Vacuum> results, Long userId) {
        for (Vacuum vacuum : results) {
            if (vacuum.getAddedBy().equals(userId)) vacuums.add(vacuum);
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createVacuum( @RequestBody @Validated VacuumDto vacuumDto) {
        User user = userService.findByEmail(loadEmail());
        Vacuum vacuum = new Vacuum();
        vacuum.setName(vacuumDto.getName());
        vacuum.setAddedBy(user.getId());
        vacuum.setStatus(Status.STOPPED);
        vacuum.setCycle(0);
        vacuum.setActive(true);
        return ResponseEntity.ok(vacuumService.save(vacuum));
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleVacuumOperation(@RequestBody @Validated ScheduledVacuumOperation operation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
        try {
            LocalDateTime scheduledDateTime = LocalDateTime.parse(operation.getScheduledDateTime(), formatter);
            LocalDateTime now = LocalDateTime.now();

            if (scheduledDateTime.isAfter(now)) {
                String cronExpression = generateCronExpression(scheduledDateTime);
                scheduleTask(operation, cronExpression);
                return ResponseEntity.ok("Operation scheduled successfully.");
            } else {
                return ResponseEntity.badRequest().body("Scheduled date has passed or is the same as the current date.");
            }
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to parse date. Ensure it's in MM/dd/yyyy HH:mm format.");
        }
    }


    private void scheduleTask(ScheduledVacuumOperation operation, String cronExpression) {
        taskScheduler.schedule(() -> performOperation(operation), new CronTrigger(cronExpression));
    }

    private void performOperation(ScheduledVacuumOperation operation) {
        updateVacuumStatus(operation.getVacuumId(), operation.getAction());
    }

    private String generateCronExpression(LocalDateTime executionDateTime) {
        return String.format("0 %d %d %d %d ?",
                executionDateTime.getMinute(),
                executionDateTime.getHour(),
                executionDateTime.getDayOfMonth(),
                executionDateTime.getMonthValue());
    }


    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateVacuum(@RequestBody @Validated Vacuum vacuum) {
        Optional<Vacuum> optionalUser = vacuumService.findById(vacuum.getId());

        if (optionalUser.isPresent()) {
            return ResponseEntity.ok(vacuumService.save(vacuum));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{action}/{id}")
    public ResponseEntity<?> updateVacuumStatus(@PathVariable Long id, @PathVariable VacuumAction action) {
        Optional<Vacuum> vacuumOptional = vacuumService.findById(id);

        if (!vacuumOptional.isPresent()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setVacuumId(id);
            errorMessage.setMessage("Vacuum not found");
            errorMessage.setAction(action);
            errorMessageService.save(errorMessage);

            return ResponseEntity.notFound().build();
        }

        Vacuum vacuum = vacuumOptional.get();
        if (!vacuum.getStatus().equals(action.getRequiredStatus())) {
            String error = "Access Denied: Can't " + action.name().toLowerCase() + " a vacuum that is not " + action.getRequiredStatus().name().toLowerCase();

            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setVacuumId(id);
            errorMessage.setMessage(error);
            errorMessage.setAction(action);
            errorMessageService.save(errorMessage);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        if (!vacuum.isActive()) {
            String error = "Access Denied: Vacuum is disabled";

            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setVacuumId(id);
            errorMessage.setMessage(error);
            errorMessage.setAction(action);
            errorMessageService.save(errorMessage);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        if (pendingOperations.putIfAbsent(id, true) != null) {
            String error = "Access Denied: Vacuum operation already in progress";

            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setVacuumId(id);
            errorMessage.setMessage(error);
            errorMessage.setAction(action);
            errorMessageService.save(errorMessage);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        if (action.getNewStatus().equals(Status.RUNNING)) vacuum.setCycle(vacuum.getCycle() + 1);

        Thread thread = new Thread(() -> updateStatus(vacuum, action));
        thread.start();

        return ResponseEntity.ok().build();
    }

    private void updateStatus(Vacuum vacuum, VacuumAction action) {
        try {
            int totalSleepTime = 15000 + (int)(Math.random() * 5000);
            Thread.sleep(totalSleepTime);
            vacuum.setStatus(action.getNewStatus());
            vacuumService.save(vacuum);

            if (action.getNewStatus().equals(Status.STOPPED) && vacuum.getCycle() == 3) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.DISCHARGING);
                vacuumService.save(vacuum);

                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                vacuum.setCycle(0);
                vacuumService.save(vacuum);
            } else if (action.getNewStatus().equals(Status.DISCHARGING)) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                vacuum.setCycle(0);
                vacuumService.save(vacuum);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        } finally {
            pendingOperations.remove(vacuum.getId());
        }
    }

    @DeleteMapping(value = "/{vacuumId}")
    public ResponseEntity<?> deleteVacuum(@PathVariable("vacuumId") Long vacuumId) {
        Optional<Vacuum> optionalVacuum = vacuumService.findById(vacuumId);
        if (!optionalVacuum.isPresent()) return ResponseEntity.notFound().build();
        Vacuum vacuum = optionalVacuum.get();
        if (!vacuum.getStatus().equals(Status.STOPPED)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Can't remove a vacuum that is not stopped");
        vacuum.setActive(false);
        vacuumService.save(vacuum);
        return ResponseEntity.ok().build();
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
