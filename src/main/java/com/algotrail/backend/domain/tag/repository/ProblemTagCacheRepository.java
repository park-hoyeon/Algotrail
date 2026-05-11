package com.algotrail.backend.domain.tag.repository;

import com.algotrail.backend.domain.tag.entity.ProblemTagCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemTagCacheRepository extends JpaRepository<ProblemTagCache, Long> {

    Optional<ProblemTagCache> findByPlatformAndProblemNumber(String platform, Long problemNumber);
}