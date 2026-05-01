package com.algotrail.backend.domain.statistics.service;

import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import com.algotrail.backend.domain.statistics.dto.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;

    public StatisticsResponse getStatistics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        long totalSolvedCount = solvedProblemRepository.countByUserId(userId);
        long todaySolvedCount = solvedProblemRepository.countByUserIdAndSolvedDate(userId, today);
        long reviewCompletedCount = reviewScheduleRepository.countBySolvedProblemUserIdAndStatus(
                userId,
                "COMPLETED"
        );

        List<SolvedProblem> allSolvedProblems = solvedProblemRepository.findByUserId(userId);

        List<StatisticsResponse.LevelStat> levelStats = allSolvedProblems.stream()
                .collect(Collectors.groupingBy(
                        solvedProblem -> solvedProblem.getProblem().getLevel() == null
                                ? "Lv.미정"
                                : solvedProblem.getProblem().getLevel(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new StatisticsResponse.LevelStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(StatisticsResponse.LevelStat::level))
                .toList();

        List<SolvedProblem> weeklySolvedProblems =
                solvedProblemRepository.findByUserIdAndSolvedDateBetween(userId, startDate, today);

        Map<LocalDate, Long> weeklyCountMap = weeklySolvedProblems.stream()
                .collect(Collectors.groupingBy(
                        SolvedProblem::getSolvedDate,
                        Collectors.counting()
                ));

        List<StatisticsResponse.DailyStat> weeklyStats = startDate.datesUntil(today.plusDays(1))
                .map(date -> new StatisticsResponse.DailyStat(
                        date.toString(),
                        weeklyCountMap.getOrDefault(date, 0L)
                ))
                .toList();

        return new StatisticsResponse(
                totalSolvedCount,
                todaySolvedCount,
                reviewCompletedCount,
                levelStats,
                weeklyStats
        );
    }
}