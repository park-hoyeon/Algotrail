package com.algotrail.backend.domain.tag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ProblemCodeFetchService {

    private final WebClient webClient;

    public String fetchCodeContentFromGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return "";
        }

        try {
            String rawUrl = convertGithubBlobUrlToRawUrl(githubUrl);

            System.out.println("[분류용 GitHub 코드 조회]");
            System.out.println("htmlUrl = " + githubUrl);
            System.out.println("rawUrl = " + rawUrl);

            String content = webClient.get()
                    .uri(java.net.URI.create(rawUrl))
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
            System.out.println("[분류용 코드 조회 실패] " + e.getMessage());
            return "";
        }
    }

    private String convertGithubBlobUrlToRawUrl(String githubUrl) {
        return githubUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replace("/blob/", "/");
    }
}