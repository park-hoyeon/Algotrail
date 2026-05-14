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
import com.algotrail.backend.domain.tag.dto.TagResolveResult;
import com.algotrail.backend.domain.tag.service.ProblemTagResolver;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
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

    private static final String DEFAULT_CATEGORY_NAME = "미분류";

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
    private final GithubSyncLockService githubSyncLockService;
    private final ProblemTagResolver problemTagResolver;

    @Async
    public void syncAsync(Long userId) {
        sync(userId);
    }

    @Transactional
    public GithubSyncResponse sync(Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();

        System.out.println("[GitHub Sync] 동기화 요청 userId=" + userId);

        boolean locked = githubSyncLockService.tryLock(userId);

        System.out.println("[GitHub Sync] lock result=" + locked);

        if (!locked) {
            return new GithubSyncResponse(
                    userId,
                    0,
                    0,
                    startedAt,
                    LocalDateTime.now(),
                    "SKIPPED",
                    "이미 GitHub 동기화가 진행 중입니다."
            );
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            GithubRepository connectedRepository = githubRepositoryRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."));

            String safeRootPath = normalizeRootPath(connectedRepository.getRootPath());

            List<SolvedProblem> newlySavedSolvedProblems = new ArrayList<>();

            SyncResult totalResult = syncRootDirectory(
                    user,
                    connectedRepository,
                    safeRootPath,
                    newlySavedSolvedProblems
            );

            int newSolvedCount = totalResult.addedCount();
            int skippedCount = totalResult.skippedCount();

            if (!newlySavedSolvedProblems.isEmpty()) {
                reviewScheduleService.createReviewSchedulesForProblems(newlySavedSolvedProblems);
            }

            LocalDateTime finishedAt = LocalDateTime.now();

            String message = "새로운 풀이 " + newSolvedCount
                    + "개를 동기화했습니다. 건너뛴 풀이 "
                    + skippedCount
                    + "개입니다.";

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

        } finally {
            System.out.println("[GitHub Sync] lock 해제 userId=" + userId);
            githubSyncLockService.unlock(userId);
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
            if (content == null || content.path() == null) {
                continue;
            }

            if (shouldSkipPath(content.path())) {
                continue;
            }

            if ("dir".equals(content.type())) {
                SyncResult result = syncGenericDirectory(
                        user,
                        connectedRepository,
                        content.path(),
                        newlySavedSolvedProblems
                );

                totalResult = totalResult.plus(result);
                continue;
            }

            if ("file".equals(content.type()) && isAlgorithmSolutionFile(content.path())) {
                SyncResult result = syncCodeFile(
                        user,
                        connectedRepository,
                        content,
                        content.path(),
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
        if (shouldSkipPath(directoryPath)) {
            return SyncResult.none();
        }

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
                .filter(file -> file != null && "file".equals(file.type()))
                .filter(file -> isAlgorithmSolutionFile(file.path()))
                .toList();

        SyncResult totalResult = SyncResult.none();

        if (!codeFiles.isEmpty()) {
            GithubContentResponse codeFile = codeFiles.get(0);

            SyncResult result = syncCodeFile(
                    user,
                    connectedRepository,
                    codeFile,
                    codeFile.path(),
                    newlySavedSolvedProblems
            );

            totalResult = totalResult.plus(result);
        }

        for (GithubContentResponse content : contents) {
            if (content == null || content.path() == null) {
                continue;
            }

            if (shouldSkipPath(content.path())) {
                continue;
            }

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
        String problemDirectoryName = parseProblemDirectoryName(path);
        Long problemNumber = parseProblemNumber(problemDirectoryName);

        if (problemNumber == null) {
            System.out.println("[Sync Skip] 문제번호 파싱 실패: " + path);
            return SyncResult.skipped();
        }

        String normalizedTitle = normalizeProblemTitle(parseProblemTitle(problemDirectoryName));

        String platform = inferPlatformFromPath(path);
        String level = inferLevelFromPath(path);
        String language = detectLanguage(codeFile.name());

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

        LocalDate solvedDate = fetchLatestCommitDateSafely(
                connectedRepository,
                codeFile.path()
        );

        SolvedProblem solvedProblem = solvedProblemRepository.save(
                new SolvedProblem(
                        user,
                        problem,
                        codeFile.html_url(),
                        language,
                        solvedDate
                )
        );

        String codeContent = fetchCodeContentFromGithubUrl(codeFile.html_url());

        TagResolveResult tagResult = problemTagResolver.resolve(
                platform,
                problemNumber,
                normalizedTitle,
                language,
                codeContent
        );

        String resolvedCategoryName = tagResult.categoryName();

        saveResolvedCategory(problem, resolvedCategoryName);

        newlySavedSolvedProblems.add(solvedProblem);

        System.out.println("[GitHub Sync] 문제 저장 완료: "
                + problemNumber + ". " + normalizedTitle
                + " / platform=" + platform
                + " / 유형=" + resolvedCategoryName
                + " / source=" + tagResult.source());

        return SyncResult.added();
    }

    private String fetchCodeContentFromGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return "";
        }

        try {
            String rawUrl = convertGithubBlobUrlToRawUrl(githubUrl);

            System.out.println("[초기 분류용 코드 조회]");
            System.out.println("htmlUrl = " + githubUrl);
            System.out.println("rawUrl = " + rawUrl);

            String content = webClient.get()
                    .uri(URI.create(rawUrl))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (content == null || content.isBlank()) {
                return "";
            }

            return content.length() > 5000
                    ? content.substring(0, 5000)
                    : content;

        } catch (Exception e) {
            System.out.println("[초기 분류용 코드 조회 실패] " + e.getMessage());
            return "";
        }
    }

    private String convertGithubBlobUrlToRawUrl(String githubUrl) {
        return githubUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replace("/blob/", "/");
    }

    private void saveResolvedCategory(Problem problem, String categoryName) {
        String safeCategoryName = normalizeCategoryName(categoryName);

        Category category = categoryRepository.findByName(safeCategoryName)
                .orElseGet(() -> {
                    System.out.println("[Category Fallback] DB에 없는 카테고리: "
                            + safeCategoryName + " -> 미분류로 저장");

                    return categoryRepository.findByName(DEFAULT_CATEGORY_NAME)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "기본 카테고리도 존재하지 않습니다: " + DEFAULT_CATEGORY_NAME
                            ));
                });

        List<ProblemCategory> existingCategories = problemCategoryRepository.findByProblem(problem);

        if (!existingCategories.isEmpty()) {
            problemCategoryRepository.deleteAll(existingCategories);
            problemCategoryRepository.flush();
        }

        problemCategoryRepository.save(new ProblemCategory(problem, category));
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return DEFAULT_CATEGORY_NAME;
        }

        return categoryName.trim();
    }

    private LocalDate fetchLatestCommitDateSafely(
            GithubRepository connectedRepository,
            String problemPath
    ) {
        try {
            return fetchLatestCommitDate(connectedRepository, problemPath);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate fetchLatestCommitDate(
            GithubRepository connectedRepository,
            String problemPath
    ) {
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

    private String normalizeRootPath(String rootPath) {
        if (rootPath == null || rootPath.isBlank() || "전체 탐색".equals(rootPath.trim())) {
            return "";
        }

        return rootPath.trim();
    }

    private String parseProblemDirectoryName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String[] tokens = path.split("/");

        if (tokens.length >= 2) {
            return tokens[tokens.length - 2];
        }

        return tokens[tokens.length - 1];
    }

    private Long parseProblemNumber(String directoryName) {
        try {
            if (directoryName == null || directoryName.isBlank()) {
                return null;
            }

            if (directoryName.contains(".")) {
                return Long.parseLong(directoryName.substring(0, directoryName.indexOf(".")).trim());
            }

            String firstToken = directoryName.split(" ")[0].trim();
            firstToken = firstToken.replaceAll("[^0-9]", "");

            if (firstToken.isBlank()) {
                return null;
            }

            return Long.parseLong(firstToken);

        } catch (Exception e) {
            return null;
        }
    }

    private String parseProblemTitle(String directoryName) {
        if (directoryName == null) {
            return "";
        }

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
                .replace('\u2005', ' ')
                .replace('\u200B', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean shouldSkipPath(String path) {
        if (path == null) return true;

        String lowerPath = path.toLowerCase();

        return lowerPath.contains("readme")
                || lowerPath.contains(".git")
                || lowerPath.contains("node_modules")
                || lowerPath.contains("build")
                || lowerPath.contains("dist")
                || lowerPath.contains(".idea")
                || lowerPath.contains(".gradle")
                || lowerPath.contains("target")
                || lowerPath.contains("out");
    }

    private boolean isAlgorithmSolutionFile(String path) {
        if (path == null) return false;

        String lowerPath = path.toLowerCase();

        if (shouldSkipPath(lowerPath)) {
            return false;
        }

        return lowerPath.endsWith(".java")
                || lowerPath.endsWith(".py")
                || lowerPath.endsWith(".js")
                || lowerPath.endsWith(".cpp")
                || lowerPath.endsWith(".c")
                || lowerPath.endsWith(".kt");
    }

    private String detectLanguage(String filename) {
        if (filename == null) return "Unknown";

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

        GithubRepository connectedRepository =
                githubRepositoryRepository.findByUserId(userId)
                        .orElse(null);

        // 기록 초기화는 연동 여부와 관계없이 가능해야 함
        if (resetRecords) {
            reviewScheduleRepository.deleteAllByUserId(userId);
            solvedProblemRepository.deleteAllByUserId(userId);
        }

        // 연동 정보가 있을 때만 삭제
        if (connectedRepository != null) {
            githubRepositoryRepository.delete(connectedRepository);
        }
    }
}