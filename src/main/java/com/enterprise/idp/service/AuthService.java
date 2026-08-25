package com.enterprise.idp.service;

import com.enterprise.idp.domain.user.AppUser;
import com.enterprise.idp.domain.user.UserRepository;
import com.enterprise.idp.domain.user.UserRole;
import com.enterprise.idp.dto.auth.AuthResponse;
import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.auth.RegisterRequest;
import com.enterprise.idp.exception.ConflictException;
import com.enterprise.idp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for authentication and user registration.
 *
 * <p>register() avoids calling authenticationManager.authenticate() inside the
 * same @Transactional boundary — the user save is uncommitted when authenticate()
 * triggers UserDetailsService in a new transaction, causing UsernameNotFoundException.
 * Instead, register() builds the Authentication object directly from the saved entity
 * and issues tokens without a second DB round-trip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /** Authenticate user and return JWT tokens. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        AppUser user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        log.info("User '{}' logged in successfully", request.getUsername());
        return buildResponse(accessToken, refreshToken, user);
    }

    /**
     * Register a new user and immediately return JWT tokens.
     *
     * <p>Tokens are issued by building an Authentication from the saved entity directly,
     * NOT by calling authenticationManager.authenticate() inside the same transaction.
     * Calling authenticate() inside @Transactional causes UserDetailsService to open a
     * child transaction that cannot see the uncommitted user row → UsernameNotFoundException
     * → InternalAuthenticationServiceException → 500.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.DEVELOPER;

        AppUser user = AppUser.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .fullName(request.getFullName())
            .role(role)
            .enabled(true)
            .build();

        userRepository.save(user);
        log.info("New user '{}' registered with role {}", user.getUsername(), role);

        // Build Authentication directly from the entity — avoids authenticationManager
        // round-trip inside the open transaction which cannot see the uncommitted write.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return buildResponse(accessToken, refreshToken, user);
    }

    private AuthResponse buildResponse(String accessToken, String refreshToken, AppUser user) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(expirationMs)
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();
    }
}
