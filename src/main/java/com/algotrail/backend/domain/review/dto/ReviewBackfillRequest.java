package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;

public record ReviewBackfillRequest(
        Long userId,
        LocalDate startDate
) {
}