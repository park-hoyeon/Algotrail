package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.*;
import com.algotrail.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/today")
    public ReviewTodayResponse getTodayReviews(@RequestParam Long userId) {
        return reviewService.getTodayReviews(userId);
    }

    @PatchMapping("/{reviewScheduleId}/complete")
    public ReviewCompleteResponse completeReview(@PathVariable Long reviewScheduleId) {
        return reviewService.completeReview(reviewScheduleId);
    }

    @PatchMapping("/{reviewScheduleId}/retry")
    public ReviewRetryResponse retryReview(
            @PathVariable Long reviewScheduleId,
            @RequestBody ReviewRetryRequest request
    ) {
        return reviewService.retryReview(reviewScheduleId, request);
    }

    @GetMapping("/upcoming")
    public UpcomingReviewResponse getUpcomingReviews(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return reviewService.getUpcomingReviews(userId, days);
    }
}