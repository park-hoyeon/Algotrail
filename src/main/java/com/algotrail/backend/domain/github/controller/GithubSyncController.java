package com.algotrail.backend.domain.github.controller;

import com.algotrail.backend.domain.github.dto.GithubSettingResponse;
import com.algotrail.backend.domain.github.dto.GithubSyncResponse;
import com.algotrail.backend.domain.github.service.GithubSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubSyncController {

    private final GithubSyncService githubSyncService;

    @GetMapping("/setting")
    public GithubSettingResponse getGithubSetting(@RequestParam Long userId) {
        return githubSyncService.getGithubSetting(userId);
    }

    @PostMapping("/sync/{userId}")
    public GithubSyncResponse sync(@PathVariable Long userId) {
        return githubSyncService.sync(userId);
    }

    @DeleteMapping("/disconnect")
    public void disconnectGithub(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean resetRecords
    ) {
        githubSyncService.disconnectGithub(userId, resetRecords);
    }
}