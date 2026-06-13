package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
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

    private static final List<Status> TERMINAL_STATUSES = List.of(
            Status.COMPLETED);

    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        log.info("Creating replenishment: productId={}, requestedQuantity={}, destinationLocationId={}",
                request.productId(), request.requestedQuantity(), request.destinationLocationId());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        validateDestinationStockIsZero(product, destinationLocation);
        validateNoActiveReplenishment(product.getId(), destinationLocation.getId(), null);

        log.debug("Triggering automatic background task creation of type REPLENISHMENT for Product '{}'", product.getName());
        Task task = taskService.createTask(TaskType.REPLENISHMENT, request.requestedQuantity(), request.productId());
        log.info("System Task ID {} generated successfully for this replenishment run", task.getId());

        Replenishment replenishment = new Replenishment(task, product, request.requestedQuantity(), destinationLocation);
        Replenishment savedReplenishment = replenishmentRepository.save(replenishment);

        log.info("Replenishment process successfully created. System ID: {}, Task ID: {}, Destination Location: '{}', Qty: {}",
            savedReplenishment.getId(), task.getId(), destinationLocation.getName(), request.requestedQuantity());

        return replenishmentMapper.toResponse(savedReplenishment);
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
            log.info("Replenishment ID {} structure modification detected (Product or Destination Location changed). Re-running system rules.", id);
            validateDestinationStockIsZero(product, destinationLocation);
            validateNoActiveReplenishment(product.getId(), destinationLocation.getId(), id);
        }

        if (isProductChanged || isQuantityChanged) {
            log.debug("Propagating replenishment quantity/product changes to down-stream Task ID {}", replenishment.getTask().getId());
            replenishment.getTask().setRequestedQuantity(request.requestedQuantity());
            workflowService.updateTask(replenishment.getTask(), request.productId(), request.requestedQuantity());
        }

        Status oldStatus = replenishment.getStatus();
        replenishment.setProduct(product);
        replenishment.setRequestedQuantity(request.requestedQuantity());
        replenishment.setStatus(request.status());
        replenishment.setDestinationLocation(destinationLocation);

        Replenishment updatedReplenishment = replenishmentRepository.save(replenishment);
        log.info("Replenishment ID {} successfully updated. Status change: {} -> {}", id, oldStatus, updatedReplenishment.getStatus());

        return replenishmentMapper.toResponse(updatedReplenishment);
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

    private void validateDestinationStockIsZero(Product product, Location location) {
        stockRepository.findByProductAndLocation(product, location)
                .ifPresent(stock -> {
                    if (stock.getQuantity() > 0) {
                        log.warn("Replenishment rejected: location {} already contains {} pcs of product ID {}",
                                location.getBarcode(), stock.getQuantity(), product.getId());
                        throw new InvalidRequestException(
                                String.format("Replenishment is allowed only when current stock is zero. Location %s currently has %d pcs.",
                                        location.getBarcode(), stock.getQuantity())
                        );
                    }
                });
    }

    @Transactional
    public void deleteReplenishment(Long replenishmentId) {
        log.warn("Deleting request for replenishment: id={}", replenishmentId);
        replenishmentRepository.delete(getReplenishment(replenishmentId));
        log.warn("Replenishment ID: {} has been permanently removed from the system database", replenishmentId);
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
            .orElseThrow(() -> {
                log.warn("Replenishment logic failure: Record with ID {} not found", replenishmentId);
                return new ReplenishmentNotFoundException(replenishmentId);
            });
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> {
                log.warn("Replenishment integrity failure: Referenced Product ID {} does not exist", productId);
                return new ProductNotFoundException(productId);
            });
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> {
                log.warn("Replenishment integrity failure: Referenced Destination Location ID {} does not exist", locationId);
                return new LocationNotFoundException(locationId);
            });
    }
}
