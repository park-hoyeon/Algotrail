package com.algotrail.backend.domain.review.dto;

import java.time.LocalDate;
import java.util.List;

public record UpcomingReviewResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<UpcomingReviewItem> reviews
) {
    public record UpcomingReviewItem(
            Long reviewScheduleId,
            Long solvedProblemId,
            String title,
            String level,
            String status,
            Integer reviewRound,
            LocalDate reviewDate,
            String githubUrl,
            String language
    ) {
    }
}