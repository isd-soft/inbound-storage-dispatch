package com.isd.wms.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public record OrderCreateRequest(
        @NotBlank String logicId
) {
}
