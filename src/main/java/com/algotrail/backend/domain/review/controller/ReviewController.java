package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.ReviewCompleteResponse;
import com.algotrail.backend.domain.review.dto.ReviewTodayResponse;
import com.algotrail.backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.algotrail.backend.domain.review.dto.ReviewRetryRequest;
import com.algotrail.backend.domain.review.dto.ReviewRetryResponse;

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
}