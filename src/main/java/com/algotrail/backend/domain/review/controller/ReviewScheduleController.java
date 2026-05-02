package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.ReviewTodayResponse;
import com.algotrail.backend.domain.review.service.ReviewScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewScheduleController {

    private final ReviewScheduleService reviewScheduleService;

    @PostMapping("/backfill")
    public Map<String, Object> backfillReviewSchedules(@RequestParam Long userId) {
        int createdCount = reviewScheduleService.backfillReviewSchedules(userId);

        return Map.of(
                "message", "기존 풀이 문제 복습 일정 생성 완료",
                "createdCount", createdCount
        );
    }

    @GetMapping("/completed")
    public ReviewTodayResponse getCompletedReviews(@RequestParam Long userId) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getCompletedReviews(userId)
        );
    }
}