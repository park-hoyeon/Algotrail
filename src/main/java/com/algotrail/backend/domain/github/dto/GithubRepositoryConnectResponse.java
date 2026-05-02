package com.algotrail.backend.domain.github.dto;

import java.time.LocalDateTime;

public record GithubRepositoryConnectResponse(
        Long userId,
        String githubUsername,
        String repositoryName,
        String repositoryUrl,
        String defaultBranch,
        String rootPath,
        boolean connected,
        LocalDateTime lastSyncedAt
) {
}