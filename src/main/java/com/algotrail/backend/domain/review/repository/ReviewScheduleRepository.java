package com.algotrail.backend.domain.review.repository;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.util.List;

public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, Long> {

    boolean existsBySolvedProblemIdAndReviewRound(Long solvedProblemId, int reviewRound);

    List<ReviewSchedule> findByReviewDateAndStatus(
            LocalDate reviewDate,
            String status
    );

    List<ReviewSchedule> findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
            Long userId,
            LocalDate reviewDate,
            String status
    );

    List<ReviewSchedule> findBySolvedProblemUserIdAndStatusAndReviewDateBetweenOrderByReviewDateAsc(
            Long userId,
            String status,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ReviewSchedule> findBySolvedProblemIdOrderByReviewRoundAsc(
            Long solvedProblemId
    );

    List<ReviewSchedule> findBySolvedProblemOrderByReviewRoundAsc(
            SolvedProblem solvedProblem
    );

    List<ReviewSchedule> findTop20ByStatusAndReviewDateLessThanEqualOrderByReviewDateAsc(
            String status,
            LocalDate reviewDate
    );

    long countBySolvedProblemUserIdAndStatus(
            Long userId,
            String status
    );

    List<ReviewSchedule> findBySolvedProblemUserIdAndStatusAndCompletedAtBetween(
            Long userId,
            String status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<ReviewSchedule> findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
            Long userId,
            String status
    );
}