package com.algotrail.backend.domain.github.controller;

import com.algotrail.backend.domain.github.dto.GithubRepoResponse;
import com.algotrail.backend.domain.github.dto.GithubRepositoryConnectRequest;
import com.algotrail.backend.domain.github.dto.GithubRepositoryConnectResponse;
import com.algotrail.backend.domain.github.service.GithubRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubRepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @GetMapping("/repositories")
    public List<GithubRepoResponse> getRepositories(@RequestParam String username) {
        return githubRepositoryService.getRepositories(username);
    }

    @PostMapping("/repository/connect")
    public GithubRepositoryConnectResponse connectRepository(
            @RequestBody GithubRepositoryConnectRequest request
    ) {
        return githubRepositoryService.connectRepository(request);
    }

    @GetMapping("/repository/connected")
    public GithubRepositoryConnectResponse getConnectedRepository(@RequestParam Long userId) {
        return githubRepositoryService.getConnectedRepository(userId);
    }
}