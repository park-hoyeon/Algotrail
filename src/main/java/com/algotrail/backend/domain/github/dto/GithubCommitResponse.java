package com.algotrail.backend.domain.github.dto;

import java.time.OffsetDateTime;

public record GithubCommitResponse(
        Commit commit
) {
    public record Commit(
            Author author
    ) {
    }

    public record Author(
            OffsetDateTime date
    ) {
    }
}