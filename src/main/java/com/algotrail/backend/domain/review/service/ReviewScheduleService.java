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

    private static final int[] REVIEW_DAYS = {3, 7, 14, 30};
    private static final int OVERDUE_LOOKBACK_DAYS = 60;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ReviewScheduleRepository reviewScheduleRepository;
    private final SolvedProblemRepository solvedProblemRepository;

    @Transactional
    public void createReviewSchedules(SolvedProblem solvedProblem) {
        if (reviewScheduleRepository.existsBySolvedProblemId(solvedProblem.getId())) {
            return;
        }

        LocalDate baseDate = getSolvedBaseDate(solvedProblem);
        createReviewSchedulesByBaseDate(solvedProblem, baseDate);
    }

    @Transactional
    public int backfillReviewSchedules(Long userId) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdOrderBySolvedDateDesc(userId);

        return createReviewSchedulesForProblems(solvedProblems);
    }

    @Transactional
    public int backfillReviewSchedulesFromDate(Long userId, LocalDate startDate) {
        List<SolvedProblem> solvedProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateGreaterThanEqualOrderBySolvedDateDesc(
                        userId,
                        startDate
                );

        return createReviewSchedulesForProblems(solvedProblems);
    }

    @Transactional
    public ReviewRebuildResult rebuildPendingReviewSchedulesFromDate(
            Long userId,
            LocalDate startDate
    ) {
        List<ReviewSchedule> pendingSchedules =
                reviewScheduleRepository.findBySolvedProblemUserIdAndStatus(
                        userId,
                        STATUS_PENDING
                );

        int deletedCount = pendingSchedules.size();
        reviewScheduleRepository.deleteAll(pendingSchedules);

        List<SolvedProblem> targetProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateGreaterThanEqualOrderBySolvedDateDesc(
                        userId,
                        startDate
                );

        int createdProblemCount = createReviewSchedulesForProblems(targetProblems);

        return new ReviewRebuildResult(
                deletedCount,
                createdProblemCount,
                createdProblemCount * REVIEW_DAYS.length
        );
    }

    @Transactional
    public int createReviewSchedulesForProblems(List<SolvedProblem> solvedProblems) {
        int createdProblemCount = 0;

        for (SolvedProblem solvedProblem : solvedProblems) {
            if (reviewScheduleRepository.existsBySolvedProblemId(solvedProblem.getId())) {
                continue;
            }

            LocalDate baseDate = getSolvedBaseDate(solvedProblem);
            createReviewSchedulesByBaseDate(solvedProblem, baseDate);

            createdProblemCount++;
        }

        return createdProblemCount;
    }

    private LocalDate getSolvedBaseDate(SolvedProblem solvedProblem) {
        if (solvedProblem.getSolvedDate() != null) {
            return solvedProblem.getSolvedDate();
        }

        return LocalDate.now();
    }

    private void createReviewSchedulesByBaseDate(SolvedProblem solvedProblem, LocalDate baseDate) {
        for (int i = 0; i < REVIEW_DAYS.length; i++) {
            int round = i + 1;

            boolean exists = reviewScheduleRepository.existsBySolvedProblemIdAndReviewRound(
                    solvedProblem.getId(),
                    round
            );

            if (exists) {
                continue;
            }

            ReviewSchedule reviewSchedule = new ReviewSchedule(
                    solvedProblem,
                    round,
                    baseDate,
                    baseDate.plusDays(REVIEW_DAYS[i])
            );

            reviewScheduleRepository.save(reviewSchedule);
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getTodayReviews(Long userId) {
        return reviewScheduleRepository
                .findBySolvedProblemUserIdAndReviewDateLessThanEqualAndStatusOrderByReviewDateAsc(
                        userId,
                        LocalDate.now(),
                        STATUS_PENDING
                );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getUpcomingReviews(Long userId, int days) {
        LocalDate today = LocalDate.now();

        LocalDate startDate = today.minusDays(OVERDUE_LOOKBACK_DAYS);
        LocalDate endDate = today.plusDays(days);

        return reviewScheduleRepository.findUpcomingReviewsByUser(
                userId,
                STATUS_PENDING,
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getCompletedReviews(Long userId) {
        return reviewScheduleRepository.findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
                userId,
                STATUS_COMPLETED
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getProblemReviewSchedules(Long solvedProblemId) {
        return reviewScheduleRepository.findBySolvedProblemIdOrderByReviewRoundAsc(solvedProblemId);
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

    @Transactional
    public int deletePendingReviewSchedulesBeforeDate(Long userId, LocalDate startDate) {
        List<ReviewSchedule> schedules =
                reviewScheduleRepository.findBySolvedProblemUserIdAndStatusAndReviewDateBefore(
                        userId,
                        STATUS_PENDING,
                        startDate
                );

        int deletedCount = schedules.size();
        reviewScheduleRepository.deleteAll(schedules);

        return deletedCount;
    }

    public record ReviewRebuildResult(
            int deletedCount,
            int createdProblemCount,
            int createdScheduleCount
    ) {
    }
}