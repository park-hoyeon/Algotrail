package com.algotrail.backend.domain.github.service;

import com.algotrail.backend.domain.category.entity.Category;
import com.algotrail.backend.domain.category.repository.CategoryRepository;
import com.algotrail.backend.domain.github.dto.*;
import com.algotrail.backend.domain.github.entity.GithubRepository;
import com.algotrail.backend.domain.github.entity.GithubSyncLog;
import com.algotrail.backend.domain.github.entity.GithubSyncStatus;
import com.algotrail.backend.domain.github.repository.GithubRepositoryRepository;
import com.algotrail.backend.domain.github.repository.GithubSyncLogRepository;
import com.algotrail.backend.domain.problem.entity.Problem;
import com.algotrail.backend.domain.problem.entity.ProblemCategory;
import com.algotrail.backend.domain.problem.entity.SolvedProblem;
import com.algotrail.backend.domain.problem.repository.ProblemCategoryRepository;
import com.algotrail.backend.domain.problem.repository.ProblemRepository;
import com.algotrail.backend.domain.problem.repository.SolvedProblemRepository;
import com.algotrail.backend.domain.review.repository.ReviewScheduleRepository;
import com.algotrail.backend.domain.review.service.ReviewScheduleService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubSyncService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final ReviewScheduleService reviewScheduleService;
    private final CategoryRepository categoryRepository;
    private final ProblemCategoryRepository problemCategoryRepository;
    private final GithubSyncLogRepository githubSyncLogRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;

    @Transactional
    public GithubSyncResponse sync(Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            GithubRepository connectedRepository = githubRepositoryRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."));

            String rootPath = connectedRepository.getRootPath();

            List<SolvedProblem> newlySavedSolvedProblems = new ArrayList<>();

            SyncResult totalResult = syncRootDirectory(
                    user,
                    connectedRepository,
                    rootPath == null ? "" : rootPath,
                    newlySavedSolvedProblems
            );

            int newSolvedCount = totalResult.addedCount();
            int skippedCount = totalResult.skippedCount();

            reviewScheduleService.createReviewSchedulesForProblems(newlySavedSolvedProblems);

            LocalDateTime finishedAt = LocalDateTime.now();

            String message = "새로운 풀이 " + newSolvedCount
                    + "개를 동기화했습니다. 기존 풀이 "
                    + skippedCount
                    + "개는 건너뛰었습니다.";

            connectedRepository.updateLastSyncedAt();

            saveSyncLog(
                    userId,
                    startedAt,
                    finishedAt,
                    newSolvedCount,
                    skippedCount,
                    GithubSyncStatus.SUCCESS,
                    message
            );

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

            saveSyncLog(userId, startedAt, finishedAt, 0, 0, GithubSyncStatus.FAILED, message);

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

    private SyncResult syncRootDirectory(
            User user,
            GithubRepository connectedRepository,
            String rootPath,
            List<SolvedProblem> newlySavedSolvedProblems
    ) {
        String url = buildContentsUrl(
                connectedRepository.getGithubUsername(),
                connectedRepository.getRepositoryName(),
                rootPath
        );

        GithubContentResponse[] contents = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (contents == null) {
            return SyncResult.none();
        }

        SyncResult totalResult = SyncResult.none();

        for (GithubContentResponse content : contents) {
            if ("dir".equals(content.type())) {
                SyncResult result = syncGenericDirectory(
                        user,
                        connectedRepository,
                        content.path(),
                        newlySavedSolvedProblems
                );

                totalResult = totalResult.plus(result);
            }

            if ("file".equals(content.type()) && isCodeFile(content.name())) {
                SyncResult result = syncCodeFile(
                        user,
                        connectedRepository,
                        content,
                        rootPath,
                        newlySavedSolvedProblems
                );

                totalResult = totalResult.plus(result);
            }
        }

        return totalResult;
    }

    private SyncResult syncGenericDirectory(
            User user,
            GithubRepository connectedRepository,
            String directoryPath,
            List<SolvedProblem> newlySavedSolvedProblems
    ) {
        String url = buildContentsUrl(
                connectedRepository.getGithubUsername(),
                connectedRepository.getRepositoryName(),
                directoryPath
        );

        GithubContentResponse[] contents = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GithubContentResponse[].class)
                .block();

        if (contents == null) {
            return SyncResult.none();
        }

        List<GithubContentResponse> codeFiles = Arrays.stream(contents)
                .filter(file -> "file".equals(file.type()))
                .filter(file -> isCodeFile(file.name()))
                .toList();

        SyncResult totalResult = SyncResult.none();

        if (!codeFiles.isEmpty()) {
            GithubContentResponse codeFile = codeFiles.get(0);

            SyncResult result = syncCodeFile(
                    user,
                    connectedRepository,
                    codeFile,
                    directoryPath,
                    newlySavedSolvedProblems
            );

            totalResult = totalResult.plus(result);
        }

        for (GithubContentResponse content : contents) {
            if ("dir".equals(content.type())) {
                SyncResult result = syncGenericDirectory(
                        user,
                        connectedRepository,
                        content.path(),
                        newlySavedSolvedProblems
                );

                totalResult = totalResult.plus(result);
            }
        }

        return totalResult;
    }

    private SyncResult syncCodeFile(
            User user,
            GithubRepository connectedRepository,
            GithubContentResponse codeFile,
            String path,
            List<SolvedProblem> newlySavedSolvedProblems
    ) {
        String problemTitle = parseTitleFromPath(path);
        Long problemNumber = parseProblemNumber(problemTitle);

        if (problemNumber == null) {
            System.out.println("❌ 문제번호 파싱 실패: " + problemTitle);
            return SyncResult.skipped();
        }

        String normalizedTitle = normalizeProblemTitle(parseProblemTitle(problemTitle));

        String platform = inferPlatformFromPath(path);
        String level = inferLevelFromPath(path);

        if (solvedProblemRepository.existsByUserIdAndProblemPlatformAndProblemProblemNumber(
                user.getId(),
                platform,
                problemNumber
        )) {
            return SyncResult.skipped();
        }

        Problem problem = problemRepository.findByPlatformAndProblemNumber(platform, problemNumber)
                .orElseGet(() -> problemRepository.save(
                        new Problem(
                                platform,
                                problemNumber,
                                normalizedTitle,
                                level,
                                null
                        )
                ));

        saveAutoCategory(problem, normalizedTitle);

        LocalDate solvedDate = fetchLatestCommitDate(
                connectedRepository,
                codeFile.path()
        );

        SolvedProblem solvedProblem = solvedProblemRepository.save(
                new SolvedProblem(
                        user,
                        problem,
                        codeFile.html_url(),
                        detectLanguage(codeFile.name()),
                        solvedDate
                )
        );

        newlySavedSolvedProblems.add(solvedProblem);

        return SyncResult.added();
    }

    private LocalDate fetchLatestCommitDate(
            GithubRepository connectedRepository,
            String problemPath
    ) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://api.github.com/repos/"
                            + connectedRepository.getGithubUsername()
                            + "/"
                            + connectedRepository.getRepositoryName()
                            + "/commits")
                    .queryParam("path", problemPath)
                    .queryParam("per_page", 1)
                    .build()
                    .encode()
                    .toUri();

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

    private String buildContentsUrl(String githubUsername, String repositoryName, String path) {
        String baseUrl = "https://api.github.com/repos/"
                + githubUsername
                + "/"
                + repositoryName
                + "/contents";

        if (path == null || path.isBlank()) {
            return baseUrl;
        }

        return baseUrl + "/" + path;
    }

    private void saveSyncLog(
            Long userId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int newSolvedCount,
            int skippedCount,
            GithubSyncStatus status,
            String message
    ) {
        githubSyncLogRepository.save(new GithubSyncLog(
                userId,
                startedAt,
                finishedAt,
                newSolvedCount,
                skippedCount,
                status,
                message
        ));
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

    private String parseProblemTitle(String directoryName) {
        if (directoryName.contains(". ")) {
            return directoryName.substring(directoryName.indexOf(". ") + 2);
        }

        return directoryName;
    }

    private String parseTitleFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String[] tokens = path.split("/");

        if (tokens.length == 0) {
            return path;
        }

        return tokens[tokens.length - 1];
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

    private String inferPlatformFromPath(String path) {
        if (path == null) {
            return "CUSTOM";
        }

        String lowerPath = path.toLowerCase();

        if (path.contains("프로그래머스") || lowerPath.contains("programmers")) {
            return "PROGRAMMERS";
        }

        if (path.contains("백준") || lowerPath.contains("baekjoon") || lowerPath.contains("boj")) {
            return "BAEKJOON";
        }

        if (lowerPath.contains("leetcode")) {
            return "LEETCODE";
        }

        if (lowerPath.matches(".*(^|/)\\d{1,5}[-_].*")) {
            return "LEETCODE";
        }

        return "CUSTOM";
    }

    private String inferLevelFromPath(String path) {
        if (path == null) {
            return "미분류";
        }

        String lowerPath = path.toLowerCase();

        if (path.contains("프로그래머스") || lowerPath.contains("programmers")) {
            if (lowerPath.contains("lv.0") || lowerPath.contains("level0") || lowerPath.contains("/0/")) return "Lv.0";
            if (lowerPath.contains("lv.1") || lowerPath.contains("level1") || lowerPath.contains("/1/")) return "Lv.1";
            if (lowerPath.contains("lv.2") || lowerPath.contains("level2") || lowerPath.contains("/2/")) return "Lv.2";
            if (lowerPath.contains("lv.3") || lowerPath.contains("level3") || lowerPath.contains("/3/")) return "Lv.3";
            if (lowerPath.contains("lv.4") || lowerPath.contains("level4") || lowerPath.contains("/4/")) return "Lv.4";
            if (lowerPath.contains("lv.5") || lowerPath.contains("level5") || lowerPath.contains("/5/")) return "Lv.5";

            return "Lv.미정";
        }

        if (path.contains("백준") || lowerPath.contains("baekjoon") || lowerPath.contains("boj")) {
            if (lowerPath.contains("bronze")) return "Bronze";
            if (lowerPath.contains("silver")) return "Silver";
            if (lowerPath.contains("gold")) return "Gold";
            if (lowerPath.contains("platinum")) return "Platinum";
            if (lowerPath.contains("diamond")) return "Diamond";
            if (lowerPath.contains("ruby")) return "Ruby";

            return "등급 미정";
        }

        if (lowerPath.contains("leetcode") || lowerPath.matches(".*(^|/)\\d{1,5}[-_].*")) {
            return "LeetCode";
        }

        return "미분류";
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

    @Transactional(readOnly = true)
    public GithubSettingResponse getGithubSetting(Long userId) {
        GithubRepository connectedRepository = githubRepositoryRepository.findByUserId(userId)
                .orElse(null);

        if (connectedRepository == null) {
            return new GithubSettingResponse(
                    false,
                    null,
                    null,
                    "-"
            );
        }

        return new GithubSettingResponse(
                connectedRepository.isConnected(),
                connectedRepository.getGithubUsername(),
                connectedRepository.getRepositoryName(),
                connectedRepository.getLastSyncedAt() == null
                        ? "-"
                        : connectedRepository.getLastSyncedAt().toString()
        );
    }

    @Transactional
    public void disconnectGithub(Long userId, boolean resetRecords) {
        GithubRepository connectedRepository = githubRepositoryRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."));

        if (resetRecords) {
            reviewScheduleRepository.deleteAllByUserId(userId);
            solvedProblemRepository.deleteAllByUserId(userId);
        }

        githubRepositoryRepository.delete(connectedRepository);
    }
}