package com.algotrail.backend.domain.review.entity;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SolvedProblem 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_problem_id")
    private SolvedProblem solvedProblem;

    private int reviewRound; // 1, 2, 3

    private LocalDate reviewDate;

    private String status; // PENDING / COMPLETED

    private LocalDateTime completedAt;

    public ReviewSchedule(SolvedProblem solvedProblem, int reviewRound, LocalDate reviewDate) {
        this.solvedProblem = solvedProblem;
        this.reviewRound = reviewRound;
        this.reviewDate = reviewDate;
        this.status = "PENDING";
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
}