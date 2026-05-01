package com.algotrail.backend.domain.github.controller;

import com.algotrail.backend.domain.github.dto.GithubSyncResponse;
import com.algotrail.backend.domain.github.service.GithubSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubSyncController {

    private final GithubSyncService githubSyncService;

    @PostMapping("/sync/{userId}")
    public GithubSyncResponse sync(@PathVariable Long userId) {
        return githubSyncService.sync(userId);
    }
}