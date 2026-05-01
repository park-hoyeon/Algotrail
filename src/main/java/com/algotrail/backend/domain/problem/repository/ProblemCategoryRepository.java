package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProblemCategoryRepository extends JpaRepository<ProblemCategory, Long> {

    List<ProblemCategory> findByProblem(Problem problem);

    void deleteByProblem(Problem problem);

    @Query("""
            SELECT pc.category.id, COUNT(sp.id)
            FROM SolvedProblem sp
            JOIN ProblemCategory pc ON sp.problem = pc.problem
            WHERE sp.user.id = :userId
            GROUP BY pc.category.id
            """)
    List<Object[]> countSolvedByCategory(Long userId);
}