package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;

public record ReviewRebuildResponse(
        Long userId,
        LocalDate startDate,
        int deletedCount,
        int createdProblemCount,
        int createdScheduleCount,
        String message
) {
}