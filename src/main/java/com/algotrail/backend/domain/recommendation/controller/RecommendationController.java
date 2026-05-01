package com.algotrail.backend.domain.recommendation.controller;

import com.algotrail.backend.domain.recommendation.dto.RecommendationResponse;
import com.algotrail.backend.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/today")
    public RecommendationResponse getTodayRecommendation(@RequestParam Long userId) {
        return recommendationService.getTodayRecommendation(userId);
    }
}