package com.algotrail.backend.domain.github.service;

import com.algotrail.backend.domain.github.dto.GithubContentResponse;
import com.algotrail.backend.domain.github.dto.GithubSyncResponse;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.ProblemRepository;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import com.algotrail.backend.domain.github.entity.GithubSyncStatus;
import com.algotrail.backend.domain.github.repository.GithubSyncLogRepository;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubSyncService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final CategoryRepository categoryRepository;
    private final ProblemCategoryRepository problemCategoryRepository;
    private final GithubSyncLogRepository githubSyncLogRepository;

    public GithubSyncResponse sync(Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            String url = "https://api.github.com/repos/"
                    + user.getGithubUsername()
                    + "/"
                    + user.getGithubRepo()
                    + "/contents";

            GithubContentResponse[] contents = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(GithubContentResponse[].class)
                    .block();

            if (contents == null) {
                LocalDateTime finishedAt = LocalDateTime.now();
                String message = "GitHub 저장소에서 파일을 가져오지 못했습니다.";

                githubSyncLogRepository.save(new GithubSyncLog(
                        userId,
                        startedAt,
                        finishedAt,
                        0,
                        0,
                        GithubSyncStatus.FAILED,
                        message
                ));

                return new GithubSyncResponse(
                        userId,
                        0,
                        0,
                        startedAt,
                        finishedAt,
                        "FAILED",
                        message
                );
            }

            int newSolvedCount = 0;
            int skippedCount = 0;

            for (GithubContentResponse content : contents) {
                if (!isProgrammersDirectory(content)) {
                    continue;
                }

                int beforeCount = newSolvedCount;
                newSolvedCount += syncProgrammersDirectory(user, content.path());
            }

            LocalDateTime finishedAt = LocalDateTime.now();
            String message = "새로운 풀이 " + newSolvedCount + "개를 동기화했습니다.";

            githubSyncLogRepository.save(new GithubSyncLog(
                    userId,
                    startedAt,
                    finishedAt,
                    newSolvedCount,
                    skippedCount,
                    GithubSyncStatus.SUCCESS,
                    message
            ));

            return new GithubSyncResponse(
                    userId,
                    newSolvedCount,
                    skippedCount,
                    startedAt,
                    finishedAt,
                    "SUCCESS",
                    message
            );

        } catch (Exception e) {
            LocalDateTime finishedAt = LocalDateTime.now();
            String message = "GitHub 동기화 중 오류가 발생했습니다: " + e.getMessage();

            githubSyncLogRepository.save(new GithubSyncLog(
                    userId,
                    startedAt,
                    finishedAt,
                    0,
                    0,
                    GithubSyncStatus.FAILED,
                    message
            ));

            return new GithubSyncResponse(
                    userId,
                    0,
                    0,
                    startedAt,
                    finishedAt,
                    "FAILED",
                    message
            );
        }
    }

    private int syncProgrammersDirectory(User user, String programmersPath) {

        String url = "https://api.github.com/repos/"
                + user.getGithubUsername()
                + "/"
                + user.getGithubRepo()
                + "/contents/"
                + programmersPath;

        GithubContentResponse[] levelDirs = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (levelDirs == null) {
            return 0;
        }

        int count = 0;

        for (GithubContentResponse levelDir : levelDirs) {
            if (!"dir".equals(levelDir.type())) {
                continue;
            }

            count += syncLevelDirectory(
                    user,
                    levelDir.path(),
                    levelDir.name()
            );
        }

        return count;
    }


    private int syncLevelDirectory(User user, String levelPath, String levelName) {

        String url = "https://api.github.com/repos/"
                + user.getGithubUsername()
                + "/"
                + user.getGithubRepo()
                + "/contents/"
                + levelPath;

        GithubContentResponse[] problemDirs = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (problemDirs == null) {
            return 0;
        }

        int count = 0;

        for (GithubContentResponse problemDir : problemDirs) {
            if (!"dir".equals(problemDir.type())) {
                continue;
            }

            count += syncProblemDirectory(
                    user,
                    problemDir.path(),
                    normalizeLevel(levelName),
                    parseProblemTitle(problemDir.name())
            );
        }

        return count;
    }

    private String parseProblemTitle(String directoryName) {
        if (directoryName.contains(". ")) {
            return directoryName.substring(directoryName.indexOf(". ") + 2);
        }

        return directoryName;
    }

    private int syncProblemDirectory(User user, String problemPath, String levelName, String problemTitle) {
        String url = "https://api.github.com/repos/"
                + user.getGithubUsername()
                + "/"
                + user.getGithubRepo()
                + "/contents/"
                + problemPath;

        GithubContentResponse[] files = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (files == null) {
            return 0;
        }

        List<GithubContentResponse> codeFiles = Arrays.stream(files)
                .filter(file -> "file".equals(file.type()))
                .filter(file -> isCodeFile(file.name()))
                .toList();

        if (codeFiles.isEmpty()) {
            return 0;
        }

        GithubContentResponse codeFile = codeFiles.get(0);

        Problem problem = problemRepository.findByPlatformAndTitle("PROGRAMMERS", problemTitle)
                .orElseGet(() -> problemRepository.save(
                        new Problem(
                                "PROGRAMMERS",
                                problemTitle,
                                normalizeLevel(levelName),
                                null
                        )
                ));

        saveAutoCategory(problem, problemTitle);

        if (solvedProblemRepository.existsByUserAndProblem(user, problem)) {
            return 0;
        }

        SolvedProblem solvedProblem = solvedProblemRepository.save(
                new SolvedProblem(
                        user,
                        problem,
                        codeFile.html_url(),
                        detectLanguage(codeFile.name()),
                        LocalDate.now()
                )
        );

        createReviewSchedules(solvedProblem);

        return 1;
    }

    private void createReviewSchedules(SolvedProblem solvedProblem) {
        LocalDate solvedDate = solvedProblem.getSolvedDate();

        reviewScheduleRepository.save(new ReviewSchedule(
                solvedProblem,
                1,
                solvedDate.plusDays(3)
        ));

        reviewScheduleRepository.save(new ReviewSchedule(
                solvedProblem,
                2,
                solvedDate.plusDays(7)
        ));

        reviewScheduleRepository.save(new ReviewSchedule(
                solvedProblem,
                3,
                solvedDate.plusDays(14)
        ));
    }

    private boolean isProgrammersDirectory(GithubContentResponse content) {
        return "dir".equals(content.type())
                && (
                content.name().contains("프로그래머스")
                        || content.name().toLowerCase().contains("programmers")
        );
    }

    private boolean isCodeFile(String filename) {
        return filename.endsWith(".java")
                || filename.endsWith(".py")
                || filename.endsWith(".js")
                || filename.endsWith(".cpp")
                || filename.endsWith(".c")
                || filename.endsWith(".kt");
    }

    private String detectLanguage(String filename) {
        if (filename.endsWith(".java")) return "Java";
        if (filename.endsWith(".py")) return "Python";
        if (filename.endsWith(".js")) return "JavaScript";
        if (filename.endsWith(".cpp")) return "C++";
        if (filename.endsWith(".c")) return "C";
        if (filename.endsWith(".kt")) return "Kotlin";
        return "Unknown";
    }

    private String normalizeLevel(String levelName) {
        if (levelName.equals("0")) return "Lv.0";
        if (levelName.equals("1")) return "Lv.1";
        if (levelName.equals("2")) return "Lv.2";
        if (levelName.equals("3")) return "Lv.3";
        if (levelName.equals("4")) return "Lv.4";
        if (levelName.equals("5")) return "Lv.5";

        String lower = levelName.toLowerCase();

        if (lower.contains("lv.0") || lower.contains("level0")) return "Lv.0";
        if (lower.contains("lv.1") || lower.contains("level1")) return "Lv.1";
        if (lower.contains("lv.2") || lower.contains("level2")) return "Lv.2";
        if (lower.contains("lv.3") || lower.contains("level3")) return "Lv.3";
        if (lower.contains("lv.4") || lower.contains("level4")) return "Lv.4";
        if (lower.contains("lv.5") || lower.contains("level5")) return "Lv.5";

        return levelName;
    }

    private void saveAutoCategory(Problem problem, String problemTitle) {
        String categoryName = inferCategoryName(problemTitle);

        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryName));

        boolean alreadyExists = problemCategoryRepository.findByProblem(problem)
                .stream()
                .anyMatch(problemCategory ->
                        problemCategory.getCategory().getId().equals(category.getId())
                );

        if (!alreadyExists) {
            problemCategoryRepository.save(new ProblemCategory(problem, category));
        }
    }

    private String inferCategoryName(String problemTitle) {
        String title = problemTitle.toLowerCase();

        if (title.contains("dfs") || title.contains("bfs") || title.contains("네트워크") || title.contains("타겟 넘버")) {
            return "BFS/DFS";
        }

        if (title.contains("배달") || title.contains("최단") || title.contains("다익스트라")) {
            return "최단경로";
        }

        if (title.contains("해시") || title.contains("완주하지 못한 선수") || title.contains("전화번호")) {
            return "해시";
        }

        if (title.contains("스택") || title.contains("큐") || title.contains("기능개발") || title.contains("올바른 괄호")) {
            return "스택/큐";
        }

        if (title.contains("정렬") || title.contains("k번째수") || title.contains("가장 큰 수")) {
            return "정렬";
        }

        if (title.contains("dp") || title.contains("타일") || title.contains("정수 삼각형")) {
            return "DP";
        }

        if (title.contains("그리디") || title.contains("체육복") || title.contains("구명보트")) {
            return "그리디";
        }

        if (title.contains("카펫") || title.contains("모의고사") || title.contains("소수 찾기")) {
            return "완전탐색";
        }

        if (title.contains("문자열") || title.contains("문자") || title.contains("이상한 문자")) {
            return "문자열";
        }

        return "구현";
    }
}