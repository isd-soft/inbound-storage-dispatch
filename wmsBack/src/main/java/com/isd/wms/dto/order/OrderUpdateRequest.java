package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public record OrderUpdateRequest(
        @NotBlank String logicId,
        @NonNull OrderStatus status
) {
}
