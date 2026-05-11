package com.algotrail.backend.domain.tag.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SolvedAcClient {

    private final WebClient webClient;

    public List<String> getTagKeys(Long problemNumber) {
        try {
            System.out.println("[solved.ac 호출] problemNumber = " + problemNumber);

            Map response = webClient.get()
                    .uri("https://solved.ac/api/v3/problem/show?problemId=" + problemNumber)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("tags") == null) {
                System.out.println("[solved.ac 실패] tags 없음: " + problemNumber);
                return List.of();
            }

            List<Map<String, Object>> tags = (List<Map<String, Object>>) response.get("tags");

            List<String> tagKeys = tags.stream()
                    .map(tag -> String.valueOf(tag.get("key")))
                    .toList();

            System.out.println("[solved.ac 태그] " + problemNumber + " -> " + tagKeys);

            return tagKeys;

        } catch (Exception e) {
            System.out.println("[solved.ac 에러] " + problemNumber + " / " + e.getMessage());
            return List.of();
        }
    }


}