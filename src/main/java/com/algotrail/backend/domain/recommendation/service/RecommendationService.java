package com.algotrail.backend.domain.recommendation.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.recommendation.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CategoryRepository categoryRepository;
    private final ProblemCategoryRepository problemCategoryRepository;

    public RecommendationResponse getTodayRecommendation(Long userId) {
        Map<Long, Long> solvedCountMap = problemCategoryRepository.countSolvedByCategory(userId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Category recommendedCategory = categoryRepository.findAll()
                .stream()
                .min(Comparator.comparing(category ->
                        solvedCountMap.getOrDefault(category.getId(), 0L)
                ))
                .orElseThrow(() -> new IllegalStateException("카테고리가 존재하지 않습니다."));

        long solvedCount = solvedCountMap.getOrDefault(recommendedCategory.getId(), 0L);

        return new RecommendationResponse(
                recommendedCategory.getId(),
                recommendedCategory.getName(),
                solvedCount,
                "오늘은 '" + recommendedCategory.getName()
                        + "' 유형을 풀어보는 것을 추천합니다. 현재 풀이 수는 "
                        + solvedCount + "개입니다."
        );
    }
}