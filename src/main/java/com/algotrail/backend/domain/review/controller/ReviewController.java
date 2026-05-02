package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.*;
import com.algotrail.backend.domain.review.service.ReviewScheduleService;
import com.algotrail.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewScheduleService reviewScheduleService;

    @GetMapping("/today")
    public ReviewTodayResponse getTodayReviews(@RequestParam Long userId) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getTodayReviews(userId)
        );
    }

    @GetMapping("/upcoming")
    public ReviewTodayResponse getUpcomingReviews(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getUpcomingReviews(userId, days)
        );
    }

    @GetMapping("/completed")
    public ReviewTodayResponse getCompletedReviews(@RequestParam Long userId) {
        return ReviewTodayResponse.of(
                LocalDate.now(),
                reviewScheduleService.getCompletedReviews(userId)
        );
    }

    @PatchMapping("/{reviewScheduleId}/complete")
    public void completeReview(@PathVariable Long reviewScheduleId) {
        reviewService.completeReview(reviewScheduleId);
    }

    @PatchMapping("/{reviewScheduleId}/retry")
    public ReviewRetryResponse retryReview(
            @PathVariable Long reviewScheduleId,
            @RequestBody ReviewRetryRequest request
    ) {
        return reviewService.retryReview(reviewScheduleId, request);
    }

    @GetMapping("/problem/{solvedProblemId}")
    public List<ProblemReviewScheduleResponse> getProblemReviewSchedules(
            @PathVariable Long solvedProblemId
    ) {
        return reviewService.getProblemReviewSchedules(solvedProblemId);
    }
}