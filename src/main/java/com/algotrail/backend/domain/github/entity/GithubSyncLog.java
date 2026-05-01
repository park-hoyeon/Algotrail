package com.algotrail.backend.domain.github.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private LocalDateTime syncStartedAt;

    private LocalDateTime syncFinishedAt;

    private int addedCount;

    private int skippedCount;

    @Enumerated(EnumType.STRING)
    private GithubSyncStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    public GithubSyncLog(
            Long userId,
            LocalDateTime syncStartedAt,
            LocalDateTime syncFinishedAt,
            int addedCount,
            int skippedCount,
            GithubSyncStatus status,
            String message
    ) {
        this.userId = userId;
        this.syncStartedAt = syncStartedAt;
        this.syncFinishedAt = syncFinishedAt;
        this.addedCount = addedCount;
        this.skippedCount = skippedCount;
        this.status = status;
        this.message = message;
    }
}