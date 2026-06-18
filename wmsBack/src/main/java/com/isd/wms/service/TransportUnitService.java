package com.isd.wms.service;

import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.TransportUnit;
import com.isd.wms.exception.TransportUnitNotFoundException;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.TransportUnitRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TransportUnitService {

    private final TransportUnitRepository tuRepository;
    private final AllocationRepository allocationRepository;
    private final OrderRepository orderRepository;
    private final ReplenishmentRepository replenishmentRepository;
    private static final String BARCODE_REGEX = "^TU\\d{6}$";

    public TransportUnitService(TransportUnitRepository tuRepository,
                                AllocationRepository allocationRepository,
                                OrderRepository orderRepository,
                                ReplenishmentRepository replenishmentRepository) {
        this.tuRepository = tuRepository;
        this.allocationRepository = allocationRepository;
        this.orderRepository = orderRepository;
        this.replenishmentRepository = replenishmentRepository;
    }

    @Transactional
    public void occupyTransportUnit(String barcode, Long taskId, boolean isOrder) {
        log.info("Starting validation and occupation for TU barcode: {}, Allocation ID: {}, Process type: {}",
            barcode, taskId, isOrder ? "ORDER" : "REPLENISHMENT");

        if (barcode == null || !barcode.matches(BARCODE_REGEX)) {
            log.warn("Validation failed: Barcode format '{}' is invalid", barcode);
            throw new IllegalArgumentException("Invalid format! Barcode must start with 'TU' followed by 6 digits.");
        }

        // 1. Preluăm Alocarea trimisă de frontend (unde taskId de pe front reprezintă allocationId)
        Allocation allocation = allocationRepository.findById(taskId)
            .orElseThrow(() -> {
                log.error("Allocation with ID {} not found", taskId);
                return new EntityNotFoundException("Allocation with ID " + taskId + " not found");
            });

        if (allocation.getTask() == null) {
            log.error("Allocation ID {} does not have any Task associated!", taskId);
            throw new IllegalStateException("Allocation is missing its parent Task!");
        }

        // Extragem ID-ul real al task-ului din graful alocării
        Long realTaskId = allocation.getTask().getId();

        // 2. Verificăm unitatea de transport scanată
        TransportUnit tu = tuRepository.findByBarcode(barcode)
            .orElseThrow(() -> {
                log.error("Scanned barcode does not exist in database: {}", barcode);
                return new TransportUnitNotFoundException(barcode);
            });

        if (tu.getOrder() != null || tu.getReplenishment() != null) {
            log.warn("Attempted to occupy an active TU: {}. Associated Order: {}, Associated Replenishment: {}",
                barcode,
                tu.getOrder() != null ? tu.getOrder().getId() : "N/A",
                tu.getReplenishment() != null ? tu.getReplenishment().getId() : "N/A");
            throw new IllegalStateException("This TU is already locked in another active process!");
        }

        // 3. Identificăm corect rădăcina procesului în funcție de tipul de flux
        if (isOrder) {
            // Pentru Order: Căutăm folosind query-ul tău custom cu obiectul Task
            var order = orderRepository.getOrderByTask(allocation.getTask())
                .orElseThrow(() -> {
                    log.error("No Order found associated with Task ID {}", realTaskId);
                    return new EntityNotFoundException("Order not found for the associated Task.");
                });

            log.info("TU {} successfully linked to Order ID: {}", barcode, order.getId());
            tu.setOrder(order);
            tu.setReplenishment(null);
        } else {
            // Pentru Replenishment: Apelăm metoda ta nativă findByTaskId definită în ReplenishmentRepository
            var replenishment = replenishmentRepository.findByTaskId(realTaskId)
                .orElseThrow(() -> {
                    log.error("No Replenishment found for Task ID {}", realTaskId);
                    return new EntityNotFoundException("Replenishment not found for Task ID " + realTaskId);
                });

            log.info("TU {} successfully linked to Replenishment ID: {}", barcode, replenishment.getId());
            tu.setReplenishment(replenishment);
            tu.setOrder(null);
        }

        // 4. Salvăm modificările securizat
        tuRepository.save(tu);
        log.info("Changes for TU {} successfully saved.", barcode);
    }

    @Transactional
    public void releaseTransportUnit(String barcode) {
        log.info("Starting release process for TU: {}", barcode);

        TransportUnit tu = tuRepository.findByBarcode(barcode)
            .orElseThrow(() -> {
                log.error("Release failed: TU with barcode {} does not exist", barcode);
                return new EntityNotFoundException("The specified Transport Unit was not found.");
            });

        tu.setOrder(null);
        tu.setReplenishment(null);
        tuRepository.save(tu);

        log.info("TU {} is now free and available for use.", barcode);
    }
}
