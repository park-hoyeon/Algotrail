package com.algotrail.backend.domain.recommendation.dto;

public record RecommendationResponse(
        Long categoryId,
        String categoryName,
        long solvedCount,
        String message
) {
}