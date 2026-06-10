package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.*;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SecurityFacade securityFacade;

    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        log.info("Creating replenishment: productId={}, requestedQuantity={}, destinationLocationId={}",
                request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());
        User supervisor = getUser(securityFacade.getCurrentUsername());

        Task task = new Task();
        task.setSupervisor(supervisor);
        task.setTaskType(TaskType.REPLENISHMENT);
        task.setRequestedQuantity(request.requestedQuantity());
        task.setStatus(TaskStatus.CREATED);

        task = taskRepository.save(task);

        Replenishment replenishment = new Replenishment();
        replenishment.setProduct(product);
        replenishment.setTask(task);
        replenishment.setRequestedQuantity(request.requestedQuantity());
        replenishment.setStatus(ReplenishmentStatus.CREATED);
        replenishment.setDestinationLocation(destinationLocation);

        replenishment = replenishmentRepository.save(replenishment);

        workflowService.generateProcessesForTask(task, product.getId(), request.requestedQuantity());

        return replenishmentMapper.toResponse(replenishment);
    }

    @Transactional
    public ReplenishmentResponse updateReplenishment(Long id, ReplenishmentUpdateRequest request) {
        log.info("Updating replenishment: id={}, status={}", id, request.status());

        Replenishment replenishment = getReplenishment(id);
        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        if (!request.productId().equals(product.getId()) || !request.requestedQuantity().equals(replenishment.getRequestedQuantity())) {
            workflowService.updateTask(replenishment.getTask(), request.productId(), request.requestedQuantity());
        }

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
        List<Replenishment> tasks = replenishmentRepository.filter(
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

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }
}