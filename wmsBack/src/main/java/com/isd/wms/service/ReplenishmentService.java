package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentNotFoundException;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.imports.ImportService;
import com.isd.wms.service.imports.dto.ReplenishmentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for managing replenishment tasks.
 * <p>
 * Replenishments ensure that picking locations maintain sufficient stock.
 * The service allows creation, update, cancellation, and assignment of
 * replenishment tasks. It also supports automatic replenishment based on
 * product thresholds and current stock levels.
 * </p>
 * <p>
 * When a replenishment is assigned, a corresponding {@link Task} is created
 * and allocated to an operator. Cancelling releases any reserved stock and
 * disassociates any transport unit.
 * </p>
 *
 * @see Replenishment
 * @see Task
 * @see Allocation
 * @see Stock
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplenishmentService {

    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final AllocationRepository allocationRepository;
    private final TransportUnitRepository transportUnitRepository;
    private final ReplenishmentMapper replenishmentMapper;
    private final WorkflowService workflowService;
    private final TaskService taskService;
    private final ImportService importService;

    private static final List<Status> ACTIVE_STATUSES = List.of(Status.CREATED, Status.ASSIGNED, Status.IN_PROGRESS);

    private void validateDestinationLocation(Product incomingProduct, Location destinationLocation) {
        stockRepository.findByLocationId(destinationLocation.getId()).ifPresent(stock -> {
            Product existingProduct = stock.getProduct().orElse(null);
            if (existingProduct != null && !existingProduct.getId().equals(incomingProduct.getId())) {
                if (stock.getQuantity() > 0 || stock.getReservedQuantity() > 0) {
                    throw new InvalidRequestException(
                        "Cannot route replenishment to " + destinationLocation.getBarcode() +
                            ". Location is already occupied by a different product: " + existingProduct.getName()
                    );
                }
            }
        });
    }

    /**
     * Creates a new replenishment request.
     *
     * @param request the creation request
     * @return the created replenishment response
     * @throws InvalidRequestException if the destination location is already occupied by a different product
     */
    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        log.info("Creating replenishment: productId={}, requestedQuantity={}, destinationLocationId={}",
            request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        validateDestinationLocation(product, destinationLocation);

        Replenishment replenishment = new Replenishment(product, request.requestedQuantity(), destinationLocation);
        replenishment.setStatus(Status.CREATED);
        replenishment = replenishmentRepository.save(replenishment);

        return replenishmentMapper.toResponse(replenishment);
    }

    @Transactional
    public ReplenishmentResponse updateReplenishment(Long id, ReplenishmentUpdateRequest request) {
        log.info("Updating replenishment: id={}, status={}", id, request.status());

        Replenishment replenishment = getReplenishment(id);

        if (replenishment.getStatus() == Status.IN_PROGRESS ||
            replenishment.getStatus() == Status.COMPLETED ||
            replenishment.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Cannot update replenishment that is currently in status: " +
                replenishment.getStatus().name());
        }

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        validateDestinationLocation(product, destinationLocation);

        boolean isProductChanged = !request.productId().equals(replenishment.getProduct().getId());
        boolean isQuantityChanged = !request.requestedQuantity().equals(replenishment.getRequestedQuantity());

        if (isProductChanged || isQuantityChanged) {
            replenishment.getTask().ifPresent(task -> {
                task.setRequestedQuantity(request.requestedQuantity());
                workflowService.updateTask(task, request.productId(), request.requestedQuantity());
            });
        }

        updateReplenishment(request, replenishment, product, destinationLocation);

        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
    }

    private static void updateReplenishment(
        ReplenishmentUpdateRequest request,
        Replenishment replenishment,
        Product product,
        Location destinationLocation) {
        replenishment.setProduct(product);
        replenishment.setRequestedQuantity(request.requestedQuantity());

        if (request.status() != null) {
            replenishment.setStatus(request.status());
        }

        replenishment.setDestinationLocation(destinationLocation);
    }

    /**
     * Automatically triggers a replenishment if stock at a location falls below
     * the product's minimum threshold and no active replenishment exists.
     *
     * @param product     the product to replenish
     * @param location    the picking location
     * @param locationQty the current available quantity at the location
     */
    @Transactional
    public void checkAndTriggerAutoReplenishment(Product product, Location location, int locationQty) {
        if (!Boolean.TRUE.equals(product.getAutoReplenish())) return;
        if (product.getReplenishQty() == null) return;

        if (locationQty <= product.getMinThreshold().orElseThrow()) {
            boolean hasActive = replenishmentRepository.existsByProductIdAndDestinationLocationIdAndStatusIn(
                product.getId(), location.getId(), ACTIVE_STATUSES
            );

            if (!hasActive) {
                log.info("Auto-triggering replenishment for product {} at location {} (Location Qty: {}, Threshold: {})",
                    product.getBarcode(), location.getBarcode(), locationQty, product.getMinThreshold());

                ReplenishmentCreateRequest req = new ReplenishmentCreateRequest(
                    product.getId(), product.getReplenishQty(), location.getId()
                );
                createReplenishment(req);
            }
        }
    }

    @Transactional
    public void deleteReplenishment(Long replenishmentId) {
        log.info("Deleting replenishment: id={}", replenishmentId);
        Replenishment replenishment = getReplenishment(replenishmentId);

        if (replenishment.getStatus() != Status.CREATED) {
            throw new InvalidRequestException("Physical deletion is only allowed for tasks in CREATED status.");
        }

        replenishment.getTask().ifPresent(task -> {
            List<Allocation> allocations = allocationRepository.findAllByTaskId(task.getId());
            for (Allocation allocation : allocations) {
                Stock stock = allocation.getStock();
                stock.setReservedQuantity(stock.getReservedQuantity() - allocation.getQuantity());
                stockRepository.save(stock);
            }
            allocationRepository.deleteAll(allocations);
        });

        replenishmentRepository.delete(replenishment);
    }

    /**
     * Cancels a replenishment. Releases reserved stock and disassociates any transport unit.
     *
     * @param replenishmentId the ID of the replenishment to cancel
     * @return the updated replenishment response (status CANCELED)
     * @throws InvalidRequestException if the replenishment is already COMPLETED or CANCELED
     */
    @Transactional
    public ReplenishmentResponse cancelReplenishment(Long replenishmentId) {
        log.info("Canceling replenishment: id={}", replenishmentId);
        Replenishment replenishment = getReplenishment(replenishmentId);

        if (replenishment.getStatus() == Status.COMPLETED || replenishment.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Cannot cancel a task that is already COMPLETED or CANCELED.");
        }

        transportUnitRepository.findByReplenishment(replenishment).ifPresent(tu -> {
            tu.setReplenishment(null);
            transportUnitRepository.save(tu);
            log.info("Successfully released Transport Unit {} from canceled replenishment {}",
                tu.getBarcode(), replenishmentId);
        });

        replenishment.getTask().ifPresent(task -> {
            List<Allocation> allocations = allocationRepository.findAllByTaskId(task.getId());
            for (Allocation allocation : allocations) {
                if (allocation.getStatus() != Status.COMPLETED) {
                    Stock stock = allocation.getStock();
                    stock.setReservedQuantity(stock.getReservedQuantity() - allocation.getQuantity());
                    stockRepository.save(stock);
                    allocation.setStatus(Status.CANCELED);
                }
            }
            allocationRepository.saveAll(allocations);
        });

        replenishment.setStatus(Status.CANCELED);
        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
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

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException(locationId));
    }

    /**
     * Assigns a replenishment to an operator by creating a task and linking it.
     *
     * @param replenishmentId the ID of the replenishment
     * @param operatorId      the ID of the operator
     */
    @Transactional
    public void assignReplenishment(Long replenishmentId, Long operatorId) {
        Replenishment replenishment = getReplenishment(replenishmentId);
        Task task = taskService.createTask(TaskType.REPLENISHMENT, replenishment.getRequestedQuantity(),
            replenishment.getProduct().getId());
        replenishment.setTask(task);
        replenishmentRepository.saveAndFlush(replenishment);
        taskService.assignTask(task.getId(), operatorId);
    }

    @Transactional
    public void importReplenishmentsFromFile(MultipartFile file) {
        List<ReplenishmentCreateRequest> replenishments = importService.importData(file, ReplenishmentInfo.class);
        replenishments.forEach(this::createReplenishment);
    }
}
