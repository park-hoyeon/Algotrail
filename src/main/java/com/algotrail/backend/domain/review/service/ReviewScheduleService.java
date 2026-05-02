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

    @Transactional
    public void createReviewSchedules(SolvedProblem solvedProblem) {
        LocalDate solvedDate = solvedProblem.getSolvedDate();

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
                        solvedProblem.getSolvedDate().plusDays(REVIEW_DAYS[i])
                );

                reviewScheduleRepository.save(reviewSchedule);
                createdCount++;
            }
        }

        return createdCount;
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> getCompletedReviews(Long userId) {
        return reviewScheduleRepository
                .findBySolvedProblemUserIdAndStatusOrderByCompletedAtDesc(
                        userId,
                        "COMPLETED"
                );
    }
}