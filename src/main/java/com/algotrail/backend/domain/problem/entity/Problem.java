package com.algotrail.backend.domain.problem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"platform", "problemNumber"})
        }
)
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platform;   // PROGRAMMERS

    @Column(nullable = false)
    private Long problemNumber;

    @Column(nullable = false)
    private String title;

    private String level;

    private String problemUrl;

    public Problem(String platform, Long problemNumber, String title, String level, String problemUrl) {
        this.platform = platform;
        this.problemNumber = problemNumber;
        this.title = title;
        this.level = level;
        this.problemUrl = problemUrl;
    }
}