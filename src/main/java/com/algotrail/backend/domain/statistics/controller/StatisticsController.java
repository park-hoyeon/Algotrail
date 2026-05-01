package com.algotrail.backend.domain.statistics.controller;

import com.algotrail.backend.domain.statistics.dto.ActivityHeatmapResponse;
import com.algotrail.backend.domain.statistics.dto.StatisticsResponse;
import com.algotrail.backend.domain.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.algotrail.backend.domain.statistics.dto.CategoryDistributionResponse;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public StatisticsResponse getStatistics(@RequestParam Long userId) {
        return statisticsService.getStatistics(userId);
    }

    @GetMapping("/activity-heatmap")
    public ActivityHeatmapResponse getActivityHeatmap(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "8") int weeks
    ) {
        return statisticsService.getActivityHeatmap(userId, weeks);
    }

    @GetMapping("/category-distribution")
    public CategoryDistributionResponse getCategoryDistribution(@RequestParam Long userId) {
        return statisticsService.getCategoryDistribution(userId);
    }
}