package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentNotFoundException;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReplenishmentService {
    private final ReplenishmentRepository replenishmentRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final ReplenishmentMapper replenishmentMapper;

    @Transactional
    public ReplenishmentResponse createReplenishment(ReplenishmentCreateRequest request) {
        validateReplenishmentRequest(
                request.productId(),
                request.requestedQuantity(),
                request.destinationLocationId()
        );

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        Replenishment replenishment = new Replenishment(
                product,
                request.requestedQuantity(),
                ReplenishmentStatus.CREATED,
                destinationLocation
        );
        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
    }


    @Transactional
    public ReplenishmentResponse updateReplenishment(Long id, ReplenishmentUpdateRequest request) {
        validateReplenishmentRequest(
                request.taskId(),
                request.productId(),
                request.requestedQuantity(),
                request.status(),
                request.destinationLocationId()
        );

        Replenishment replenishment = getReplenishment(id);
        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        replenishment.setProduct(product);
        replenishment.setRequestedQuantity(request.requestedQuantity());
        replenishment.setStatus(request.status());
        replenishment.setDestinationLocation(destinationLocation);

        return replenishmentMapper.toResponse(replenishmentRepository.save(replenishment));
    }

    @Transactional
    public void deleteReplenishment(Long replenishmentTaskId) {
        replenishmentRepository.delete(getReplenishment(replenishmentTaskId));
    }

    public ReplenishmentResponse getReplenishmentById(Long replenishmentTaskId) {
        return replenishmentMapper.toResponse(getReplenishment(replenishmentTaskId));
    }

    public List<ReplenishmentResponse> getAllReplenishments() {
        return replenishmentRepository.findAll().stream()
                .map(replenishmentMapper::toResponse)
                .toList();
    }

    public List<ReplenishmentResponse> searchReplenishments(ReplenishmentSearchRequest request) {
        List<Replenishment> tasks;

        tasks = replenishmentRepository.filter(
                request.taskId(),
                request.productId(),
                request.requestedQuantity(),
                request.status(),
                request.destinationLocationId()
        );

        return tasks.stream().map(replenishmentMapper::toResponse).toList();
    }

    private Replenishment getReplenishment(Long replenishmentTaskId) {
        return replenishmentRepository.findById(replenishmentTaskId)
                .orElseThrow(() -> new ReplenishmentNotFoundException(replenishmentTaskId));
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

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ProductNotFoundException(locationId));
    }

    private void validateReplenishmentRequest(
            @NonNull Long taskId,
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull ReplenishmentStatus status,
            @NonNull Long destinationLocationId) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment requested quantity cannot be nonpositive");
        }
        if (status != ReplenishmentStatus.CREATED) {
            throw new InvalidRequestException("Replenishment status must be CREATED");
        }
    }

    public void validateReplenishmentRequest(
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull Long destinationLocationId) {
    }
}
