package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentTaskCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskResponse;
import com.isd.wms.dto.replenishment.ReplenishmentTaskSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.ReplenishmentTask;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentTaskStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentTaskNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.ReplenishmentTaskMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.ReplenishmentTaskRepository;
import com.isd.wms.repository.UserRepository;
import org.springframework.data.annotation.Version;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReplenishmentTaskService {
    private final ReplenishmentTaskRepository replenishmentTaskRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ReplenishmentTaskMapper replenishmentTaskMapper;

    public ReplenishmentTaskService(
            ReplenishmentTaskRepository replenishmentTaskRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            LocationRepository locationRepository,
            ReplenishmentTaskMapper replenishmentTaskMapper) {
        this.replenishmentTaskRepository = replenishmentTaskRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.replenishmentTaskMapper = replenishmentTaskMapper;
    }

    @Transactional
    public ReplenishmentTaskResponse createReplenishmentTask(ReplenishmentTaskCreateRequest request) {
        validateReplenishmentTaskRequest(
                request.productId(),
                Long.valueOf(0),
                request.requestedQuantity(),
                ReplenishmentTaskStatus.CREATED,
                request.sourceLocationId(),
                request.destinationLocationId()
        );

        Product product = getProduct(request.productId());
        Location sourceLocation = getLocation(request.sourceLocationId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        ReplenishmentTask task = new ReplenishmentTask(
                product,
                request.requestedQuantity(),
                ReplenishmentTaskStatus.CREATED,
                sourceLocation,
                destinationLocation
        );
        return replenishmentTaskMapper.toResponse(replenishmentTaskRepository.save(task));
    }


    @Transactional
    public ReplenishmentTaskResponse updateReplenishmentTask(Long id, ReplenishmentTaskUpdateRequest request) {
        validateReplenishmentTaskRequest(
                request.productId(),
                request.operatorId(),
                request.requestedQuantity(),
                request.status(),
                request.sourceLocationId(),
                request.destinationLocationId()
        );

        ReplenishmentTask task = getReplenishmentTask(id);
        Product product = getProduct(request.productId());
        User operator = getUser(request.operatorId());
        Location sourceLocation = getLocation(request.sourceLocationId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        task.setProduct(product);
        task.setOperator(operator);
        task.setRequestedQuantity(request.requestedQuantity());
        task.setStatus(request.status());
        task.setSourceLocation(sourceLocation);
        task.setDestinationLocation(destinationLocation);

        return replenishmentTaskMapper.toResponse(replenishmentTaskRepository.save(task));
    }

    @Transactional
    public void deleteReplenishmentTask(Long replenishmentTaskId) {
        replenishmentTaskRepository.delete(getReplenishmentTask(replenishmentTaskId));
    }

    @Transactional
    public ReplenishmentTaskResponse assignReplenishmentTask(Long replenishmentTaskId) {
        ReplenishmentTask task = getReplenishmentTask(replenishmentTaskId);
        String username = getCurrentUsername();
        validateReplenishmentTaskRequest(username, task.getStatus());
        User user = getUser(username);
        task.setStatus(ReplenishmentTaskStatus.ASSIGNED);
        task.setOperator(user);
        return replenishmentTaskMapper.toResponse(replenishmentTaskRepository.save(task));
    }

    public ReplenishmentTaskResponse getReplenishmentTaskById(Long replenishmentTaskId) {
        return replenishmentTaskMapper.toResponse(getReplenishmentTask(replenishmentTaskId));
    }

    public List<ReplenishmentTaskResponse> getAllReplenishmentTasks() {
        return replenishmentTaskRepository.findAll().stream()
                .map(replenishmentTaskMapper::toResponse)
                .toList();
    }

    public List<ReplenishmentTaskResponse> searchReplenishmentTasks(ReplenishmentTaskSearchRequest request) {
        List<ReplenishmentTask> tasks;

        tasks = replenishmentTaskRepository.filter(
                request.productId(),
                request.operatorId(),
                request.requestedQuantity(),
                request.status(),
                request.sourceLocationId(),
                request.destinationLocationId()
        );

        return tasks.stream().map(replenishmentTaskMapper::toResponse).toList();
    }

    private ReplenishmentTask getReplenishmentTask(Long replenishmentTaskId) {
        return replenishmentTaskRepository.findById(replenishmentTaskId)
                .orElseThrow(() -> new ReplenishmentTaskNotFoundException(replenishmentTaskId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication.getName();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ProductNotFoundException(locationId));
    }

    private void validateReplenishmentTaskRequest(
            Long productId,
            Long operatorId,
            Integer requestedQuantity,
            ReplenishmentTaskStatus status,
            Long sourceLocationId,
            Long destinationLocationId) {
        if (productId == null) {
            throw new InvalidRequestException("Replenishment Task product id is required");
        }
        if (operatorId == null) {
            throw new InvalidRequestException("Replenishment Task operator id is required");
        }
        if (requestedQuantity == null) {
            throw new InvalidRequestException("Replenishment Task requested quantity is required");
        }
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment Task requested quantity cannot be nonpositive");
        }
        if (sourceLocationId == null) {
            throw new InvalidRequestException("Replenishment Task source location id is required");
        }
        if (destinationLocationId == null) {
            throw new InvalidRequestException("Replenishment Task destination location id is required");
        }
    }

    public void validateReplenishmentTaskRequest(String username, ReplenishmentTaskStatus status) {
        if (username == null || username.isEmpty()) {
            throw new InvalidRequestException("Replenishment Task Username is required");
        }
        if (status != ReplenishmentTaskStatus.CREATED) {
            throw new InvalidRequestException("Replenishment Task status must be CREATED");
        }
    }
}
