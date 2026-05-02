package com.algotrail.backend.domain.github.entity;

import com.algotrail.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GithubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String githubUsername;

    private String repositoryName;

    private String repositoryUrl;

    private String defaultBranch;

    private boolean connected;

    private LocalDateTime connectedAt;

    private LocalDateTime lastSyncedAt;

    private String rootPath;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void updateRepository(
            String githubUsername,
            String repositoryName,
            String repositoryUrl,
            String defaultBranch,
            String rootPath
    ) {
        this.githubUsername = githubUsername;
        this.repositoryName = repositoryName;
        this.repositoryUrl = repositoryUrl;
        this.defaultBranch = defaultBranch;
        this.rootPath = rootPath == null ? "" : rootPath;
        this.connected = true;
        this.connectedAt = LocalDateTime.now();
    }

    public void updateLastSyncedAt() {
        this.lastSyncedAt = LocalDateTime.now();
    }
}