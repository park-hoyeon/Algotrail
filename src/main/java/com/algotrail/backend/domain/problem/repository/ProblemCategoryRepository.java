package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemCategoryRepository extends JpaRepository<ProblemCategory, Long> {

    List<ProblemCategory> findByProblem(Problem problem);

    @Query("""
        SELECT pc.category.name, COUNT(sp.id)
        FROM SolvedProblem sp
        JOIN ProblemCategory pc ON sp.problem = pc.problem
        WHERE sp.user.id = :userId
        GROUP BY pc.category.name
        """)
    List<Object[]> countSolvedByCategory(@Param("userId") Long userId);

    @Query("""
        SELECT pc
        FROM ProblemCategory pc
        JOIN FETCH pc.problem p
        JOIN FETCH pc.category c
        WHERE c.name = :categoryName
        ORDER BY p.id ASC
        """)
    List<ProblemCategory> findByCategoryName(
            @Param("categoryName") String categoryName,
            Pageable pageable
    );
}