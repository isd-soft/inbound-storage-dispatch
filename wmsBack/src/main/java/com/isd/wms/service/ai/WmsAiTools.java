package com.isd.wms.service.ai;

import com.isd.wms.dto.ai.StockCheckRequest;
import com.isd.wms.dto.order.ExtendedOrderCreateRequest;
import com.isd.wms.dto.order.OrderCreateRequest;
import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.repository.*;
import com.isd.wms.service.OrderService;
import com.isd.wms.service.ReplenishmentService;
import com.isd.wms.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("wmsAiTools")
@RequiredArgsConstructor
public class WmsAiTools {

    public record AiOrderItem(String productBarcode, Integer quantity) {}

    private final ReplenishmentRepository replenishmentRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReplenishmentService replenishmentService;
    private final OrderService orderService;
    private final TaskService taskService;

    @Tool(description = "Searches for products by name. IMPORTANT: If a multi-word search fails, try calling this tool again with a SINGLE, core keyword in singular form (e.g., use 'laptop' instead of 'laptops pro').")
    public String searchProductByName(@ToolParam(description = "A single keyword or short phrase to search for") String nameQuery) {
        log.info("AI invoked searchProductByName tool for query: {}", nameQuery);
        String[] keywords = nameQuery.toLowerCase().split("\\s+");

        List<Product> matches = productRepository.findAll().stream()
            .filter(p -> matchesKeywords(p.getName().toLowerCase(), keywords))
            .toList();

        return matches.isEmpty()
            ? "No products found matching the keyword: '" + nameQuery + "'. Try again with a different, simpler, and singular keyword."
            : formatProductList(matches);
    }

    @Tool(description = "Lists all available physical locations (shelves) in the warehouse. Use this to find a valid destination barcode before creating a Replenishment or an Order.")
    public String getWarehouseLocations() {
        log.info("AI invoked getWarehouseLocations tool");
        List<Location> locations = locationRepository.findAll();
        return locations.isEmpty() ? "No locations found in the warehouse." : formatLocations(locations);
    }

    @Tool(description = "Lists all registered Operators in the system. Use this to find an operator's exact username before assigning an order or task to them.")
    public String getAvailableOperators() {
        log.info("AI invoked getAvailableOperators tool");
        List<User> operators = userRepository.findAll().stream()
            .filter(u -> u.getUserRole().name().equals("ROLE_OPERATOR"))
            .toList();

        return operators.isEmpty() ? "No operators found in the system." : formatOperators(operators);
    }

    @Tool(description = "Use this to check ALL detailed information about a product and its stock in the warehouse by barcode.")
    public String checkStockByBarcode(StockCheckRequest request) {
        log.info("AI invoked checkStockByBarcode tool for barcode: {}", request.barcode());
        Product product = findProductOrNull(request.barcode());
        if (product == null) return "Product with barcode " + request.barcode() + " not found in the database.";

        List<Stock> stocks = stockRepository.findAvailableStocksByProductId(product.getId());
        return formatStockDetails(product, stocks);
    }

    @Tool(description = "Returns a summary of all currently active (CREATED, ASSIGNED, IN_PROGRESS) replenishment tasks.")
    public String getActiveReplenishmentsInfo() {
        log.info("AI invoked getActiveReplenishmentsInfo tool");
        List<Replenishment> activeTasks = replenishmentRepository.findByStatus(Status.IN_PROGRESS);
        activeTasks.addAll(replenishmentRepository.findByStatus(Status.ASSIGNED));
        activeTasks.addAll(replenishmentRepository.findByStatus(Status.CREATED));

        return activeTasks.isEmpty() ? "There are currently no active replenishment tasks." : formatActiveTasks(activeTasks);
    }

    @Tool(description = "Checks the database for products that have low stock across the entire warehouse.")
    public String getLowStockWarning() {
        log.info("AI invoked getLowStockWarning tool");
        List<Product> allProducts = productRepository.findAll();
        return formatLowStockWarnings(allProducts);
    }

    @Tool(description = "Creates a new Replenishment Task to move stock from a REPL zone to a PICK zone.")
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

    @Tool(description = "Assigns a specific Replenishment Task to an Operator using the Task ID.")
    public String assignReplenishmentToOperator(
        @ToolParam(description = "The Task ID of the replenishment") Long taskId,
        @ToolParam(description = "Exact username of the operator") String operatorUsername) {

        log.info("AI invoked assignReplenishmentToOperator");
        User operator = findOperatorOrNull(operatorUsername);
        if (operator == null) return "Error: Operator not found.";

        try {
            taskService.assignTask(taskId, operator.getId());
            return "Success! Replenishment task #" + taskId + " has been assigned to " + operatorUsername;
        } catch (Exception e) {
            return "Failed to assign replenishment: " + e.getMessage();
        }
    }

