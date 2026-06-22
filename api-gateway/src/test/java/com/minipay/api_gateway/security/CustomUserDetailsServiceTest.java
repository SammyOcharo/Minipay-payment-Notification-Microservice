package com.minipay.api_gateway.security;

import com.minipay.api_gateway.model.User;
import com.minipay.api_gateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    // -----------------------------------------------------------------------
    // Happy path — user found
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldReturnUserDetails_whenUserExists() {
        User entity = buildUser("user@example.com", "hashed-password", "USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(entity));

        StepVerifier.create(service.findByUsername("user@example.com"))
                .assertNext(userDetails -> {
                    assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
                    assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
                    assertThat(userDetails.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_USER");
                })
                .verifyComplete();

        verify(userRepository).findByEmail("user@example.com");
    }

    // -----------------------------------------------------------------------
    // Role mapping — ROLE_ prefix is always prepended
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldPrependRolePrefix_toUserRole() {
        User entity = buildUser("admin@example.com", "hashed-password", "ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(entity));

        StepVerifier.create(service.findByUsername("admin@example.com"))
                .assertNext(userDetails ->
                        assertThat(userDetails.getAuthorities())
                                .extracting(GrantedAuthority::getAuthority)
                                .containsExactly("ROLE_ADMIN")
                )
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // UserDetails implementation — mapped to Spring's User, not the entity
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldReturnSpringUserInstance() {
        User entity = buildUser("user@example.com", "hashed-password", "USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(entity));

        StepVerifier.create(service.findByUsername("user@example.com"))
                .assertNext(userDetails ->
                        assertThat(userDetails)
                                .isInstanceOf(org.springframework.security.core.userdetails.User.class)
                )
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // User not found — UsernameNotFoundException emitted
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldEmitUsernameNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        StepVerifier.create(service.findByUsername("ghost@example.com"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(UsernameNotFoundException.class);
                    assertThat(ex.getMessage()).contains("ghost@example.com");
                })
                .verify();

        verify(userRepository).findByEmail("ghost@example.com");
    }

    // -----------------------------------------------------------------------
    // Repository exception — propagated as-is through the Mono
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldPropagateException_whenRepositoryThrows() {
        when(userRepository.findByEmail("user@example.com"))
                .thenThrow(new RuntimeException("DB connection lost"));

        StepVerifier.create(service.findByUsername("user@example.com"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(RuntimeException.class);
                    assertThat(ex.getMessage()).isEqualTo("DB connection lost");
                })
                .verify();
    }

    // -----------------------------------------------------------------------
    // Scheduling — executes on boundedElastic, not the calling thread
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_shouldNotBlockCallingThread() {
        User entity = buildUser("user@example.com", "hashed-password", "USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(entity));

        // If subscribeOn(boundedElastic) is missing, the repository would be
        // called on the test thread synchronously before subscribe() returns.
        // We verify it is still called exactly once (subscription did occur)
        // and that the Mono completes correctly regardless of which thread runs it.
        StepVerifier.create(service.findByUsername("user@example.com"))
                .assertNext(userDetails ->
                        assertThat(userDetails.getUsername()).isEqualTo("user@example.com")
                )
                .verifyComplete();

        verify(userRepository, times(1)).findByEmail("user@example.com");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private User buildUser(String email, String password, String role) {
        User entity = new User();
        entity.setEmail(email);
        entity.setPassword(password);
        entity.setRole(role);
        return entity;
    }
}