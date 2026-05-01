package com.algotrail.backend.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewCompleteResponse(
        Long reviewScheduleId,
        String status,
        LocalDateTime completedAt,
        String message
) {
}