    @Tool(description = "Cancels an existing active Replenishment Task using its Replenishment ID.")
    public String cancelReplenishmentTask(@ToolParam(description = "The Replenishment ID (NOT Task ID) to cancel") Long replenishmentId) {
        log.info("AI invoked cancelReplenishmentTask");
        try {
            replenishmentService.cancelReplenishment(replenishmentId);
            return "Success! Replenishment ID " + replenishmentId + " has been canceled and stock is released.";
        } catch (Exception e) {
            return "Failed to cancel replenishment: " + e.getMessage();
        }
    }

    @Tool(description = "Creates a new Customer Order WITH order lines (products). Requires a logical ID, destination DISPATCH location, and a list of items to pick.")
    public String createOrder(
        @ToolParam(description = "Logical custom order ID provided by the user") String logicId,
        @ToolParam(description = "Barcode of the destination location (Dispatch zone)") String destinationLocationBarcode,
        @ToolParam(description = "List of products and quantities to include in the order") List<AiOrderItem> items) {

        log.info("AI invoked createOrder");
        Location dest = findLocationOrNull(destinationLocationBarcode);
        if (dest == null) return "Error: Destination location barcode not found.";

        List<OrderLineCreateRequest> lineRequests = new ArrayList<>();
        for (AiOrderItem item : items) {
            Product p = findProductOrNull(item.productBarcode());
            if (p == null) return "Error: Product with barcode " + item.productBarcode() + " not found.";
            lineRequests.add(new OrderLineCreateRequest(null, p.getId(), item.quantity()));
        }

        try {
            orderService.addExtendedOrder(new ExtendedOrderCreateRequest(new OrderCreateRequest(logicId, dest.getId()), lineRequests));
            return "Success! Order '" + logicId + "' created and picking tasks generated.";
        } catch (Exception e) {
            return "Failed to create order: " + e.getMessage();
        }
    }

    @Tool(description = "Assigns an existing Order to a specific Operator.")
    public String assignOrderToOperator(
        @ToolParam(description = "Logical order ID (e.g. 'ORD-123')") String logicId,
        @ToolParam(description = "Exact username of the operator") String operatorUsername) {

        log.info("AI invoked assignOrderToOperator");
        Order order = orderRepository.findAll().stream().filter(o -> o.getLogicId().equalsIgnoreCase(logicId)).findFirst().orElse(null);
        if (order == null) return "Error: Order with logical ID " + logicId + " not found.";

        User operator = findOperatorOrNull(operatorUsername);
        if (operator == null) return "Error: Operator '" + operatorUsername + "' not found.";

        try {
            orderService.assignOrder(order.getId(), operator.getId());
            return "Success! Order " + logicId + " has been assigned to operator " + operatorUsername;
        } catch (Exception e) {
            return "Failed to assign order: " + e.getMessage();
        }
    }

    @Tool(description = "Deletes an existing order by its logical ID (e.g., 'ORD-001').")
    public String deleteOrder(@ToolParam(description = "Logical order ID to delete") String logicId) {
        log.info("AI invoked deleteOrder");
        OrderResponse order = orderService.getAllOrders().stream().filter(o -> o.logicId().equalsIgnoreCase(logicId)).findFirst().orElse(null);
        if (order == null) return "Error: Order with logical ID " + logicId + " not found.";

        try {
            orderService.deleteOrderById(order.id());
            return "Success! Order " + logicId + " has been deleted.";
        } catch (Exception e) {
            return "Failed to delete order: " + e.getMessage();
        }
    }

