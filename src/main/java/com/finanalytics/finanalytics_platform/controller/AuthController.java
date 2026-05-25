package com.finanalytics.finanalytics_platform.controller;

import com.finanalytics.finanalytics_platform.dto.*;
import com.finanalytics.finanalytics_platform.entity.User;
import com.finanalytics.finanalytics_platform.repository.UserRepository;
import com.finanalytics.finanalytics_platform.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserRepository        userRepo;
    private final PasswordEncoder       encoder;

    /**
     * POST /api/auth/register
     * Returns the SAME message whether the email exists or not.
     * Prevents user enumeration attacks — attacker cannot determine
     * which emails are registered.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (!userRepo.existsByEmail(req.email().toLowerCase())) {
            User user = User.builder()
                    .email(req.email().toLowerCase().trim())
                    .fullName(req.fullName().trim())
                    .password(encoder.encode(req.password()))   // BCrypt hash — never plaintext
                    .roles(Set.of("ROLE_USER"))
                    .build();
            userRepo.save(user);
            log.info("USER_REGISTERED email={}", user.getEmail());
        }
        // Same response regardless — prevents enumeration
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Registration processed. Check your email to verify."));
    }

    /**
     * POST /api/auth/login
     * Returns 401 for both wrong password AND unknown email.
     * Same response prevents user enumeration.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.email().toLowerCase().trim(),
                            req.password()
                    )
            );
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            log.warn("FAILED_LOGIN email={}", req.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid credentials"));
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new AuthResponse(null, null, "Account locked. Contact support."));
        }

        User user = userRepo.findByEmail(req.email().toLowerCase()).orElseThrow();

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail(),
                user.getRoles().stream().toList());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("USER_LOGIN email={}", user.getEmail());
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, null));
    }

    /**
     * POST /api/auth/refresh
     * Exchange a valid refresh token for a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            String email = jwtUtil.extractEmail(req.refreshToken());
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String newAccess = jwtUtil.generateAccessToken(
                    user.getEmail(), user.getRoles().stream().toList());
            return ResponseEntity.ok(new AuthResponse(newAccess, req.refreshToken(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid or expired refresh token"));
        }
    }
}