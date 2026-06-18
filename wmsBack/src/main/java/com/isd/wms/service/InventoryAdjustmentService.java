package com.isd.wms.service;

import com.isd.wms.dto.inventory.InventoryAdjustmentRequest;
import com.isd.wms.dto.inventory.InventoryAdjustmentResponse;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.dto.order.shortage.AffectedOrderLineResponse;
import com.isd.wms.dto.order.shortage.ShortageOrderResponse;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.InventoryHistory;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.InventoryAdjustmentReason;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.Zone;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.StockMapper;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.InventoryHistoryRepository;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAdjustmentService {

    private final StockRepository stockRepository;
    private final AllocationRepository allocationRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final UserRepository userRepository;
    private final StockMapper stockMapper;

    @Transactional(readOnly = true)
    public InventoryAdjustmentResponse previewAdjustment(Long stockId, InventoryAdjustmentRequest request) {
        return processAdjustment(stockId, request, true);
    }

    @Transactional
    public InventoryAdjustmentResponse adjustStock(Long stockId, InventoryAdjustmentRequest request) {
        return processAdjustment(stockId, request, false);
    }

    private InventoryAdjustmentResponse processAdjustment(Long stockId, InventoryAdjustmentRequest request, boolean preview) {
        if (request.newQuantity() == null || request.newQuantity() < 0) {
            throw new InvalidRequestException("New quantity must be greater than or equal to 0");
        }
        if (request.reason() == null) {
            throw new InvalidRequestException("Adjustment reason is required");
        }

        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new StockNotFoundException(stockId));
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Product product = stock.getProduct().orElseThrow(() -> new InvalidRequestException("Stock has no product"));
        Location stockLocation = stock.getLocation();
        int previousQuantity = Optional.ofNullable(stock.getQuantity()).orElse(0);
        int difference = request.newQuantity() - previousQuantity;
        boolean quantityReduced = difference < 0;

        log.info("Stock adjustment started: stockId={}, previousQuantity={}, newQuantity={}, difference={}, reason={}, userId={}, preview={}",
            stockId, previousQuantity, request.newQuantity(), difference, request.reason(), request.userId(), preview);

        AdjustmentSimulation simulation = simulateAdjustment(stock, request, product, user, previousQuantity);
        int reservedQuantity = simulation.taskImpacts().stream()
            .mapToInt(TaskImpact::preservedOnModified)
            .sum();

        Long historyId = null;
        if (!preview) {
            historyId = applySimulation(stock, request, user, simulation, previousQuantity, stockLocation, reservedQuantity);
        }

        InventoryAdjustmentResponse response = toResponse(stock, simulation, previousQuantity, request, preview, historyId, reservedQuantity);
        log.info("Stock adjustment completed: stockId={}, previousQuantity={}, newQuantity={}, preview={}, affectedOrders={}, affectedLines={}, reallocationSucceeded={}, partialShortage={}, orderCancelled={}",
            stockId,
            previousQuantity,
            request.newQuantity(),
            preview,
            response.affectedOrders().size(),
            response.affectedOrderLines().size(),
            response.reallocationSucceeded(),
            response.partialShortageCreated(),
            response.orderCancelled());

        if (quantityReduced) {
            log.info("Stock reduction triggered shortage recalculation: stockId={}, reason={}", stockId, request.reason());
        }

        return response;
    }

    private AdjustmentSimulation simulateAdjustment(Stock stock, InventoryAdjustmentRequest request, Product product, User user, int previousQuantity) {
        Long stockId = stock.getId();
        List<Allocation> modifiedStockAllocations = allocationRepository.findAllByStockId(stockId).stream()
            .filter(this::isActiveAllocation)
            .sorted(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId))
            .toList();

        Map<Long, Integer> preservedOnModifiedByTask = new LinkedHashMap<>();
        Map<Long, Integer> reducedOnModifiedByTask = new LinkedHashMap<>();
        Map<Long, List<AlternativeAllocationPlan>> alternativePlansByTask = new LinkedHashMap<>();
        int remainingCapacityOnModifiedStock = request.newQuantity();

        for (Allocation allocation : modifiedStockAllocations) {
            int originalQuantity = Optional.ofNullable(allocation.getQuantity()).orElse(0);
            int preservedQuantity = Math.min(originalQuantity, remainingCapacityOnModifiedStock);
            int reducedQuantity = originalQuantity - preservedQuantity;
            remainingCapacityOnModifiedStock -= preservedQuantity;

            Long taskId = allocation.getTask().getId();
            preservedOnModifiedByTask.merge(taskId, preservedQuantity, Integer::sum);
            reducedOnModifiedByTask.merge(taskId, reducedQuantity, Integer::sum);
        }

        List<AlternativeStockState> alternativeStockStates = stockRepository.findAvailableStocksByProductIdAndZone(product.getId(), Zone.PICKING).stream()
            .filter(candidate -> !Objects.equals(candidate.getId(), stockId))
            .sorted(Comparator.comparing((Stock candidate) -> Optional.ofNullable(candidate.getQuantity()).orElse(0) - Optional.ofNullable(candidate.getReservedQuantity()).orElse(0)).reversed()
                .thenComparing(Stock::getId))
            .map(candidate -> new AlternativeStockState(candidate, availableQuantity(candidate)))
            .toList();

        Map<Long, Integer> currentAvailableByStockId = new HashMap<>();
        for (AlternativeStockState alternativeStockState : alternativeStockStates) {
            currentAvailableByStockId.put(alternativeStockState.stock().getId(), alternativeStockState.availableQuantity());
        }

        List<Long> affectedTaskIds = modifiedStockAllocations.stream()
            .map(allocation -> allocation.getTask().getId())
            .distinct()
            .toList();

        List<TaskImpact> taskImpacts = affectedTaskIds.stream()
            .map(taskId -> buildTaskImpact(taskId, stock, request, preservedOnModifiedByTask, reducedOnModifiedByTask, currentAvailableByStockId, alternativePlansByTask))
            .sorted(Comparator.comparing(TaskImpact::orderCreatedAt).thenComparing(TaskImpact::orderId))
            .toList();

        Map<Long, Integer> preservedByTask = new HashMap<>(preservedOnModifiedByTask);
        Map<Long, Integer> reducedByTask = new HashMap<>(reducedOnModifiedByTask);
        Map<Long, List<AlternativeAllocationPlan>> altPlansByTask = new LinkedHashMap<>();
        for (TaskImpact impact : taskImpacts) {
            preservedByTask.put(impact.taskId(), impact.preservedOnModified());
            reducedByTask.put(impact.taskId(), impact.reducedFromModified());
            altPlansByTask.put(impact.taskId(), impact.alternativePlans());
        }

        return new AdjustmentSimulation(
            previousQuantity,
            request.newQuantity(),
            request.reason(),
            request.comment(),
            stock,
            user,
            taskImpacts,
            preservedByTask,
            reducedByTask,
            altPlansByTask
        );
    }

    private TaskImpact buildTaskImpact(
        Long taskId,
        Stock modifiedStock,
        InventoryAdjustmentRequest request,
        Map<Long, Integer> preservedOnModifiedByTask,
        Map<Long, Integer> reducedOnModifiedByTask,
        Map<Long, Integer> availableByStockId,
        Map<Long, List<AlternativeAllocationPlan>> alternativePlansByTask
    ) {
        OrderLine orderLine = orderLineRepository.findByTaskId(taskId)
            .orElseThrow(() -> new InvalidRequestException("Order line not found for task " + taskId));
        Order order = orderLine.getOrder();
        Task task = orderLine.getTask();

        List<Allocation> allAllocationsForTask = allocationRepository.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId).stream()
            .filter(this::isActiveAllocation)
            .toList();

        int currentAllocatedOther = allAllocationsForTask.stream()
            .filter(allocation -> !Objects.equals(allocation.getStock().getId(), modifiedStock.getId()))
            .mapToInt(allocation -> Optional.ofNullable(allocation.getQuantity()).orElse(0))
            .sum();

        int preservedOnModified = Optional.ofNullable(preservedOnModifiedByTask.get(taskId)).orElse(0);
        int reducedFromModified = Optional.ofNullable(reducedOnModifiedByTask.get(taskId)).orElse(0);
        int allocatedAfterReduction = currentAllocatedOther + preservedOnModified;
        int requestedQuantity = Optional.ofNullable(orderLine.getRequestedQuantity()).orElse(0);

        List<AlternativeAllocationPlan> plans = new ArrayList<>();
        int shortageNeeded = Math.max(0, requestedQuantity - allocatedAfterReduction);
        int allocatedFromAlternatives = 0;

        if (shortageNeeded > 0) {
            for (Map.Entry<Long, Integer> entry : availableByStockId.entrySet()) {
                if (shortageNeeded <= 0) {
                    break;
                }

                int available = Optional.ofNullable(entry.getValue()).orElse(0);
                if (available <= 0) {
                    continue;
                }

                int quantityToAllocate = Math.min(shortageNeeded, available);
                plans.add(new AlternativeAllocationPlan(entry.getKey(), quantityToAllocate));
                availableByStockId.put(entry.getKey(), available - quantityToAllocate);
                shortageNeeded -= quantityToAllocate;
                allocatedFromAlternatives += quantityToAllocate;
            }
        }

        int finalAllocated = allocatedAfterReduction + allocatedFromAlternatives;
        int shortageQuantity = Math.max(0, requestedQuantity - finalAllocated);
        boolean allFulfilled = shortageQuantity == 0;
        boolean revalidationRequired = task.getStatus() == TaskStatus.IN_PROGRESS && (reducedFromModified > 0 || allocatedFromAlternatives > 0 || shortageQuantity > 0);

        Status lineStatus = shortageQuantity > 0
            ? (finalAllocated > 0 ? Status.PARTIALLY_COMPLETED : Status.CANCELED)
            : orderLine.getStatus();

        Long reallocatedLocationId = plans.isEmpty() ? null : plans.getFirst().stockId();
        String reallocatedLocationBarcode = plans.isEmpty() ? null : getStockBarcode(plans.getFirst().stockId());

        return new TaskImpact(
            taskId,
            order.getId(),
            order.getLogicId(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            orderLine.getId(),
            task,
            orderLine,
            requestedQuantity,
            currentAllocatedOther,
            preservedOnModified,
            reducedFromModified,
            allocatedFromAlternatives,
            shortageQuantity,
            lineStatus,
            revalidationRequired,
            modifiedStock.getLocation().getId(),
            modifiedStock.getLocation().getBarcode(),
            reallocatedLocationId,
            reallocatedLocationBarcode,
            plans
        );
    }

    private Long applySimulation(
        Stock stock,
        InventoryAdjustmentRequest request,
        User user,
        AdjustmentSimulation simulation,
        int previousQuantity,
        Location stockLocation,
        int totalPreservedOnModifiedStock
    ) {
        int difference = request.newQuantity() - previousQuantity;

        stock.setQuantity(request.newQuantity());
        stock.setReservedQuantity(totalPreservedOnModifiedStock);
        stock.setManufactureDate(request.manufactureDate() == null ? stock.getManufactureDate() : request.manufactureDate());
        stock.setExpirationDate(request.expirationDate() == null ? stock.getExpirationDate() : request.expirationDate());
        stockRepository.save(stock);

        List<Allocation> allocationsToSave = new ArrayList<>();
        List<Stock> stocksToSave = new ArrayList<>();
        List<OrderLine> linesToSave = new ArrayList<>();
        List<Task> tasksToSave = new ArrayList<>();
        List<Long> impactedOrderIds = new ArrayList<>();

        List<Allocation> modifiedStockAllocations = allocationRepository.findAllByStockId(stock.getId()).stream()
            .filter(this::isActiveAllocation)
            .sorted(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId))
            .toList();

        int remainingCapacityOnModifiedStock = request.newQuantity();
        for (Allocation allocation : modifiedStockAllocations) {
            int originalQuantity = Optional.ofNullable(allocation.getQuantity()).orElse(0);
            int preservedQuantity = Math.min(originalQuantity, remainingCapacityOnModifiedStock);
            remainingCapacityOnModifiedStock -= preservedQuantity;

            if (preservedQuantity <= 0) {
                allocation.setStatus(Status.CANCELED);
                allocation.setQuantity(0);
            } else {
                allocation.setQuantity(preservedQuantity);
            }
            allocationsToSave.add(allocation);
        }

        for (TaskImpact impact : simulation.taskImpacts()) {
            impact.orderLine().setDeliveredQuantity(impact.finalAllocatedQuantity());
            impact.orderLine().setShortageQuantity(impact.shortageQuantity());
            if (impact.shortageQuantity() > 0) {
                impact.orderLine().setStatus(impact.lineStatus());
            }
            linesToSave.add(impact.orderLine());

            if (impact.revalidationRequired()) {
                impact.task().setStatus(TaskStatus.REQUIRES_REVALIDATION);
                tasksToSave.add(impact.task());
            }

            if (impact.shortageQuantity() > 0 && impact.lineStatus() == Status.PARTIALLY_COMPLETED) {
                log.warn("Partial completion created: orderId={}, orderLineId={}, deliveredQuantity={}, shortageQuantity={}",
                    impact.orderId(), impact.orderLineId(), impact.finalAllocatedQuantity(), impact.shortageQuantity());
            }
            if (impact.lineStatus() == Status.CANCELED) {
                log.warn("Order line canceled because no quantity can be fulfilled: orderId={}, orderLineId={}",
                    impact.orderId(), impact.orderLineId());
            }

            for (AlternativeAllocationPlan alternativePlan : impact.alternativePlans()) {
                Stock alternativeStock = stockRepository.findById(alternativePlan.stockId())
                    .orElseThrow(() -> new StockNotFoundException(alternativePlan.stockId()));
                alternativeStock.setReservedQuantity(Optional.ofNullable(alternativeStock.getReservedQuantity()).orElse(0) + alternativePlan.quantity());
                stocksToSave.add(alternativeStock);

                Allocation newAllocation = new Allocation(
                    impact.task(),
                    alternativeStock,
                    alternativePlan.quantity(),
                    resolveAllocationStatusForTask(impact.task())
                );
                allocationsToSave.add(newAllocation);
            }

            impactedOrderIds.add(impact.orderId());
        }

        allocationRepository.saveAll(allocationsToSave);
        stockRepository.saveAll(stocksToSave);
        orderLineRepository.saveAll(linesToSave);
        taskRepository.saveAll(tasksToSave);

        List<Order> ordersToSave = impactedOrderIds.stream()
            .distinct()
            .map(orderId -> orderRepository.findById(orderId).orElseThrow(() -> new InvalidRequestException("Order not found: " + orderId)))
            .peek(order -> order.setStatus(determineOrderStatus(order)))
            .toList();
        orderRepository.saveAll(ordersToSave);

        InventoryHistory history = new InventoryHistory(
            stock.getProduct().orElse(null),
            stock.getProduct().map(Product::getBarcode).orElse(null),
            difference,
            request.newQuantity(),
            previousQuantity,
            stockLocation,
            stockLocation,
            InventoryOperationType.ADJUST_STOCK,
            request.reason(),
            request.comment(),
            user
        );
        history.setTimestamp(LocalDateTime.now());
        InventoryHistory savedHistory = inventoryHistoryRepository.save(history);

        if (simulation.reallocationSucceeded()) {
            log.info("Reallocation succeeded for all affected order lines on stock {}", stock.getId());
        }

        return savedHistory.getId();
    }

    private Status resolveAllocationStatusForTask(Task task) {
        return switch (task.getStatus()) {
            case IN_PROGRESS -> Status.IN_PROGRESS;
            case ASSIGNED, REQUIRES_REVALIDATION -> Status.ASSIGNED;
            case COMPLETED -> Status.COMPLETED;
            case CANCELED -> Status.CANCELED;
            default -> Status.CREATED;
        };
    }

    private OrderStatus determineOrderStatus(Order order) {
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        if (orderLines.isEmpty()) {
            return order.getStatus();
        }

        boolean inProgress = orderLines.stream().anyMatch(line ->
            line.getStatus() == Status.IN_PROGRESS || line.getTask().getStatus() == TaskStatus.IN_PROGRESS
        );
        if (inProgress || order.getStatus() == OrderStatus.IN_PROGRESS) {
            return OrderStatus.IN_PROGRESS;
        }

        return order.getStatus();
    }

    private InventoryAdjustmentResponse toResponse(Stock stock, AdjustmentSimulation simulation, int previousQuantity, InventoryAdjustmentRequest request, boolean preview, Long historyId, int reservedQuantity) {
        List<AffectedOrderLineResponse> affectedOrderLines = simulation.taskImpacts().stream()
            .map(this::toAffectedOrderLineResponse)
            .toList();

        List<ShortageOrderResponse> shortageOrders = simulation.taskImpacts().stream()
            .collect(Collectors.groupingBy(TaskImpact::orderId, LinkedHashMap::new, Collectors.toList()))
            .values()
            .stream()
            .map(this::toShortageOrderResponse)
            .toList();

        boolean allFulfilled = affectedOrderLines.stream().noneMatch(line -> line.shortageQuantity() > 0 || Status.CANCELED.name().equals(line.status()));
        boolean anyShortage = affectedOrderLines.stream().anyMatch(line -> Status.PARTIALLY_COMPLETED.name().equals(line.status()));
        boolean anyCancelled = affectedOrderLines.stream().anyMatch(line -> Status.CANCELED.name().equals(line.status()));
        String message;
        if (anyCancelled && shortageOrders.stream().allMatch(order -> order.shortageLines() == order.totalLines())) {
            message = "Order cancelled because no lines can be fulfilled.";
        } else if (anyShortage) {
            message = "Partial completion created and affected order lines were recalculated.";
        } else if (allFulfilled) {
            message = "Reallocation succeeded for all affected order lines.";
        } else {
            message = "Inventory adjustment completed.";
        }

        return new InventoryAdjustmentResponse(
            toStockResponse(stock, request.newQuantity(), reservedQuantity),
            previousQuantity,
            request.newQuantity(),
            request.newQuantity() - previousQuantity,
            request.reason().name(),
            request.comment(),
            historyId,
            preview,
            allFulfilled,
            anyShortage,
            anyCancelled,
            message,
            shortageOrders,
            affectedOrderLines
        );
    }

    private AffectedOrderLineResponse toAffectedOrderLineResponse(TaskImpact impact) {
        return new AffectedOrderLineResponse(
            impact.orderId(),
            impact.orderNumber(),
            impact.orderLineId(),
            impact.taskId(),
            impact.orderLine().getProduct().getId(),
            impact.orderLine().getProduct().getName(),
            impact.requestedQuantity(),
            impact.finalAllocatedQuantity(),
            impact.shortageQuantity(),
            impact.originalLocationId(),
            impact.originalLocationBarcode(),
            impact.reallocatedLocationId(),
            impact.reallocatedLocationBarcode(),
            impact.lineStatus().name(),
            impact.revalidationRequired(),
            impact.orderCreatedAt(),
            impact.orderUpdatedAt()
        );
    }

    private ShortageOrderResponse toShortageOrderResponse(List<TaskImpact> impacts) {
        TaskImpact first = impacts.getFirst();
        long shortageLines = impacts.stream().filter(impact -> impact.shortageQuantity() > 0 || impact.lineStatus() == Status.CANCELED).count();
        return new ShortageOrderResponse(
            first.orderId(),
            first.orderNumber(),
            first.orderLine().getOrder().getDestinationLocation().getBarcode(),
            first.orderLine().getOrder().getStatus().name(),
            impacts.size(),
            Math.toIntExact(shortageLines),
            first.orderCreatedAt(),
            first.orderUpdatedAt()
        );
    }

    private StockResponse toStockResponse(Stock stock, Integer quantity, Integer reservedQuantity) {
        Stock snapshot = new Stock();
        snapshot.setId(stock.getId());
        snapshot.setQuantity(quantity);
        snapshot.setReservedQuantity(reservedQuantity);
        snapshot.setManufactureDate(stock.getManufactureDate());
        snapshot.setExpirationDate(stock.getExpirationDate());
        snapshot.setLocation(stock.getLocation());
        snapshot.setProduct(stock.getProduct().orElse(null));
        return stockMapper.toResponse(snapshot);
    }

    private boolean isActiveAllocation(Allocation allocation) {
        return allocation.getStatus() != Status.COMPLETED && allocation.getStatus() != Status.CANCELED;
    }

    private int availableQuantity(Stock stock) {
        return Optional.ofNullable(stock.getQuantity()).orElse(0) - Optional.ofNullable(stock.getReservedQuantity()).orElse(0);
    }

    private String getStockBarcode(Long stockId) {
        return stockRepository.findById(stockId)
            .map(stock -> stock.getLocation() == null ? null : stock.getLocation().getBarcode())
            .orElse(null);
    }

    private static final class AlternativeStockState {
        private final Stock stock;
        private final int availableQuantity;

        private AlternativeStockState(Stock stock, int availableQuantity) {
            this.stock = stock;
            this.availableQuantity = availableQuantity;
        }

        public Stock stock() {
            return stock;
        }

        public int availableQuantity() {
            return availableQuantity;
        }
    }

    private record AlternativeAllocationPlan(Long stockId, int quantity) {
    }

    private record AdjustmentSimulation(
        int previousQuantity,
        int newQuantity,
        InventoryAdjustmentReason reason,
        String comment,
        Stock stock,
        User user,
        List<TaskImpact> taskImpacts,
        Map<Long, Integer> preservedByTask,
        Map<Long, Integer> reducedByTask,
        Map<Long, List<AlternativeAllocationPlan>> alternativePlansByTask
    ) {
        private boolean reallocationSucceeded() {
            return taskImpacts.stream().noneMatch(impact -> impact.shortageQuantity() > 0 || impact.lineStatus() == Status.CANCELED);
        }
    }

    private record TaskImpact(
        Long taskId,
        Long orderId,
        String orderNumber,
        LocalDateTime orderCreatedAt,
        LocalDateTime orderUpdatedAt,
        Long orderLineId,
        Task task,
        OrderLine orderLine,
        Integer requestedQuantity,
        Integer allocatedFromOther,
        Integer preservedOnModified,
        Integer reducedFromModified,
        Integer allocatedFromAlternatives,
        Integer shortageQuantity,
        Status lineStatus,
        boolean revalidationRequired,
        Long originalLocationId,
        String originalLocationBarcode,
        Long reallocatedLocationId,
        String reallocatedLocationBarcode,
        List<AlternativeAllocationPlan> alternativePlans
    ) {
        Integer finalAllocatedQuantity() {
            return Optional.ofNullable(allocatedFromOther).orElse(0)
                + Optional.ofNullable(preservedOnModified).orElse(0)
                + Optional.ofNullable(allocatedFromAlternatives).orElse(0);
        }
    }
}