    @Tool(description = "Automatically distributes ALL unassigned Orders and Replenishments among available operators (Least-Loaded balancing).")
    public String autoDistributeWorkload() {
        log.info("AI invoked autoDistributeWorkload");
        List<User> operators = userRepository.findAll().stream().filter(u -> u.getUserRole().name().equals("ROLE_OPERATOR")).toList();
        if (operators.isEmpty()) return "Error: No operators registered in the system.";

        List<Order> unassignedOrders = orderRepository.findAll().stream().filter(o -> o.getStatus() == OrderStatus.CREATED).toList();
        List<Replenishment> unassignedReplenishments = replenishmentRepository.findAll().stream().filter(r -> r.getStatus() == Status.CREATED).toList();

        if (unassignedOrders.isEmpty() && unassignedReplenishments.isEmpty()) {
            return "All tasks are already assigned. Workload is balanced.";
        }

        Map<User, Long> operatorLoadMap = calculateInitialWorkload(operators);
        int[] counts = executeDistribution(operators, operatorLoadMap, unassignedOrders, unassignedReplenishments);

        return String.format("Workload distribution completed successfully! Assigned %d Orders and %d Replenishments among %d operators.", counts[0], counts[1], operators.size());
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

    private boolean matchesKeywords(String productName, String[] keywords) {
        for (String kw : keywords) {
            if (kw.length() <= 2) continue;
            String singularKw = kw.endsWith("s") ? kw.substring(0, kw.length() - 1) : kw;
            if (productName.contains(singularKw)) return true;
        }
        return false;
    }

    private Map<User, Long> calculateInitialWorkload(List<User> operators) {
        List<Task> allTasks = replenishmentRepository.findAll().stream().map(Replenishment::getTask).toList();
        return operators.stream().collect(Collectors.toMap(op -> op, op -> allTasks.stream()
            .filter(t -> t.getOperator().isPresent() && t.getOperator().get().equals(op)).count()));
    }

    private int[] executeDistribution(List<User> operators, Map<User, Long> loadMap, List<Order> orders, List<Replenishment> replenishments) {
        int assignedOrders = 0, assignedRepls = 0;

        for (Order order : orders) {
            User leastLoaded = getLeastLoadedOperator(operators, loadMap);
            try {
                orderService.assignOrder(order.getId(), leastLoaded.getId());
                loadMap.put(leastLoaded, loadMap.get(leastLoaded) + 1);
                assignedOrders++;
            } catch (Exception ignored) {}
        }
        for (Replenishment repl : replenishments) {
            User leastLoaded = getLeastLoadedOperator(operators, loadMap);
            try {
                taskService.assignTask(repl.getTask().getId(), leastLoaded.getId());
                loadMap.put(leastLoaded, loadMap.get(leastLoaded) + 1);
                assignedRepls++;
            } catch (Exception ignored) {}
        }
        return new int[]{assignedOrders, assignedRepls};
    }

    private User getLeastLoadedOperator(List<User> operators, Map<User, Long> loadMap) {
        return loadMap.entrySet().stream().min(Comparator.comparingLong(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(operators.get(0));
    }

    private String formatProductList(List<Product> products) {
        StringBuilder sb = new StringBuilder("Found the following potential product matches:\n");
        products.forEach(p -> sb.append(String.format("- Name: %s | Barcode: %s\n", p.getName(), p.getBarcode())));
        sb.append("\nHint: Use the exact barcode from this list with the checkStockByBarcode tool to verify inventory before taking actions.");
        return sb.toString();
    }

    private String formatLocations(List<Location> locations) {
        StringBuilder sb = new StringBuilder("Available Locations:\n");
        locations.forEach(l -> sb.append(String.format("- Barcode: %s\n", l.getBarcode())));
        return sb.toString();
    }

    private String formatOperators(List<User> operators) {
        StringBuilder sb = new StringBuilder("Available Operators:\n");
        operators.forEach(op -> sb.append(String.format("- Username: %s | ID: %d\n", op.getUsername(), op.getId())));
        return sb.toString();
    }

    private String formatStockDetails(Product product, List<Stock> stocks) {
        StringBuilder sb = new StringBuilder("Product Details:\n");
        sb.append("- Name: ").append(product.getName()).append("\n")
            .append("- Barcode: ").append(product.getBarcode()).append("\n")
            .append("- Category: ").append(product.getCategory() != null ? product.getCategory().getName() : "None").append("\n")
            .append("- Auto-Replenish Enabled: ").append(product.getAutoReplenish()).append("\n");

        if (Boolean.TRUE.equals(product.getAutoReplenish())) {
            sb.append("- Min Threshold: ").append(product.getMinThreshold()).append("\n")
                .append("- Replenish Qty: ").append(product.getReplenishQty()).append("\n");
        }

        if (stocks.isEmpty()) return sb.append("\nStock Status: Product is currently fully out of stock (0 pcs in warehouse).").toString();

        sb.append("\nDetailed Stock Locations:\n");
        int totalQty = 0, totalReserved = 0, totalAvailable = 0;

        for (Stock stock : stocks) {
            int qty = stock.getQuantity(), res = stock.getReservedQuantity(), avail = qty - res;
            sb.append(String.format("- Location: %s | Total Physical: %d | Reserved (in tasks): %d | Available: %d\n",
                stock.getLocation().getBarcode(), qty, res, avail));
            totalQty += qty; totalReserved += res; totalAvailable += avail;
        }

        sb.append(String.format("\nWarehouse Summary: Total Physical: %d | Total Reserved: %d | Total Available: %d", totalQty, totalReserved, totalAvailable));
        return sb.toString();
    }

    private String formatActiveTasks(List<Replenishment> tasks) {
        StringBuilder sb = new StringBuilder("Here is the raw data for active tasks:\n");
        tasks.forEach(r -> sb.append(String.format("- Replenishment ID: %d | Task ID: %d | Product: %s | Quantity: %d | Status: %s\n",
            r.getId(), r.getTask().getId(), r.getProduct().getName(), r.getRequestedQuantity(), r.getStatus().name())));
        return sb.toString();
    }

    private String formatLowStockWarnings(List<Product> products) {
        StringBuilder sb = new StringBuilder("Low stock raw data:\n");
        boolean found = false;
        for (Product p : products) {
            if (p.getMinThreshold() != null) {
                int totalAvailable = stockRepository.findAvailableStocksByProductId(p.getId()).stream().mapToInt(s -> s.getQuantity() - s.getReservedQuantity()).sum();
                if (totalAvailable <= p.getMinThreshold()) {
                    sb.append(String.format("- %s (Barcode: %s): Available %d, Threshold %d\n", p.getName(), p.getBarcode(), totalAvailable, p.getMinThreshold()));
                    found = true;
                }
            }
        }
        return found ? sb.toString() : "All products are above their minimum thresholds.";
    }
}
