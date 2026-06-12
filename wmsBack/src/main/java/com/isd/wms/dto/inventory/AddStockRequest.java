package com.isd.wms.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddStockRequest {
    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Location id is required")
    private Long locationId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private LocalDate manufactureDate;
    private LocalDate expirationDate;

    @NotNull(message = "User id is required")
    private Long userId;
}
