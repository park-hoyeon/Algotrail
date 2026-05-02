package com.algotrail.backend.domain.github.service;

import com.algotrail.backend.domain.github.client.GithubApiClient;
import com.algotrail.backend.domain.github.dto.GithubRepoResponse;
import com.algotrail.backend.domain.github.dto.GithubRepositoryConnectRequest;
import com.algotrail.backend.domain.github.dto.GithubRepositoryConnectResponse;
import com.algotrail.backend.domain.github.entity.GithubRepository;
import com.algotrail.backend.domain.github.repository.GithubRepositoryRepository;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GithubRepositoryService {

    private final GithubApiClient githubApiClient;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GithubRepoResponse> getRepositories(String username) {
        return githubApiClient.getRepositories(username);
    }

    public GithubRepositoryConnectResponse connectRepository(GithubRepositoryConnectRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        GithubRepository githubRepository = githubRepositoryRepository.findByUserId(request.userId())
                .orElseGet(() -> GithubRepository.builder()
                        .user(user)
                        .connected(true)
                        .build());

        githubRepository.updateRepository(
                request.githubUsername(),
                request.repositoryName(),
                request.repositoryUrl(),
                request.defaultBranch(),
                request.rootPath()
        );

        GithubRepository saved = githubRepositoryRepository.save(githubRepository);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GithubRepositoryConnectResponse getConnectedRepository(Long userId) {
        GithubRepository githubRepository = githubRepositoryRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("연동된 GitHub 저장소가 없습니다."));

        return toResponse(githubRepository);
    }

    private GithubRepositoryConnectResponse toResponse(GithubRepository githubRepository) {
        return new GithubRepositoryConnectResponse(
                githubRepository.getUser().getId(),
                githubRepository.getGithubUsername(),
                githubRepository.getRepositoryName(),
                githubRepository.getRepositoryUrl(),
                githubRepository.getDefaultBranch(),
                githubRepository.getRootPath(),
                githubRepository.isConnected(),
                githubRepository.getLastSyncedAt()
        );
    }
}