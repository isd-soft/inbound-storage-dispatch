package com.isd.wms.service.ai;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.service.ReplenishmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI tool functions for managing replenishment tasks.
 * <p>
 * Provides tools for creating, assigning, cancelling, and querying replenishments.
 * Replenishments move stock from bulk storage (REPL zone) to picking zones.
 * All operations delegate to the core {@link ReplenishmentService}.
 * </p>
 *
 * @see ReplenishmentService
 * @see Replenishment
 * @see Task
 */
@Slf4j
@Service("replenishmentAiTools")
@RequiredArgsConstructor
public class ReplenishmentAiTools {

    private final ReplenishmentService replenishmentService;
    private final ReplenishmentRepository replenishmentRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new replenishment task to move a specified quantity of a product
     * to a destination picking location.
     *
     * @param productBarcode             the product barcode
     * @param quantity                   the quantity to replenish
     * @param destinationLocationBarcode the destination location (must exist)
     * @return a success or error message
     */
    @Tool(description = "Creates a new Replenishment Task to move existing stock from a REPL zone to a PICK zone.")
    public String createReplenishmentTask(
        @ToolParam(description = "Barcode of the product to replenish") String productBarcode,
        @ToolParam(description = "Quantity to replenish") Integer quantity,
        @ToolParam(description = "Barcode of the destination location (MUST be a valid location barcode)") String destinationLocationBarcode) {

        log.info("AI invoked createReplenishmentTask");
        Product product = findProductOrNull(productBarcode);
        if (product == null) return "Error: Product not found.";

        Location dest = findLocationOrNull(destinationLocationBarcode);
        if (dest == null) return "Error: Destination location barcode not found.";

        try {
            replenishmentService.createReplenishment(new ReplenishmentCreateRequest(product.getId(), quantity, dest.getId()));
            return "Success! Replenishment task has been created successfully.";
        } catch (Exception e) {
            return "Failed to create replenishment task: " + e.getMessage();
        }
    }

    /**
     * Assigns a replenishment task to an operator by its ID and operator username.
     *
     * @param replenishmentId   the replenishment reference ID
     * @param operatorUsername  the operator's username
     * @return a success or error message
     */
    @Tool(description = "Assigns a specific Replenishment Task to an Operator using the Ref ID.")
    public String assignReplenishmentToOperator(
        @ToolParam(description = "The Replenishment ID (Ref ID) to assign") Long replenishmentId,
        @ToolParam(description = "Exact username of the operator") String operatorUsername) {

        log.info("AI invoked assignReplenishmentToOperator");
        User operator = findOperatorOrNull(operatorUsername);
        if (operator == null) return "Error: Operator not found.";

        try {
            replenishmentService.assignReplenishment(replenishmentId, operator.getId());
            return "Success! Replenishment task #" + replenishmentId + " has been assigned to " + operatorUsername;
        } catch (Exception e) {
            return "Failed to assign replenishment: " + e.getMessage();
        }
    }

    /**
     * Cancels an active replenishment task. Releases any reserved stock.
     *
     * @param replenishmentId the replenishment reference ID
     * @return a success or error message
     */
    @Tool(description = "Cancels an existing active Replenishment Task using its Replenishment ID (Ref ID).")
    public String cancelReplenishmentTask(@ToolParam(description = "The Replenishment ID (Ref ID) to cancel") Long replenishmentId) {
        log.info("AI invoked cancelReplenishmentTask for ID {}", replenishmentId);
        try {
            replenishmentService.cancelReplenishment(replenishmentId);
            return "Success! Replenishment ID " + replenishmentId + " has been canceled and stock is released.";
        } catch (Exception e) {
            return "Failed to cancel replenishment: " + e.getMessage();
        }
    }

    /**
     * Returns a summary of all active replenishment tasks (CREATED, ASSIGNED, IN_PROGRESS).
     *
     * @return a Markdown table with task details
     */
    @Tool(description = "Returns a summary of all currently active (CREATED, ASSIGNED, IN_PROGRESS) replenishment tasks.")
    public String getActiveReplenishmentsInfo() {
        log.info("AI invoked getActiveReplenishmentsInfo tool");
        List<Replenishment> activeTasks = replenishmentRepository.findByStatus(Status.IN_PROGRESS);
        activeTasks.addAll(replenishmentRepository.findByStatus(Status.ASSIGNED));
        activeTasks.addAll(replenishmentRepository.findByStatus(Status.CREATED));

        return activeTasks.isEmpty() ? "There are currently no active replenishment tasks." : formatActiveTasks(activeTasks);
    }

    private Product findProductOrNull(String barcode) {
        return productRepository.findByBarcode(barcode).orElse(null);
    }

    private Location findLocationOrNull(String barcode) {
        return locationRepository.findAll().stream().filter(l -> l.getBarcode().equalsIgnoreCase(barcode)).findFirst().orElse(null);
    }

    private User findOperatorOrNull(String username) {
        return userRepository.findAll().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    private String formatActiveTasks(List<Replenishment> tasks) {
        StringBuilder sb = new StringBuilder("### Active Replenishment Tasks\n");
        sb.append("| Ref ID | Product | Quantity | Destination | Operator | Status |\n");
        sb.append("|--------|---------|----------|-------------|----------|--------|\n");

        tasks.forEach(r -> {
            String dest = r.getDestinationLocation() != null ? r.getDestinationLocation().getBarcode() : "N/A";

            String operator = r.getTask()
                .flatMap(Task::getOperator)
                .map(User::getUsername)
                .orElse("Unassigned");

            sb.append(String.format("| %d | %s | %d | %s | %s | %s |\n",
                r.getId(), r.getProduct().getName(), r.getRequestedQuantity(), dest, operator, r.getStatus().name()));
        });

        return sb.toString();
    }
}
