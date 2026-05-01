package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblem, Long> {

    boolean existsByUserAndProblem(User user, Problem problem);
}