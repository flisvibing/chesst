package com.chesst.auth;

import com.chesst.auth.dto.*;
import com.chesst.common.exception.BusinessException;
import com.chesst.config.AppProperties;
import com.chesst.security.JwtService;
import com.chesst.user.User;
import com.chesst.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository users,
                       PasswordEncoder encoder,
                       JwtService jwtService,
                       AppProperties appProperties,
                       JavaMailSender mailSender) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.mailSender = mailSender;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!req.passwordsMatch()) {
            throw new BusinessException("Passwords do not match");
        }
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw new BusinessException("Username already taken");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("Email already registered");
        }

        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole("user");
        u.setEmailVerified(false);
        u.setVerificationCode(generateCode());
        u.setVerificationCodeExp(Instant.now().plus(Duration.ofMinutes(15)));
        u = users.save(u);

        sendVerificationEmail(u.getEmail(), u.getVerificationCode());

        log.info("Registered new user id={} username={}", u.getId(), u.getUsername());
        return toAuthResponse(u, false);
    }

    public AuthResponse login(LoginRequest req) {
        User u = users
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(req.identifier(), req.identifier())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }
        if (appProperties.email().verificationRequired() && !u.isEmailVerified()) {
            throw new BusinessException("Please verify your email before signing in. "
                    + "A 6-digit code was sent to " + u.getEmail() + ".");
        }
        return toAuthResponse(u, true);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest req) {
        User u = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BusinessException("Email not found"));
        if (u.isEmailVerified()) {
            return;
        }
        if (u.getVerificationCode() == null
                || !u.getVerificationCode().equals(req.code())
                || u.getVerificationCodeExp() == null
                || u.getVerificationCodeExp().isBefore(Instant.now())) {
            throw new BusinessException("Invalid or expired verification code");
        }
        u.setEmailVerified(true);
        u.setVerificationCode(null);
        u.setVerificationCodeExp(null);
        users.save(u);
        log.info("Email verified for user id={}", u.getId());
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest req) {
        User u = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BusinessException("Email not found"));
        if (u.isEmailVerified()) {
            throw new BusinessException("Email already verified");
        }
        u.setVerificationCode(generateCode());
        u.setVerificationCodeExp(Instant.now().plus(Duration.ofMinutes(15)));
        users.save(u);
        sendVerificationEmail(u.getEmail(), u.getVerificationCode());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        users.findByEmailIgnoreCase(req.email()).ifPresent(u -> {
            String resetToken = generateCode() + generateCode();
            u.setVerificationCode(resetToken);
            u.setVerificationCodeExp(Instant.now().plus(Duration.ofHours(1)));
            users.save(u);
            sendEmail(u.getEmail(), "Chesst password reset",
                    "Use this code to reset your password: " + resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User u = users.findAll().stream()
                .filter(x -> req.token().equals(x.getVerificationCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));
        if (u.getVerificationCodeExp() == null || u.getVerificationCodeExp().isBefore(Instant.now())) {
            throw new BusinessException("Invalid or expired reset token");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setVerificationCode(null);
        u.setVerificationCodeExp(null);
        users.save(u);
    }

    private AuthResponse toAuthResponse(User u, boolean issueToken) {
        String token = issueToken
                ? jwtService.generateAccessToken(u.getId(), u.getUsername(), u.getRole())
                : null;
        long expiresIn = appProperties.jwt().accessTokenTtlMinutes() * 60L;
        return new AuthResponse(
                token,
                "Bearer",
                expiresIn,
                new AuthResponse.UserProfile(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getDisplayName(),
                        u.getRating(),
                        u.isEmailVerified(),
                        u.getLichessUsername(),
                        u.getChesscomUsername(),
                        u.getCreatedAt()
                )
        );
    }

    private String generateCode() {
        int n = 100000 + RNG.nextInt(900000);
        return String.valueOf(n);
    }

    @Async
    protected void sendVerificationEmail(String to, String code) {
        String subject = "Chesst — verify your email";
        String body = "Welcome to Chesst!\n\n"
                + "Your verification code is: " + code + "\n\n"
                + "It expires in 15 minutes.\n";
        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            if (appProperties.email().logCodesWhenNoSmtp()
                    && (appProperties.email().from() == null || appProperties.email().from().isBlank())) {
                log.info("[EMAIL to={}] {} :: {}", to, subject, body);
                return;
            }
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(appProperties.email().from());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            log.info("[EMAIL FALLBACK to={}] {} :: {}", to, subject, body);
        }
    }
}
