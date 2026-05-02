package com.algotrail.backend.domain.review.service;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewScheduleService {

    private final ReviewScheduleRepository reviewScheduleRepository;
    private final SolvedProblemRepository solvedProblemRepository;

    private static final int[] REVIEW_DAYS = {3, 7, 14, 30};

    // 이 날짜 이후에 푼 문제만 복습 일정 생성
    private static final LocalDate REVIEW_START_DATE = LocalDate.of(2026, 4, 28);

    @Transactional
    public void createReviewSchedules(SolvedProblem solvedProblem) {
        LocalDate solvedDate = solvedProblem.getSolvedDate();

        if (solvedDate.isBefore(REVIEW_START_DATE)) {
            return;
        }

        for (int i = 0; i < REVIEW_DAYS.length; i++) {
            int round = i + 1;

            boolean exists = reviewScheduleRepository
                    .existsBySolvedProblemIdAndReviewRound(
                            solvedProblem.getId(),
                            round
                    );

            if (exists) continue;

            ReviewSchedule review = new ReviewSchedule(
                    solvedProblem,
                    round,
                    solvedDate.plusDays(REVIEW_DAYS[i])
            );

            reviewScheduleRepository.save(review);
        }
    }

    @Transactional
    public int backfillReviewSchedules(Long userId) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId);

        int createdCount = 0;

        for (SolvedProblem solvedProblem : solvedProblems) {
            LocalDate solvedDate = solvedProblem.getSolvedDate();

            if (solvedDate.isBefore(REVIEW_START_DATE)) {
                continue;
            }

            for (int i = 0; i < REVIEW_DAYS.length; i++) {
                int round = i + 1;

                boolean exists = reviewScheduleRepository
                        .existsBySolvedProblemIdAndReviewRound(
                                solvedProblem.getId(),
                                round
                        );

                if (exists) continue;

                ReviewSchedule reviewSchedule = new ReviewSchedule(
                        solvedProblem,
                        round,
                        solvedDate.plusDays(REVIEW_DAYS[i])
                );

                reviewScheduleRepository.save(reviewSchedule);
                createdCount++;
            }
        }

        return createdCount;
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getTodayReviews(Long userId) {
        return reviewScheduleRepository
                .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        LocalDate.now(),
                        "PENDING"
                );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getUpcomingReviews(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        return reviewScheduleRepository.findUpcomingReviewsByUser(
                userId,
                "PENDING",
                today,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getCompletedReviews(Long userId) {
        return reviewScheduleRepository
                .findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
                        userId,
                        "COMPLETED"
                );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getProblemReviewSchedules(Long solvedProblemId) {
        return reviewScheduleRepository
                .findBySolvedProblemIdOrderByReviewRoundAsc(solvedProblemId);
    }

    @Transactional
    public void completeReview(Long reviewScheduleId) {
        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복습 일정을 찾을 수 없습니다."));

        reviewSchedule.complete();
    }

    @Transactional
    public void retryReview(Long reviewScheduleId) {
        ReviewSchedule reviewSchedule = reviewScheduleRepository.findById(reviewScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복습 일정을 찾을 수 없습니다."));

        reviewSchedule.retry();
    }
}