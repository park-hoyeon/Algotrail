package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProblemReviewScheduleResponse(
        Long reviewScheduleId,
        Integer reviewRound,
        LocalDate reviewDate,
        String status,
        LocalDateTime completedAt
) {
}