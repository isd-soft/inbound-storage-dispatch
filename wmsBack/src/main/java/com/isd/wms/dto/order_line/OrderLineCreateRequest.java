package com.isd.wms.dto.order_line;

import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record OrderLineCreateRequest(
        Long orderId,
        @NonNull Long taskId,
        @NonNull Long productId,
        @NonNull
        @Min(0)
        Integer requestedQuantity
) {
        public OrderLineCreateRequest(OrderLineCreateRequest request, Long orderId) {
            this(orderId, request.taskId(), request.productId(), request.requestedQuantity());
        }
}
