package com.algotrail.backend.domain.github.dto;

public record GithubSyncResponse(
        Long userId,
        int newSolvedCount,
        String message
) {
}