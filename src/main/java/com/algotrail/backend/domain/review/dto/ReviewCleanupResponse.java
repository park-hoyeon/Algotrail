package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;

public record ReviewCleanupResponse(
        Long userId,
        LocalDate startDate,
        int deletedCount,
        String message
) {
}