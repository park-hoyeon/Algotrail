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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final TransactionTemplate transactionTemplate;

    @Async
    public void syncAsync(Long userId) {
        sync(userId);
    }

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
            GithubRepository connectedRepository = transactionTemplate.execute(status ->
                    githubRepositoryRepository.findByUserId(userId)
                            .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."))
            );

            if (connectedRepository == null) {
                throw new IllegalArgumentException("연동된 GitHub 저장소가 없습니다.");
            }

            String safeRootPath = normalizeRootPath(connectedRepository.getRootPath());

            SyncResult totalResult = syncRootDirectory(
                    userId,
                    connectedRepository,
                    safeRootPath
            );

            int newSolvedCount = totalResult.addedCount();
            int skippedCount = totalResult.skippedCount();

            LocalDateTime finishedAt = LocalDateTime.now();

            String message = "새로운 풀이 " + newSolvedCount
                    + "개를 동기화했습니다. 건너뛴 풀이 "
                    + skippedCount
                    + "개입니다.";

            transactionTemplate.executeWithoutResult(status -> {
                GithubRepository repository = githubRepositoryRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."));

                repository.updateLastSyncedAt();
                githubRepositoryRepository.save(repository);

                saveSyncLog(
                        userId,
                        startedAt,
                        finishedAt,
                        newSolvedCount,
                        skippedCount,
                        GithubSyncStatus.SUCCESS,
                        message
                );
            });

            System.out.println("[GitHub Sync] 완료 userId=" + userId + " / " + message);

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
            e.printStackTrace();

            LocalDateTime finishedAt = LocalDateTime.now();
            String message = "GitHub 동기화 중 오류가 발생했습니다: " + e.getMessage();

            transactionTemplate.executeWithoutResult(status ->
                    saveSyncLog(userId, startedAt, finishedAt, 0, 0, GithubSyncStatus.FAILED, message)
            );

            System.out.println("[GitHub Sync] 실패 userId=" + userId + " / " + message);

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
            Long userId,
            GithubRepository connectedRepository,
            String rootPath
    ) {
        String url = buildContentsUrl(
                connectedRepository.getGithubUsername(),
                connectedRepository.getRepositoryName(),
                rootPath
        );

        System.out.println("[GitHub Sync] root contents url=" + url);

        GithubContentResponse[] contents = fetchContentsSafely(rootPath, url);

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

            if ("file".equals(content.type()) && isAlgorithmSolutionFile(content.path())) {
                SyncResult result = syncCodeFile(
                        userId,
                        connectedRepository,
                        content,
                        content.path()
                );

                totalResult = totalResult.plus(result);
                continue;
            }

            if ("dir".equals(content.type())) {
                SyncResult result = syncGenericDirectory(
                        userId,
                        connectedRepository,
                        content.path()
                );

                totalResult = totalResult.plus(result);
            }
        }

        return totalResult;
    }

    private SyncResult syncGenericDirectory(
            Long userId,
            GithubRepository connectedRepository,
            String directoryPath
    ) {
        if (shouldSkipPath(directoryPath)) {
            return SyncResult.none();
        }

        String url = buildContentsUrl(
                connectedRepository.getGithubUsername(),
                connectedRepository.getRepositoryName(),
                directoryPath
        );

        GithubContentResponse[] contents = fetchContentsSafely(directoryPath, url);

        if (contents == null) {
            return SyncResult.skipped();
        }

        SyncResult totalResult = SyncResult.none();

        for (GithubContentResponse content : contents) {
            if (content == null || content.path() == null) {
                continue;
            }

            if (shouldSkipPath(content.path())) {
                continue;
            }

            if ("file".equals(content.type()) && isAlgorithmSolutionFile(content.path())) {
                SyncResult result = syncCodeFile(
                        userId,
                        connectedRepository,
                        content,
                        content.path()
                );

                totalResult = totalResult.plus(result);
                continue;
            }

            if ("dir".equals(content.type())) {
                SyncResult result = syncGenericDirectory(
                        userId,
                        connectedRepository,
                        content.path()
                );

                totalResult = totalResult.plus(result);
            }
        }

        return totalResult;
    }

    private GithubContentResponse[] fetchContentsSafely(String path, String url) {
        try {
            return webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(GithubContentResponse[].class)
                    .block();

        } catch (Exception e) {
            System.out.println("[GitHub Sync] 디렉토리 조회 실패, 건너뜀 path="
                    + path + " / url=" + url + " / error=" + e.getMessage());
            return null;
        }
    }

    private SyncResult syncCodeFile(
            Long userId,
            GithubRepository connectedRepository,
            GithubContentResponse codeFile,
            String path
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

        Boolean alreadyExists = transactionTemplate.execute(status ->
                solvedProblemRepository.existsByUserIdAndProblemPlatformAndProblemProblemNumber(
                        userId,
                        platform,
                        problemNumber
                )
        );

        if (Boolean.TRUE.equals(alreadyExists)) {
            return SyncResult.skipped();
        }

        LocalDate solvedDate = fetchLatestCommitDateSafely(
                connectedRepository,
                codeFile.path()
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

        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

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

            SolvedProblem solvedProblem = solvedProblemRepository.save(
                    new SolvedProblem(
                            user,
                            problem,
                            codeFile.html_url(),
                            language,
                            solvedDate
                    )
            );

            saveResolvedCategory(problem, resolvedCategoryName);

            reviewScheduleService.createReviewSchedulesForProblems(List.of(solvedProblem));
        });

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
            System.out.println("[GitHub Sync] 커밋 날짜 조회 실패, 오늘 날짜로 대체 path="
                    + problemPath + " / error=" + e.getMessage());
            return LocalDate.now();
        }
    }

    private LocalDate fetchLatestCommitDate(
            GithubRepository connectedRepository,
            String problemPath
    ) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.github.com")
                .pathSegment(
                        "repos",
                        connectedRepository.getGithubUsername(),
                        connectedRepository.getRepositoryName(),
                        "commits"
                )
                .queryParam("path", decodeGithubPath(problemPath))
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
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://api.github.com")
                .pathSegment("repos", githubUsername, repositoryName, "contents");

        String safePath = decodeGithubPath(path);

        if (!safePath.isBlank()) {
            String[] pathSegments = safePath.split("/");

            for (String segment : pathSegments) {
                if (segment != null && !segment.isBlank()) {
                    builder.pathSegment(segment);
                }
            }
        }

        return builder.build()
                .encode()
                .toUriString();
    }

    private String decodeGithubPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        try {
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return path;
        }
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

        String decodedPath = decodeGithubPath(path);
        String[] tokens = decodedPath.split("/");

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
        if (path == null || path.isBlank()) {
            return true;
        }

        String normalizedPath = decodeGithubPath(path)
                .replace("\\", "/")
                .toLowerCase();

        String[] segments = normalizedPath.split("/");

        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }

            if (segment.equals(".git")
                    || segment.equals("node_modules")
                    || segment.equals("build")
                    || segment.equals("dist")
                    || segment.equals(".idea")
                    || segment.equals(".gradle")
                    || segment.equals("target")
                    || segment.equals("out")
                    || segment.equals("__pycache__")) {
                return true;
            }

            if (segment.equals("readme.md")
                    || segment.equals("readme.txt")
                    || segment.equals("readme")) {
                return true;
            }
        }

        return false;
    }

    private boolean isAlgorithmSolutionFile(String path) {
        if (path == null) return false;

        String lowerPath = decodeGithubPath(path).toLowerCase();

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

        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".java")) return "Java";
        if (lowerFilename.endsWith(".py")) return "Python";
        if (lowerFilename.endsWith(".js")) return "JavaScript";
        if (lowerFilename.endsWith(".cpp")) return "C++";
        if (lowerFilename.endsWith(".c")) return "C";
        if (lowerFilename.endsWith(".kt")) return "Kotlin";

        return "Unknown";
    }

    private String inferPlatformFromPath(String path) {
        if (path == null) {
            return "CUSTOM";
        }

        String decodedPath = decodeGithubPath(path);
        String lowerPath = decodedPath.toLowerCase();

        if (decodedPath.contains("프로그래머스") || lowerPath.contains("programmers")) {
            return "PROGRAMMERS";
        }

        if (decodedPath.contains("백준") || lowerPath.contains("baekjoon") || lowerPath.contains("boj")) {
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

        String decodedPath = decodeGithubPath(path);
        String lowerPath = decodedPath.toLowerCase();

        if (decodedPath.contains("프로그래머스") || lowerPath.contains("programmers")) {
            if (lowerPath.contains("lv.0") || lowerPath.contains("level0") || lowerPath.contains("/0/")) return "Lv.0";
            if (lowerPath.contains("lv.1") || lowerPath.contains("level1") || lowerPath.contains("/1/")) return "Lv.1";
            if (lowerPath.contains("lv.2") || lowerPath.contains("level2") || lowerPath.contains("/2/")) return "Lv.2";
            if (lowerPath.contains("lv.3") || lowerPath.contains("level3") || lowerPath.contains("/3/")) return "Lv.3";
            if (lowerPath.contains("lv.4") || lowerPath.contains("level4") || lowerPath.contains("/4/")) return "Lv.4";
            if (lowerPath.contains("lv.5") || lowerPath.contains("level5") || lowerPath.contains("/5/")) return "Lv.5";

            return "Lv.미정";
        }

        if (decodedPath.contains("백준") || lowerPath.contains("baekjoon") || lowerPath.contains("boj")) {
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

        if (resetRecords) {
            reviewScheduleRepository.deleteAllByUserId(userId);
            solvedProblemRepository.deleteAllByUserId(userId);
        }

        if (connectedRepository != null) {
            githubRepositoryRepository.delete(connectedRepository);
        }
    }
}