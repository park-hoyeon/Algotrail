package com.algotrail.backend.domain.review.repository;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, Long> {

    List<ReviewSchedule> findByReviewDateAndStatus(LocalDate date, String status);

    List<ReviewSchedule> findBySolvedProblemOrderByReviewRoundAsc(SolvedProblem solvedProblem);

    List<ReviewSchedule> findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
            Long userId,
            LocalDate reviewDate,
            String status
    );
}