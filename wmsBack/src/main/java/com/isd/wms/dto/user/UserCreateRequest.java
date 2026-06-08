package com.isd.wms.dto.user;

import com.isd.wms.validation.StrongPassword;

public record UserCreateRequest(
        String username,
        String email,
        @StrongPassword String password,
        String userRole
) {
}
