package com.minipay.api_gateway.exception;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ExceptionTestController {

    @GetMapping("/test/unauthorized")
    public Mono<String> unauthorized() {
        throw new UnauthorizedException("Invalid token");
    }

    @GetMapping("/test/illegal-argument")
    public Mono<String> illegalArgument() {
        throw new IllegalArgumentException("Bad input");
    }

    @GetMapping("/test/general-error")
    public Mono<String> generalError() {
        throw new RuntimeException("Boom");
    }

    @PostMapping("/test/validate")
    public Mono<String> validate(@Valid @RequestBody ExceptionTestRequest request) {
        return Mono.just("ok");
    }
}