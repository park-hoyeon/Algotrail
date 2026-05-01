package com.algotrail.backend.domain.category.dto;

public record CategoryBackfillResponse(
        int updatedCount,
        int skippedCount,
        String message
) {
}