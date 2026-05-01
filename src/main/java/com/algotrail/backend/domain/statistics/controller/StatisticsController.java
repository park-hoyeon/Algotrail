package com.algotrail.backend.domain.statistics.controller;

import com.algotrail.backend.domain.statistics.dto.StatisticsResponse;
import com.algotrail.backend.domain.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/{userId}")
    public StatisticsResponse getStatistics(@PathVariable Long userId) {
        return statisticsService.getStatistics(userId);
    }
}