package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.*;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;

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
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final ReplenishmentMapper replenishmentMapper;
    private final WorkflowService workflowService;
    private final TaskService taskService;

    private static final List<ReplenishmentStatus> TERMINAL_STATUSES = List.of(
        ReplenishmentStatus.COMPLETED);
    private static final List<Status> TERMINAL_STATUSES = List.of(
            Status.COMPLETED);

    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        log.info("Creating replenishment: productId={}, requestedQuantity={}, destinationLocationId={}",
            request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        validateDestinationStockForReplenishment(product, destinationLocation);
        validateNoActiveReplenishment(product.getId(), destinationLocation.getId(), null);

        Task task = taskService.createTask(TaskType.REPLENISHMENT, request.requestedQuantity(), request.productId());

        Replenishment replenishment = new Replenishment(task, product, request.requestedQuantity(), destinationLocation);
        replenishment = replenishmentRepository.save(replenishment);

        return replenishmentMapper.toResponse(replenishment);
    }

    @Transactional
    public ReplenishmentResponse updateReplenishment(Long id, ReplenishmentUpdateRequest request) {
        log.info("Updating replenishment: id={}, status={}", id, request.status());

        Replenishment replenishment = getReplenishment(id);
        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        boolean isProductChanged = !request.productId().equals(replenishment.getProduct().getId());
        boolean isLocationChanged = !request.destinationLocationId().equals(replenishment.getDestinationLocation().getId());
        boolean isQuantityChanged = !request.requestedQuantity().equals(replenishment.getRequestedQuantity());

        if (isProductChanged || isLocationChanged) {
            validateDestinationStockForReplenishment(product, destinationLocation);
            validateNoActiveReplenishment(product.getId(), destinationLocation.getId(), id);
        }

        if (isProductChanged || isQuantityChanged) {
            replenishment.getTask().setRequestedQuantity(request.requestedQuantity());
            workflowService.updateTask(replenishment.getTask(), request.productId(), request.requestedQuantity());
        }

        replenishment.setProduct(product);
        replenishment.setRequestedQuantity(request.requestedQuantity());
        replenishment.setStatus(request.status());
        replenishment.setDestinationLocation(destinationLocation);

        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
    }

    @Transactional
    public void checkAndTriggerAutoReplenishment(Product product, Location location, int locationQty) {
        if (!Boolean.TRUE.equals(product.getAutoReplenish())) return;
        if (product.getMinThreshold() == null || product.getReplenishQty() == null) return;

        if (locationQty <= product.getMinThreshold()) {
            boolean hasActive = replenishmentRepository.existsByProductIdAndDestinationLocationIdAndStatusNotIn(
                product.getId(), location.getId(), TERMINAL_STATUSES
            );

            if (!hasActive) {
                log.info("Auto-triggering replenishment for product {} at location {} (Location Qty: {}, Threshold: {})",
                    product.getSku(), location.getBarcode(), locationQty, product.getMinThreshold());

                ReplenishmentCreateRequest req = new ReplenishmentCreateRequest(
                    product.getId(), product.getReplenishQty(), location.getId()
                );
                createReplenishment(req);
            }
        }
    }

    private void validateNoActiveReplenishment(Long productId, Long locationId, Long excludeReplenishmentId) {
        boolean hasDuplicate;

        if (excludeReplenishmentId == null) {
            hasDuplicate = replenishmentRepository.existsByProductIdAndDestinationLocationIdAndStatusNotIn(
                productId, locationId, TERMINAL_STATUSES
            );
        } else {
            hasDuplicate = replenishmentRepository.existsByProductIdAndDestinationLocationIdAndStatusNotInAndIdNot(
                productId, locationId, TERMINAL_STATUSES, excludeReplenishmentId
            );
        }

        if (hasDuplicate) {
            log.warn("Replenishment rejected: An active replenishment task already exists for productId={} and locationId={}",
                productId, locationId);
            throw new InvalidRequestException("An active replenishment task for this product and destination location already exists.");
        }
    }

    private void validateDestinationStockForReplenishment(Product product, Location location) {
        stockRepository.findByProductAndLocation(product, location)
            .ifPresent(stock -> {
                int allowedThreshold = Boolean.TRUE.equals(product.getAutoReplenish()) && product.getMinThreshold() != null
                    ? product.getMinThreshold() : 0;

                if (stock.getQuantity() > allowedThreshold) {
                    log.warn("Replenishment rejected: location {} currently has {} pcs of product ID {} (Threshold is {})",
                        location.getBarcode(), stock.getQuantity(), product.getId(), allowedThreshold);
                    throw new InvalidRequestException(
                        String.format("Replenishment is allowed only when current stock is below threshold (%d). Location %s currently has %d pcs.",
                            allowedThreshold, location.getBarcode(), stock.getQuantity())
                    );
                }
            });
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

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException(locationId));
    }
}
