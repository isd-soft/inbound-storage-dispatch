package com.isd.wms.controller;

import com.isd.wms.dto.dashboard.SupervisorDashboardResponse;
import com.isd.wms.service.SupervisorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supervisor/dashboard")
@RequiredArgsConstructor
public class SupervisorDashboardController {

    private final SupervisorDashboardService supervisorDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public SupervisorDashboardResponse getDashboard() {
        return supervisorDashboardService.getDashboard();
    }
}
