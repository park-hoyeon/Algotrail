package com.algotrail.backend.domain.github.dto;

public record GithubRepoResponse(
        String name,
        String fullName,
        String htmlUrl,
        String defaultBranch
) {
}