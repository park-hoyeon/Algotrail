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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_problem_id")
    private SolvedProblem solvedProblem;

    private int reviewRound;

    private LocalDate baseDate;

    private LocalDate reviewDate;

    private String status;

    private LocalDateTime completedAt;

    public ReviewSchedule(
            SolvedProblem solvedProblem,
            int reviewRound,
            LocalDate baseDate,
            LocalDate reviewDate
    ) {
        this.solvedProblem = solvedProblem;
        this.reviewRound = reviewRound;
        this.baseDate = baseDate;
        this.reviewDate = reviewDate;
        this.status = "PENDING";
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void retry() {
        this.status = "PENDING";
        this.completedAt = null;
    }
}