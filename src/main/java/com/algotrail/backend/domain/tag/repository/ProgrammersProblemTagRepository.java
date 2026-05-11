package com.algotrail.backend.domain.tag.repository;

import com.algotrail.backend.domain.tag.entity.ProgrammersProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgrammersProblemTagRepository extends JpaRepository<ProgrammersProblemTag, Long> {
    Optional<ProgrammersProblemTag> findByProblemTitle(String problemTitle);
}