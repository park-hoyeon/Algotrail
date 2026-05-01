package com.algotrail.backend.domain.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record ActivityHeatmapResponse(
        int weeks,
        List<ActivityHeatmapItem> items
) {
    public record ActivityHeatmapItem(
            LocalDate date,
            long solvedCount,
            long reviewCompletedCount,
            long totalCount,
            int level
    ) {
    }
}