package com.enterprise.idp.service;

import com.enterprise.idp.domain.user.AppUser;
import com.enterprise.idp.domain.user.UserRepository;
import com.enterprise.idp.domain.user.UserRole;
import com.enterprise.idp.dto.auth.AuthResponse;
import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.auth.RegisterRequest;
import com.enterprise.idp.exception.ConflictException;
import com.enterprise.idp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 *
 * <p>register() no longer calls authenticationManager.authenticate() internally —
 * it builds the Authentication object directly from the saved entity to avoid the
 * @Transactional / child-transaction race with UserDetailsService. Tests reflect this.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new AppUser();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@enterprise.com");
        existingUser.setPassword("hashed_password");
        existingUser.setFullName("Test User");
        existingUser.setRole(UserRole.DEVELOPER);
        existingUser.setEnabled(true);
    }

    @Test
    @DisplayName("register() — creates user and returns accessToken with the requested username")
    void register_success() {
        // register() builds Authentication directly from the saved entity (no authManager call).
        // authenticationManager mock is NOT stubbed here — Mockito strict mode would reject it.
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("new@enterprise.com");
        req.setPassword("SecurePass123");
        req.setFullName("New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@enterprise.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(AppUser.class))).thenReturn(existingUser);
        // register() calls generateAccessToken(any Authentication) — the internal
        // UsernamePasswordAuthenticationToken it builds is an Authentication instance.
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class)))
            .thenReturn("access.token.value");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh.token.value");

        AuthResponse response = authService.register(req);

        // username comes from the locally constructed user (req.getUsername()), not from the save mock
        assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.value");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("register() — throws ConflictException when username taken")
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("another@enterprise.com");
        req.setPassword("Pass123");
        req.setFullName("Dupe");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("testuser");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() — returns accessToken on valid credentials")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("access.token.value");
        when(jwtTokenProvider.generateRefreshToken("testuser")).thenReturn("refresh.token.value");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.value");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("login() — throws BadCredentialsException on wrong password")
    void login_badCredentials_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(BadCredentialsException.class);
    }
}
