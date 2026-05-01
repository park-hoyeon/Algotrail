package com.algotrail.backend.domain.statistics.dto;

import java.util.List;

public record StatisticsResponse(
        long totalSolvedCount,
        long todaySolvedCount,
        long reviewCompletedCount,
        List<LevelStat> levelStats,
        List<DailyStat> weeklyStats
) {
    public record LevelStat(
            String level,
            long solvedCount
    ) {
    }

    public record DailyStat(
            String date,
            long solvedCount
    ) {
    }
}