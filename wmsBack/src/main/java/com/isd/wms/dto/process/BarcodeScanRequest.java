package com.isd.wms.dto.process;

import jakarta.validation.constraints.NotBlank;

public record BarcodeScanRequest(
        @NotBlank String barcode
) {
}
