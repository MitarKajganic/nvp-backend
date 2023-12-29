package com.example.services;

import com.example.models.entities.ErrorMessage;
import com.example.models.enums.Status;
import com.example.models.entities.Vacuum;
import com.example.models.enums.VacuumAction;
import com.example.repositories.MyService;
import com.example.repositories.VacuumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class VacuumService implements MyService<Vacuum, Long> {

    private final VacuumRepository vacuumRepository;
    private final ErrorMessageService errorMessageService;
    private final UserService userService;
    private final ConcurrentHashMap<Long, AtomicBoolean> pendingOperations = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    public VacuumService(VacuumRepository vacuumRepository, ErrorMessageService errorMessageService, UserService userService) {
        this.vacuumRepository = vacuumRepository;
        this.errorMessageService = errorMessageService;
        this.userService = userService;
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown(); // Properly shutdown the executor service when the application is stopping
    }

    @Override
    public <S extends Vacuum> S save(S vacuum) {
        return vacuumRepository.save(vacuum);
    }

    @Override
    public Optional<Vacuum> findById(Long vacuumId) {
        return vacuumRepository.findById(vacuumId);
    }

    @Override
    public List<Vacuum> findAll() {
        return (List<Vacuum>) vacuumRepository.findAll();
    }

    @Override
    public void deleteById(Long vacuumId) {
        vacuumRepository.deleteById(vacuumId);
    }

    public List<Vacuum> findAllByNameContaining(String name) {
        return vacuumRepository.findAllByNameContaining(name);
    }

    public List<Vacuum> findAllByAddedBy(Long userId) {
        return vacuumRepository.findAllByAddedBy(userId);
    }

    public List<Vacuum> findAllByStatus(Status status) {
        return vacuumRepository.findAllByStatus(status);
    }

    public List<Vacuum> findAllByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return vacuumRepository.findAllByCreatedAtBetween(startDateTime, endDateTime);
    }

    @Transactional
    public ResponseEntity<?> updateVacuumStatus(Long id, VacuumAction action, String userEmail) {
        try {
            Optional<Vacuum> vacuumOptional = findById(id);

            if (!vacuumOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Vacuum vacuum = vacuumOptional.get();
            Long userId = userService.findByEmail(userEmail).getId();

            if (!vacuum.getAddedBy().equals(userId))
                return buildErrorResponse(id, "Access Denied: Vacuum doesn't exist or doesn't belong to user", action);
            if (!vacuum.getStatus().equals(action.getRequiredStatus()))
                return buildErrorResponse(id, "Access Denied: Can't " + action.name().toLowerCase() + " a vacuum that is not " + action.getRequiredStatus().name().toLowerCase(), action);
            if (!vacuum.isActive())
                return buildErrorResponse(id, "Access Denied: Vacuum is disabled", action);
            if (!startOperation(id))
                return buildErrorResponse(id, "Access Denied: Vacuum operation already in progress", action);
            if (action.getNewStatus().equals(Status.RUNNING))
                vacuum.setCycle(vacuum.getCycle() + 1);

            executorService.submit(() -> updateStatus(vacuum, action, id));
            return ResponseEntity.ok().build();

        } catch (OptimisticLockException ole) {
            return buildErrorResponse(id, "Failed to update: The vacuum status was updated by another transaction", action);
        } finally {
            endOperation(id);
        }
    }

    private ResponseEntity<?> buildErrorResponse(Long id, String message, VacuumAction action) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setVacuumId(id);
        errorMessage.setMessage(message);
        errorMessage.setAction(action);
        errorMessageService.save(errorMessage);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }


    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void updateStatus(Vacuum vacuum, VacuumAction action, Long id) {
        try {
            int totalSleepTime = 15000 + (int) (Math.random() * 5000);
            Thread.sleep(totalSleepTime);
            vacuum.setStatus(action.getNewStatus());
            save(vacuum);
            vacuum = vacuumRepository.findById(vacuum.getId()).get();


            if (action.getNewStatus().equals(Status.STOPPED) && vacuum.getCycle() == 3) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.DISCHARGING);
                vacuum.setCycle(0);
                save(vacuum);
                vacuum = vacuumRepository.findById(vacuum.getId()).get();

                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                save(vacuum);
            } else if (action.getNewStatus().equals(Status.DISCHARGING)) {
                Thread.sleep(totalSleepTime);
                vacuum.setStatus(Status.STOPPED);
                vacuum.setCycle(0);
                save(vacuum);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        } catch (OptimisticLockException ole) {
            throw new RuntimeException("Optimistic locking failed during updateStatus", ole);
        } finally {
            AtomicBoolean operationStatus = pendingOperations.get(id);
            if (operationStatus != null) {
                operationStatus.set(false);
            }
        }
    }

    private boolean startOperation(Long id) {
        AtomicBoolean alreadyRunning = pendingOperations.computeIfAbsent(id, k -> new AtomicBoolean(false));
        return alreadyRunning.compareAndSet(false, true);
    }

    private void endOperation(Long id) {
        AtomicBoolean operationStatus = pendingOperations.get(id);
        if (operationStatus != null) {
            operationStatus.set(false);
            if (!operationStatus.get()) {
                pendingOperations.remove(id);
            }
        }
    }
}
