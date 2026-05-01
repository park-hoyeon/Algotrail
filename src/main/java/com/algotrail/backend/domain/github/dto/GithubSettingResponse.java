package com.algotrail.backend.domain.github.dto;

public record GithubSettingResponse(
        boolean connected,
        String githubUsername,
        String githubRepo,
        String lastSyncedAt
) {
}