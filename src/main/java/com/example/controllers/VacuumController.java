package com.example.controllers;

import com.example.models.Status;
import com.example.models.User;
import com.example.models.Vacuum;
import com.example.models.dto.VacuumDto;
import com.example.services.UserService;
import com.example.services.VacuumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin
@RestController
@RequestMapping("/vacuums")
public class VacuumController {

    private final VacuumService vacuumService;

    private final UserService userService;

    private final ConcurrentHashMap<Long, Boolean> pendingOperations = new ConcurrentHashMap<>();

    @Autowired
    public VacuumController(VacuumService vacuumService, UserService userService) {
        this.vacuumService = vacuumService;
        this.userService = userService;
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

    @PostMapping(value = "/{email}",
                 consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createVacuum( @PathVariable("email") String email,
                                                @RequestBody @Validated VacuumDto vacuumDto) {
        User user = userService.findByEmail(email);
        Vacuum vacuum = new Vacuum();
        vacuum.setName(vacuumDto.getName());
        vacuum.setAddedBy(user.getId());
        vacuum.setStatus(Status.STOPPED);
        vacuum.setCycle(0);
        vacuum.setActive(true);
        return ResponseEntity.ok(vacuumService.save(vacuum));
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

    @GetMapping("/search")
    public List<Vacuum> search( @RequestParam String email,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) List<String> statuses,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateFrom,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateTo) {


        Long userId = userService.findByEmail(email).getId();
        Set<Vacuum> vacuums = new HashSet<>();
        List<Vacuum> results;

        if (name != null && !name.isEmpty()) {
            results = vacuumService.findAllByNameContaining(name);
            filterAndAddResults(vacuums, results, userId);
        }

        if (statuses != null && !statuses.isEmpty()) {
            for (String status : statuses) {
                results = vacuumService.findAllByStatus(Status.valueOf(status));
                filterAndAddResults(vacuums, results, userId);
            }
        }

        if (dateFrom != null && dateTo != null) {
            LocalDateTime startDateTime = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime endDateTime = dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            results = vacuumService.findAllByCreatedAtBetween(startDateTime, endDateTime);
            vacuums.addAll(results);
        }

        return new ArrayList<>(vacuums);
    }

    private void filterAndAddResults(Set<Vacuum> vacuums, List<Vacuum> results, Long userId) {
        for (Vacuum vacuum : results) {
            if (vacuum.getAddedBy().equals(userId)) vacuums.add(vacuum);
        }
    }

    @PutMapping("/{action}/{id}")
    public ResponseEntity<?> updateVacuumStatus(@PathVariable Long id, @PathVariable VacuumAction action) {
        try {
            Optional<Vacuum> vacuumOptional = vacuumService.findById(id);
            if (!vacuumOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Vacuum vacuum = vacuumOptional.get();
            if (!vacuum.getStatus().equals(action.getRequiredStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Can't " + action.name().toLowerCase() + " a vacuum that is not " + action.getRequiredStatus().name().toLowerCase());
            }

            if (pendingOperations.putIfAbsent(id, true) != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vacuum operation already in progress");
            }

            if (action.getNewStatus().equals(Status.RUNNING)) vacuum.setCycle(vacuum.getCycle() + 1);

            Thread thread = new Thread(() -> updateStatus(vacuum, action));
            thread.start();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void updateStatus(Vacuum vacuum, VacuumAction action) {
        try {
            int totalSleepTime = 15000 + (int)(Math.random() * 5000);
            Thread.sleep(totalSleepTime);
            vacuum.setStatus(action.getNewStatus());
            vacuumService.save(vacuum);

            if (action.getNewStatus().equals(Status.STOPPED) && vacuum.getCycle() % 3 == 0) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.DISCHARGING);
                vacuumService.save(vacuum);

                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                vacuumService.save(vacuum);
            } else if (action.getNewStatus().equals(Status.DISCHARGING)) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                vacuumService.save(vacuum);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        } finally {
            pendingOperations.remove(vacuum.getId());
        }
    }
}
