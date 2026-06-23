package com.minipay.api_gateway.config;

import com.minipay.api_gateway.filter.JwtAuthenticationFilter;
import com.minipay.api_gateway.security.CustomUserDetailsService;
import com.minipay.api_gateway.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login").permitAll()
                        // OAuth2
                        .pathMatchers("/oauth2/**",
                                "/login/oauth2/**").permitAll()
                        .pathMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // Actuator
                        .pathMatchers("/actuator/**").permitAll()
                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(
                                new OAuth2SuccessHandler()))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager manager =
                new UserDetailsRepositoryReactiveAuthenticationManager(
                        userDetailsService);
        manager.setPasswordEncoder(passwordEncoder());
        return manager;
    }
}