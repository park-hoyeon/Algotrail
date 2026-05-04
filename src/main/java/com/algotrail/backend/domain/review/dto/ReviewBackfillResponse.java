package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;

public record ReviewBackfillResponse(
        Long userId,
        LocalDate startDate,
        int createdProblemCount,
        int createdScheduleCount,
        String message
) {
}