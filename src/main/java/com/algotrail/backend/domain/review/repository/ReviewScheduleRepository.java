package com.algotrail.backend.domain.review.repository;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, Long> {

    boolean existsBySolvedProblemIdAndReviewRound(Long solvedProblemId, int reviewRound);

    boolean existsBySolvedProblemId(Long solvedProblemId);

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

    List<ReviewSchedule> findBySolvedProblemUserIdAndStatus(
            Long userId,
            String status
    );

    List<ReviewSchedule> findBySolvedProblemUserIdAndStatusAndReviewDateBefore(
            Long userId,
            String status,
            LocalDate reviewDate
    );

    void deleteBySolvedProblemIdIn(List<Long> solvedProblemIds);

    @Query("""
        SELECT rs
        FROM ReviewSchedule rs
        JOIN FETCH rs.solvedProblem sp
        JOIN FETCH sp.problem p
        WHERE sp.user.id = :userId
          AND rs.status = :status
          AND rs.reviewDate >= :startDate
          AND rs.reviewDate <= :endDate
        ORDER BY rs.reviewDate ASC, rs.reviewRound ASC
    """)
    List<ReviewSchedule> findUpcomingReviewsByUser(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT DISTINCT rs.completedAt
        FROM ReviewSchedule rs
        WHERE rs.solvedProblem.user.id = :userId
          AND rs.status = 'COMPLETED'
        """)
    List<LocalDate> findCompletedDates(Long userId);

    @Modifying
    @Query("""
    DELETE FROM ReviewSchedule rs
    WHERE rs.solvedProblem.user.id = :userId
""")
    void deleteAllByUserId(@Param("userId") Long userId);
}