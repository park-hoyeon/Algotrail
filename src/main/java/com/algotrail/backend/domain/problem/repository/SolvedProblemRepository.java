package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemStatus;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblem, Long> {

    boolean existsByUserAndProblem(User user, Problem problem);

    List<SolvedProblem> findByUserIdOrderBySolvedDateDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndSolvedDate(Long userId, LocalDate solvedDate);

    List<SolvedProblem> findTop5ByUserIdOrderBySolvedDateDesc(Long userId);

    List<SolvedProblem> findByUserId(Long userId);

    List<SolvedProblem> findByUserIdAndSolvedDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
        SELECT DISTINCT sp
        FROM SolvedProblem sp
        JOIN FETCH sp.problem p
        WHERE sp.user.id = :userId
          AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
          AND (:status IS NULL OR sp.status = :status)
          AND (
                :categoryId IS NULL OR EXISTS (
                    SELECT pc.id
                    FROM ProblemCategory pc
                    WHERE pc.problem = p
                      AND pc.category.id = :categoryId
                )
          )
        ORDER BY sp.solvedDate DESC
        """)
    List<SolvedProblem> searchProblems(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") ProblemStatus status
    );

    @Query("""
    SELECT DISTINCT sp.solvedDate
    FROM SolvedProblem sp
    WHERE sp.user.id = :userId
    ORDER BY sp.solvedDate DESC
    """)
    List<LocalDate> findDistinctSolvedDatesByUserIdOrderBySolvedDateDesc(
            @Param("userId") Long userId
    );

    @Query("""
        select avg(sp.solveTimeMinutes)
        from SolvedProblem sp
        where sp.user.id = :userId
        and sp.solvedDate >= :startDate
        and sp.solveTimeMinutes is not null
        """)
    Double findAverageSolveTimeMinutesByUserIdAfterDate(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    boolean existsByUserIdAndProblemPlatformAndProblemProblemNumber(
            Long userId,
            String platform,
            Long problemNumber
    );

    @Query("""
    SELECT DISTINCT sp.solvedDate
    FROM SolvedProblem sp
    WHERE sp.user.id = :userId
    """)
    List<LocalDate> findSolvedDates(Long userId);

}