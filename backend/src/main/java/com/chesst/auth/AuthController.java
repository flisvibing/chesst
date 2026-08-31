package com.chesst.auth;

import com.chesst.auth.dto.*;
import com.chesst.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Long uid = AuthContext.resolve(request.getAttribute("userId"));
        return ResponseEntity.ok(Map.of("ok", true, "userId", uid == null ? "null" : uid));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resend(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerification(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> reset(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
