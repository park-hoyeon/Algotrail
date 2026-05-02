package com.algotrail.backend.domain.github.client;

import com.algotrail.backend.domain.github.dto.GithubRepoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GithubApiClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .build();

    public List<GithubRepoResponse> getRepositories(String username) {
        Map[] response = restClient.get()
                .uri("/users/{username}/repos?sort=updated&per_page=100", username)
                .retrieve()
                .body(Map[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .map(repo -> new GithubRepoResponse(
                        String.valueOf(repo.get("name")),
                        String.valueOf(repo.get("full_name")),
                        String.valueOf(repo.get("html_url")),
                        String.valueOf(repo.get("default_branch"))
                ))
                .toList();
    }
}