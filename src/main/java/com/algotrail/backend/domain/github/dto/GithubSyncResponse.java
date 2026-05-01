package com.algotrail.backend.domain.github.dto;

import java.time.LocalDateTime;

public record GithubSyncResponse(
        Long userId,
        int newSolvedCount,
        int skippedCount,
        LocalDateTime syncStartedAt,
        LocalDateTime syncFinishedAt,
        String status,
        String message
) {
}