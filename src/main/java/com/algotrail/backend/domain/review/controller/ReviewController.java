package com.algotrail.backend.domain.review.controller;

import com.algotrail.backend.domain.review.dto.ReviewCompleteResponse;
import com.algotrail.backend.domain.review.dto.ReviewTodayResponse;
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
}