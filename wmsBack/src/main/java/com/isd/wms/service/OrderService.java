package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.dto.order.shortage.AffectedOrderLineResponse;
import com.isd.wms.dto.order.shortage.ShortageDetailsResponse;
import com.isd.wms.dto.order.shortage.ShortageOrderResponse;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Stock;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final ExtendedOrderMapper extendedOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final LocationRepository locationRepository;
    private final OrderLineService orderLineService;
    private final AllocationRepository allocationRepository;
    private final TaskRepository taskRepository;
    private final OrderLineRepository orderLineRepository;
    private final SecurityFacade securityFacade;

    @Transactional
    public OrderResponse addExtendedOrder(ExtendedOrderCreateRequest request) {
        Order order = addOrder(request.order());
        for (OrderLineCreateRequest oRequest : request.lines()) {
            oRequest = new OrderLineCreateRequest(oRequest, order.getId());
            orderLineService.addOrderLine(order, oRequest);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    public Order addOrder(OrderCreateRequest request) {
        Order order = new Order(request.logicId(), getLocation(request.destinationLocationId()));
        return orderRepository.save(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        if (!request.status().equals(OrderStatus.CREATED)) {
            throw new InvalidRequestException("Order status must be CREATED");
        }
        Order order = getOrder(id);

        order.setLogicId(request.logicId());
        order.setDestinationLocation(getLocation(request.destinationLocationId()));
        order.setStatus(request.status());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrderById(Long id) {
        Order order = getOrder(id);
        releaseReservedStock(order);
        orderRepository.delete(order);
        log.info("Deleted order and released reserved stock: orderId={}", id);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByCreatedByUsername(securityFacade.getCurrentUsername()).stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    public OrderResponse getOrderById(@NonNull Long orderId) {
        return orderMapper.toResponse(getOrder(orderId));
    }

    public Order getOrder(@NonNull Long orderId) {
        return orderRepository.findByIdAndCreatedByUsername(orderId, securityFacade.getCurrentUsername())
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void assignOrder(Long orderId, Long operatorId) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.IN_PROGRESS
            || order.getStatus() == OrderStatus.PARTIALLY_COMPLETED
            || order.getStatus() == OrderStatus.COMPLETED
            || order.getStatus() == OrderStatus.CANCELED) {
            throw new InvalidRequestException("Order assignment is not allowed for IN_PROGRESS, PARTIALLY_COMPLETED, COMPLETED or CANCELED orders");
        }

        int updated = orderRepository.updateStatus(orderId, OrderStatus.ASSIGNED);
        if (updated == 0) {
            throw new OrderNotFoundException(orderId);
        }
        log.info("Updated order with id {}", orderId);

        int tasksUpdated = taskRepository.updateOperatorByOrderId(orderId, operatorId);
        log.info("Updated {} tasks for order {}", tasksUpdated, orderId);

        int allocationsUpdated = allocationRepository.updateStatusByOrderId(orderId, Status.ASSIGNED);
        log.info("Updated {} allocations for order {}", allocationsUpdated, orderId);

        int orderLinesUpdated = orderLineRepository.updateStatusByOrderId(orderId, Status.ASSIGNED);
        log.info("Updated {} order lines for order {}", orderLinesUpdated, orderId);
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException(locationId));
    }


    private void releaseReservedStock(Order order) {
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        for (OrderLine orderLine : orderLines) {
            if (orderLine.getTask() == null) {
                continue;
            }
            List<Allocation> allocations = allocationRepository.findAllByTaskId(orderLine.getTask().getId());
            for (Allocation allocation : allocations) {
                if (allocation.getStatus() == Status.COMPLETED || allocation.getStatus() == Status.CANCELED) {
                    continue;
                }
                Stock stock = allocation.getStock();
                int updatedReservedQuantity = Math.max(0, stock.getReservedQuantity() - Optional.ofNullable(allocation.getQuantity()).orElse(0));
                stock.setReservedQuantity(updatedReservedQuantity);
                log.info("Released reserved stock on order delete: orderId={}, orderLineId={}, stockId={}, releasedQuantity={}, remainingReserved={}",
                    order.getId(), orderLine.getId(), stock.getId(), allocation.getQuantity(), updatedReservedQuantity);
            }
        }
    }

    public ExtendedOrderResponse getExtendedOrderById(Long orderId) {
        return extendedOrderMapper.toResponse(getOrder(orderId));
    }



    public List<OrderResponse> searchOrders(OrderSearchRequest request) {
        return orderRepository.filter(
                securityFacade.getCurrentUsername(),
                request.logicId(),
                request.destinationLocationId(),
                request.status(),
                request.createdAt(),
                request.updatedAt()
            ).stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    public List<ExtendedOrderResponse> getAllExtendedOrders() {
        List<Order> orders = orderRepository.findAllByCreatedByUsername(securityFacade.getCurrentUsername());
        return orders.stream()
            .map(extendedOrderMapper::toResponse)
            .toList();
    }

    public List<ShortageOrderResponse> getShortageOrders() {
        return orderRepository.findAllByCreatedByUsername(securityFacade.getCurrentUsername()).stream()
            .filter(this::isShortageOrder)
            .map(this::toShortageOrderResponse)
            .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
            .toList();
    }

    public ShortageDetailsResponse getShortageDetails(Long orderId) {
        Order order = getOrder(orderId);
        List<OrderLine> lines = orderLineRepository.findAllByOrderId(order.getId());
        List<Allocation> allocations = allocationRepository.findAllByOrder(order);

        List<AffectedOrderLineResponse> shortageLines = lines.stream()
            .filter(line -> line.getStatus() == Status.PARTIALLY_COMPLETED
                || line.getStatus() == Status.CANCELED
                || Optional.ofNullable(line.getShortageQuantity()).orElse(0) > 0)
            .map(line -> toAffectedOrderLineResponse(order, line, allocations))
            .toList();

        return new ShortageDetailsResponse(
            order.getId(),
            order.getLogicId(),
            order.getDestinationLocation().getId(),
            order.getDestinationLocation().getBarcode(),
            order.getStatus().name(),
            shortageLines
        );
    }

    private boolean isShortageOrder(Order order) {
        List<OrderLine> lines = orderLineRepository.findAllByOrderId(order.getId());
        boolean allCanceled = !lines.isEmpty() && lines.stream().allMatch(line -> line.getStatus() == Status.CANCELED);
        boolean hasShortage = lines.stream().anyMatch(line ->
            line.getStatus() == Status.PARTIALLY_COMPLETED
                || line.getStatus() == Status.CANCELED
                || Optional.ofNullable(line.getShortageQuantity()).orElse(0) > 0
        );
        return hasShortage
            || allCanceled
            || order.getStatus() == OrderStatus.PARTIALLY_COMPLETED
            || order.getStatus() == OrderStatus.CANCELED;
    }

    private ShortageOrderResponse toShortageOrderResponse(Order order) {
        List<OrderLine> lines = orderLineRepository.findAllByOrderId(order.getId());
        long shortageLines = lines.stream()
            .filter(line -> line.getStatus() == Status.PARTIALLY_COMPLETED
                || line.getStatus() == Status.CANCELED
                || Optional.ofNullable(line.getShortageQuantity()).orElse(0) > 0)
            .count();
        return new ShortageOrderResponse(
            order.getId(),
            order.getLogicId(),
            order.getDestinationLocation().getBarcode(),
            order.getStatus().name(),
            lines.size(),
            Math.toIntExact(shortageLines),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private AffectedOrderLineResponse toAffectedOrderLineResponse(Order order, OrderLine line, List<Allocation> allocations) {
        List<Allocation> lineAllocations = allocations.stream()
            .filter(allocation -> line.getTask() != null && allocation.getTask().getId().equals(line.getTask().getId()))
            .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
            .toList();

        int deliveredQuantity = resolveDeliveredQuantity(line, lineAllocations);
        int shortageQuantity = Optional.ofNullable(line.getShortageQuantity()).orElse(Math.max(0, line.getRequestedQuantity() - deliveredQuantity));
        Long originalLocationId = lineAllocations.isEmpty() ? null : lineAllocations.getFirst().getStock().getLocation().getId();
        String originalLocationBarcode = lineAllocations.isEmpty() ? null : lineAllocations.getFirst().getStock().getLocation().getBarcode();
        Long reallocatedLocationId = lineAllocations.stream()
            .map(allocation -> allocation.getStock().getLocation())
            .filter(location -> location != null && !Objects.equals(location.getId(), originalLocationId))
            .map(Location::getId)
            .findFirst()
            .orElse(null);
        String reallocatedLocationBarcode = lineAllocations.stream()
            .map(allocation -> allocation.getStock().getLocation())
            .filter(location -> location != null && !Objects.equals(location.getId(), originalLocationId))
            .map(Location::getBarcode)
            .findFirst()
            .orElse(null);
        boolean revalidationRequired = line.getTask() != null && line.getTask().getStatus() == com.isd.wms.enums.TaskStatus.REQUIRES_REVALIDATION;

        return new AffectedOrderLineResponse(
            order.getId(),
            order.getLogicId(),
            line.getId(),
            line.getTask() == null ? null : line.getTask().getId(),
            line.getProduct().getId(),
            line.getProduct().getName(),
            line.getRequestedQuantity(),
            deliveredQuantity,
            shortageQuantity,
            originalLocationId,
            originalLocationBarcode,
            reallocatedLocationId,
            reallocatedLocationBarcode,
            line.getStatus().name(),
            revalidationRequired,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private int resolveDeliveredQuantity(OrderLine line, List<Allocation> lineAllocations) {
        Integer deliveredQuantity = line.getDeliveredQuantity();
        if (deliveredQuantity != null && deliveredQuantity > 0) {
            return deliveredQuantity;
        }

        return lineAllocations.stream()
            .filter(allocation -> allocation.getStatus() != Status.CANCELED)
            .mapToInt(allocation -> Optional.ofNullable(allocation.getQuantity()).orElse(0))
            .sum();
    }
}
