package com.minipay.api_gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OAuth2SuccessHandler
        implements ServerAuthenticationSuccessHandler {

    @Override
    public Mono<Void> onAuthenticationSuccess(
            WebFilterExchange exchange,
            Authentication authentication) {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // TODO: Generate JWT and redirect to frontend with token
        // For now log the successful OAuth2 login
        return Mono.fromRunnable(() ->
                System.out.println("OAuth2 login success for: " + email)
        );
    }
}
