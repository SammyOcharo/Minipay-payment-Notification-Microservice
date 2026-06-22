package com.minipay.api_gateway.security;

import com.minipay.api_gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return Mono.fromCallable(() ->
                userRepository.findByEmail(email)
                        .map(user -> (UserDetails) new User(
                                user.getEmail(),
                                user.getPassword(),
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole()))
                        ))
                        .orElseThrow(() -> {
                            log.warn("User not found: {}", email);
                            return new UsernameNotFoundException(
                                    "User not found: " + email);
                        })
        ).subscribeOn(Schedulers.boundedElastic());
    }
}