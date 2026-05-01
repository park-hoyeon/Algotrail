package com.algotrail.backend.domain.github.dto;

public record GithubContentResponse(
        String name,
        String path,
        String type,
        String download_url,
        String html_url
) {
}