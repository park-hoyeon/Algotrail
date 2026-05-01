package com.algotrail.backend.domain.problem.entity;

import com.algotrail.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolvedProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  User 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    //  Problem 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(length = 1000)
    private String githubUrl;

    private String language;

    private LocalDate solvedDate;

    @Enumerated(EnumType.STRING)
    private ProblemStatus status;

    private Integer solveTimeMinutes;

    @Column(columnDefinition = "TEXT")
    private String memo;

    public SolvedProblem(User user, Problem problem, String githubUrl, String language, LocalDate solvedDate) {
        this.user = user;
        this.problem = problem;
        this.githubUrl = githubUrl;
        this.language = language;
        this.solvedDate = solvedDate;
        this.status = ProblemStatus.SOLVED;
    }

    public void updateStatus(ProblemStatus status) {
        this.status = status;
    }

    public void updateInfo(ProblemStatus status, Integer solveTimeMinutes, String memo) {
        if (status != null) {
            this.status = status;
        }

        if (solveTimeMinutes != null) {
            this.solveTimeMinutes = solveTimeMinutes;
        }

        if (memo != null) {
            this.memo = memo;
        }
    }
}