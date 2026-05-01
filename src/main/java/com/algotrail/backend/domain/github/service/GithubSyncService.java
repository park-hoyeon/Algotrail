package com.algotrail.backend.domain.github.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.github.dto.*;
import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import com.algotrail.backend.domain.github.entity.GithubSyncStatus;
import com.algotrail.backend.domain.github.repository.GithubSyncLogRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.ProblemRepository;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.entity.ReviewSchedule;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

            SyncResult totalResult = SyncResult.none();

            for (GithubContentResponse content : contents) {
                if (!isProgrammersDirectory(content)) {
                    continue;
                }

                SyncResult result = syncProgrammersDirectory(user, content.path());
                totalResult = totalResult.plus(result);
            }

            int newSolvedCount = totalResult.addedCount();
            int skippedCount = totalResult.skippedCount();

            LocalDateTime finishedAt = LocalDateTime.now();

            String message = "새로운 풀이 " + newSolvedCount
                    + "개를 동기화했습니다. 기존 풀이 "
                    + skippedCount
                    + "개는 건너뛰었습니다.";

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

    private SyncResult syncProgrammersDirectory(User user, String programmersPath) {
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
            return SyncResult.none();
        }

        SyncResult totalResult = SyncResult.none();

        for (GithubContentResponse levelDir : levelDirs) {
            if (!"dir".equals(levelDir.type())) {
                continue;
            }

            SyncResult result = syncLevelDirectory(
                    user,
                    levelDir.path(),
                    levelDir.name()
            );

            totalResult = totalResult.plus(result);
        }

        return totalResult;
    }

    private SyncResult syncLevelDirectory(User user, String levelPath, String levelName) {
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
            return SyncResult.none();
        }

        SyncResult totalResult = SyncResult.none();

        for (GithubContentResponse problemDir : problemDirs) {
            if (!"dir".equals(problemDir.type())) {
                continue;
            }

            SyncResult result = syncProblemDirectory(
                    user,
                    problemDir.path(),
                    normalizeLevel(levelName),
                    parseProblemTitle(problemDir.name()),
                    parseProblemNumber(problemDir.name())
            );

            totalResult = totalResult.plus(result);
        }

        return totalResult;
    }

    private Long parseProblemNumber(String directoryName) {
        try {
            if (directoryName.contains(".")) {
                return Long.parseLong(directoryName.substring(0, directoryName.indexOf(".")).trim());
            }

            String firstToken = directoryName.split(" ")[0].trim();
            return Long.parseLong(firstToken);

        } catch (Exception e) {
            return null;
        }
    }

    private SyncResult syncProblemDirectory(User user, String problemPath, String levelName, String problemTitle, Long problemNumber) {
        String url = "https://api.github.com/repos/"
                + user.getGithubUsername()
                + "/"
                + user.getGithubRepo()
                + "/contents/"
                + problemPath;

        System.out.println("problemNumber = " + problemNumber + ", title = " + problemTitle);

        if (problemNumber == null) {
            System.out.println("❌ 문제번호 파싱 실패: " + problemTitle);
            return SyncResult.skipped();
        }

        GithubContentResponse[] files = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (files == null) {
            return SyncResult.none();
        }

        if (problemNumber == null) {
            System.out.println("문제 번호 파싱 실패: " + problemTitle);
            return SyncResult.skipped();
        }

        List<GithubContentResponse> codeFiles = Arrays.stream(files)
                .filter(file -> "file".equals(file.type()))
                .filter(file -> isCodeFile(file.name()))
                .toList();

        if (codeFiles.isEmpty()) {
            return SyncResult.none();
        }

        GithubContentResponse codeFile = codeFiles.get(0);

        String normalizedTitle = normalizeProblemTitle(problemTitle);

        Problem problem = problemRepository.findByPlatformAndProblemNumber("PROGRAMMERS", problemNumber)
                .orElseGet(() -> problemRepository.save(
                        new Problem(
                                "PROGRAMMERS",
                                problemNumber,
                                normalizedTitle,
                                normalizeLevel(levelName),
                                null
                        )
                ));

        saveAutoCategory(problem, normalizedTitle);


        if (solvedProblemRepository.existsByUserAndProblem(user, problem)) {
            return SyncResult.skipped();
        }

        LocalDate solvedDate = fetchLatestCommitDate(user, problemPath);

        SolvedProblem solvedProblem = solvedProblemRepository.save(
                new SolvedProblem(
                        user,
                        problem,
                        codeFile.html_url(),
                        detectLanguage(codeFile.name()),
                        solvedDate
                )
        );

        createFirstReviewSchedule(solvedProblem);

        return SyncResult.added();
    }

    private void createFirstReviewSchedule(SolvedProblem solvedProblem) {
        LocalDate solvedDate = solvedProblem.getSolvedDate();

        reviewScheduleRepository.save(new ReviewSchedule(
                solvedProblem,
                1,
                solvedDate.plusDays(3)
        ));
    }

    private LocalDate fetchLatestCommitDate(User user, String problemPath) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://api.github.com/repos/"
                            + user.getGithubUsername()
                            + "/"
                            + user.getGithubRepo()
                            + "/commits")
                    .queryParam("path", problemPath)
                    .queryParam("per_page", 1)
                    .build()
                    .encode()
                    .toUri();

            System.out.println("commit uri = " + uri);

            GithubCommitResponse[] commits = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(GithubCommitResponse[].class)
                    .block();

            if (commits == null || commits.length == 0) {
                throw new IllegalStateException("커밋 내역이 없습니다. path=" + problemPath);
            }

            if (commits[0].commit() == null
                    || commits[0].commit().author() == null
                    || commits[0].commit().author().date() == null) {
                throw new IllegalStateException("커밋 날짜 정보가 없습니다. path=" + problemPath);
            }

            return commits[0]
                    .commit()
                    .author()
                    .date()
                    .toLocalDate();

        } catch (Exception e) {
            throw new IllegalStateException("GitHub 커밋 날짜 조회 실패: " + problemPath + " / " + e.getMessage(), e);
        }
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

    private String parseProblemTitle(String directoryName) {
        if (directoryName.contains(". ")) {
            return directoryName.substring(directoryName.indexOf(". ") + 2);
        }

        return directoryName;
    }

    private String normalizeProblemTitle(String title) {
        if (title == null) {
            return "";
        }

        return title
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
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

    public GithubSettingResponse getGithubSetting(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return new GithubSettingResponse(
                user.getGithubUsername() != null && user.getGithubRepo() != null,
                user.getGithubUsername(),
                user.getGithubRepo(),
                "-"
        );
    }

    @Transactional
    public void disconnectGithub(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.disconnectGithub();
    }


}