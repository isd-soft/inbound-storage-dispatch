package com.isd.wms.service;

import com.isd.wms.dto.inventory.InventoryAdjustmentRequest;
import com.isd.wms.dto.inventory.InventoryAdjustmentResponse;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentApplier;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentContext;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentPlan;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentPlanner;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentResponseMapper;
import com.isd.wms.service.inventoryadjustment.InventoryAdjustmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAdjustmentService {

    private final InventoryAdjustmentValidator inventoryAdjustmentValidator;
    private final InventoryAdjustmentPlanner inventoryAdjustmentPlanner;
    private final InventoryAdjustmentApplier inventoryAdjustmentApplier;
    private final InventoryAdjustmentResponseMapper inventoryAdjustmentResponseMapper;

    @Transactional(readOnly = true)
    public InventoryAdjustmentResponse previewAdjustment(Long stockId, InventoryAdjustmentRequest request) {
        return processAdjustment(stockId, request, true);
    }

    @Transactional
    public InventoryAdjustmentResponse adjustStock(Long stockId, InventoryAdjustmentRequest request) {
        return processAdjustment(stockId, request, false);
    }

    private InventoryAdjustmentResponse processAdjustment(Long stockId, InventoryAdjustmentRequest request, boolean preview) {
        InventoryAdjustmentContext context = inventoryAdjustmentValidator.validateAndLoad(stockId, request);
        log.info(
            "Stock adjustment started: stockId={}, previousQuantity={}, newQuantity={}, difference={}, reason={}, userId={}, preview={}",
            stockId,
            context.previousQuantity(),
            context.newQuantity(),
            context.quantityDifference(),
            request.reason(),
            request.userId(),
            preview
        );

        InventoryAdjustmentPlan plan = inventoryAdjustmentPlanner.buildPlan(context);
        log.info(
            "Adjustment plan calculated: stockId={}, affectedOrderLines={}, affectedOrders={}, preservedQuantityOnAdjustedStock={}",
            stockId,
            plan.affectedTasks().size(),
            plan.affectedTasks().stream().map(affectedTask -> affectedTask.orderId()).distinct().count(),
            plan.preservedQuantityOnAdjustedStock()
        );

        Long inventoryHistoryId = preview ? null : inventoryAdjustmentApplier.applyAdjustmentPlan(plan);
        InventoryAdjustmentResponse response = inventoryAdjustmentResponseMapper.toResponse(plan, preview, inventoryHistoryId);

        log.info(
            "Stock adjustment completed: stockId={}, preview={}, affectedOrders={}, affectedLines={}, reallocationSucceeded={}, partialShortage={}, orderCancelled={}",
            stockId,
            preview,
            response.affectedOrders().size(),
            response.affectedOrderLines().size(),
            response.reallocationSucceeded(),
            response.partialShortageCreated(),
            response.orderCancelled()
        );

        if (context.quantityDifference() < 0) {
            log.info("Stock reduction triggered shortage recalculation: stockId={}, reason={}", stockId, request.reason());
        }

        return response;
    }
}
