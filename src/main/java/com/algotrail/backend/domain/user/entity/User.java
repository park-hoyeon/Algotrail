package com.algotrail.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "providerId"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginProvider provider;

    @Column(nullable = false)
    private String providerId;

    private String githubUsername;

    private String githubRepo;

    public User(
            String username,
            String email,
            String profileImageUrl,
            LoginProvider provider,
            String providerId
    ) {
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    public void updateProfile(String username, String email, String profileImageUrl) {
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    public void connectGithub(String githubUsername, String githubRepo) {
        this.githubUsername = githubUsername;
        this.githubRepo = githubRepo;
    }

    public void disconnectGithub() {
        this.githubUsername = null;
        this.githubRepo = null;
    }
}