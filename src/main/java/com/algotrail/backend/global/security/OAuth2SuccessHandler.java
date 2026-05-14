package com.algotrail.backend.global.security;

import com.algotrail.backend.domain.user.entity.LoginProvider;
import com.algotrail.backend.domain.user.entity.User;
import com.algotrail.backend.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String requestUri = request.getRequestURI();
        boolean isGithub = requestUri.contains("github");

        LoginProvider provider = isGithub ? LoginProvider.GITHUB : LoginProvider.GOOGLE;

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId;
        String username;
        String email;
        String profileImageUrl;
        String githubUsername = null;

        if (provider == LoginProvider.GITHUB) {
            providerId = String.valueOf(attributes.get("id"));
            username = String.valueOf(attributes.get("login"));
            email = attributes.get("email") == null ? null : String.valueOf(attributes.get("email"));
            profileImageUrl = attributes.get("avatar_url") == null ? null : String.valueOf(attributes.get("avatar_url"));
            githubUsername = username;
        } else {
            providerId = String.valueOf(attributes.get("sub"));
            username = String.valueOf(attributes.get("name"));
            email = attributes.get("email") == null ? null : String.valueOf(attributes.get("email"));
            profileImageUrl = attributes.get("picture") == null ? null : String.valueOf(attributes.get("picture"));
        }

        String finalGithubUsername = githubUsername;

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    existingUser.updateProfile(username, email, profileImageUrl);
                    if (provider == LoginProvider.GITHUB && existingUser.getGithubUsername() == null) {
                        existingUser.connectGithub(finalGithubUsername, null);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = new User(username, email, profileImageUrl, provider, providerId);
                    if (provider == LoginProvider.GITHUB) {
                        newUser.connectGithub(finalGithubUsername, null);
                    }
                    return newUser;
                });

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.createToken(savedUser.getId());

        String redirectUrl = "http://localhost:5173/oauth/success"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&userId=" + savedUser.getId();

        response.sendRedirect(redirectUrl);
    }
}