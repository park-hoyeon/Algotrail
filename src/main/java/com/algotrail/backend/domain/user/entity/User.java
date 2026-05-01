package com.algotrail.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String githubUsername;

    @Column(nullable = false)
    private String githubRepo;

    public User(String username, String githubUsername, String githubRepo) {
        this.username = username;
        this.githubUsername = githubUsername;
        this.githubRepo = githubRepo;
    }
}