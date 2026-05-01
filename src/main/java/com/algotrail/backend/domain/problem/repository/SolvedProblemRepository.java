package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblem, Long> {
}