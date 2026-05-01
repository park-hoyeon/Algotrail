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

    public GithubSyncResponse sync(Long userId) {
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
            return new GithubSyncResponse(userId, 0, "GitHub 저장소에서 파일을 가져오지 못했습니다.");
        }

        int newSolvedCount = 0;

        for (GithubContentResponse content : contents) {
            if (!isProgrammersDirectory(content)) {
                continue;
            }

            newSolvedCount += syncProgrammersDirectory(user, content.path());
        }

        return new GithubSyncResponse(
                userId,
                newSolvedCount,
                "새로운 풀이 " + newSolvedCount + "개를 동기화했습니다."
        );
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
}