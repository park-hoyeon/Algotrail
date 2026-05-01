package com.algotrail.backend.domain.problem.repository;

import com.algotrail.backend.domain.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findByPlatformAndTitle(String platform, String title);
}