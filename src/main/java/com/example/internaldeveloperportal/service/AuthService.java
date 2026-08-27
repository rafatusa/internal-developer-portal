package com.example.internaldeveloperportal.service;

import com.example.internaldeveloperportal.domain.PortalUser;
import com.example.internaldeveloperportal.dto.AuthDtos.LoginRequest;
import com.example.internaldeveloperportal.dto.AuthDtos.RegisterRequest;
import com.example.internaldeveloperportal.dto.AuthDtos.TokenResponse;
import com.example.internaldeveloperportal.exception.ConflictException;
import com.example.internaldeveloperportal.repository.PortalUserRepository;
import com.example.internaldeveloperportal.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and credential verification, issuing JWTs on success. */
@Service
public class AuthService {

    private final PortalUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PortalUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates a new portal user.
     *
     * @throws ConflictException when the username is already taken
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is already taken");
        }
        PortalUser user = new PortalUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
        return issue(user);
    }

    /**
     * Verifies credentials and issues a token.
     *
     * @throws BadCredentialsException when the user is unknown or the password is wrong
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        PortalUser user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issue(user);
    }

    private TokenResponse issue(PortalUser user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new TokenResponse(token, "Bearer", jwtService.getExpiresInSeconds(), user.getUsername());
    }
}
