package com.algotrail.backend.domain.dashboard.controller;

import com.algotrail.backend.domain.dashboard.dto.DashboardResponse;
import com.algotrail.backend.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard(@RequestParam Long userId) {
        return dashboardService.getDashboard(userId);
    }
}