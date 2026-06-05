package com.isd.wms.dto.user;

public record UserCreateRequest(
        String username,
        String email,
        String password,
        String userRole
) {
}
