package com.isd.wms.controller;

import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('OPERATOR', 'SUPERVISOR', 'DEV')")
    public List<ProcessResponse> getAvailableProcesses() {
        return processService.getAvailableProcesses();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public List<ProcessResponse> getMyProcesses() {
        return processService.getMyProcesses();
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessResponse assignProcess(@PathVariable Long id) {
        return processService.assignProcess(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessResponse completeProcess(@PathVariable Long id) {
        return processService.completeProcess(id);
    }
}