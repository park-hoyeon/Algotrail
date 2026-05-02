package com.algotrail.backend.domain.github.dto;

public record GithubRepositoryConnectRequest(
        Long userId,
        String githubUsername,
        String repositoryName,
        String repositoryUrl,
        String defaultBranch,
        String rootPath
) {
}