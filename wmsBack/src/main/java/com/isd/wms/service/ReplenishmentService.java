package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentNotFoundException;
import com.isd.wms.exception.TaskNotFoundException;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplenishmentService {
    private final ReplenishmentRepository replenishmentRepository;
    private final TaskRepository taskRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ReplenishmentMapper replenishmentMapper;
    private final WorkflowService workflowService;

    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        log.info("Creating replenishment: productId={}, requestedQuantity={}, destinationLocationId={}",
                request.productId(), request.requestedQuantity(), request.destinationLocationId());

        validateReplenishmentRequest(request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());
        User supervisor = getUser(getCurrentUsername());

        Task task = Task.builder()
                .supervisor(supervisor)
                .taskType(TaskType.REPLENISHMENT)
                .requestedQuantity(request.requestedQuantity())
                .status(TaskStatus.CREATED)
                .build();
        task = taskRepository.save(task);

        Replenishment replenishment = Replenishment.builder()
                .product(product)
                .task(task)
                .requestedQuantity(request.requestedQuantity())
                .status(ReplenishmentStatus.CREATED)
                .destinationLocation(destinationLocation)
                .build();
        replenishment = replenishmentRepository.save(replenishment);

        workflowService.generateProcessesForTask(task, product.getId(), request.requestedQuantity());

        return replenishmentMapper.toResponse(replenishment);
    }

    @Transactional
    public ReplenishmentResponse updateReplenishment(Long id, ReplenishmentUpdateRequest request) {
        log.info("Updating replenishment: id={}, status={}", id, request.status());
        validateReplenishmentRequest(request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Replenishment replenishment = getReplenishment(id);
        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        replenishment.setProduct(product);
        replenishment.setRequestedQuantity(request.requestedQuantity());
        replenishment.setStatus(request.status());
        replenishment.setDestinationLocation(destinationLocation);

        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
    }

    @Transactional
    public void deleteReplenishment(Long replenishmentId) {
        log.info("Deleting replenishment: id={}", replenishmentId);
        replenishmentRepository.delete(getReplenishment(replenishmentId));
    }

    public ReplenishmentResponse getReplenishmentById(Long replenishmentId) {
        return replenishmentMapper.toResponse(getReplenishment(replenishmentId));
    }

    public List<ReplenishmentResponse> getAllReplenishments() {
        return replenishmentRepository.findAll().stream()
                .map(replenishmentMapper::toResponse)
                .toList();
    }

    public List<ReplenishmentResponse> searchReplenishments(ReplenishmentSearchRequest request) {
        List<Replenishment> tasks;

        tasks = replenishmentRepository.filter(
                request.taskId(),
                request.productId(),
                request.requestedQuantity(),
                request.status(),
                request.destinationLocationId()
        );

        return tasks.stream().map(replenishmentMapper::toResponse).toList();
    }

    private Replenishment getReplenishment(Long replenishmentId) {
        return replenishmentRepository.findById(replenishmentId)
                .orElseThrow(() -> new ReplenishmentNotFoundException(replenishmentId));
    }
    
    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ProductNotFoundException(locationId));
    }

    private void validateReplenishmentRequest(
            @NonNull Long taskId,
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull ReplenishmentStatus status,
            @NonNull Long destinationLocationId) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment requested quantity cannot be nonpositive");
        }
        if (status != ReplenishmentStatus.CREATED) {
            throw new InvalidRequestException("Replenishment status must be CREATED");
        }
    }

    public void validateReplenishmentRequest(
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull Long destinationLocationId) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment requested quantity cannot be nonpositive");
        }
    }
}
