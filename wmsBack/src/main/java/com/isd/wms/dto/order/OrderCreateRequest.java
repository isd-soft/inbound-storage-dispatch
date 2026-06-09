package com.isd.wms.dto.order;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import lombok.NonNull;

import java.util.List;

public record OrderCreateRequest(
        @NonNull String logicId,
        @NonNull List<OrderLineCreateRequest> orderLines
) {
}
