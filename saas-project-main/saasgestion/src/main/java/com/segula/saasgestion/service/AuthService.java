package com.segula.saasgestion.service;
import com.segula.saasgestion.domain.AppUser;
import com.segula.saasgestion.domain.RefreshToken;
import com.segula.saasgestion.dto.ChangePasswordRequest;
import com.segula.saasgestion.dto.LoginRequest;
import com.segula.saasgestion.dto.LoginResponse;
import com.segula.saasgestion.repository.AppUserRepository;
import com.segula.saasgestion.repository.RefreshTokenRepository;
import com.segula.saasgestion.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository      userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final JwtUtils               jwtUtils;
    private final PasswordEncoder        passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest req) {
        AppUser user = userRepo
            .findByEmailIgnoreCaseAndDeletedAtIsNull(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        if (!user.isActive()) {
            throw new IllegalStateException("Compte desactive");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        refreshRepo.revokeAllByUserId(user.getId());

        String accessToken = jwtUtils.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());
        String refreshRaw  = jwtUtils.generateRefreshToken(user.getId());
        String refreshHash = hashToken(refreshRaw);

        RefreshToken rt = RefreshToken.builder()
            .user(user)
            .tokenHash(refreshHash)
            .issuedAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenMs() / 1000))
            .build();
        refreshRepo.save(rt);

        log.info("Login reussi pour {} (forceChange={})", user.getEmail(), user.isMustChangePassword());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshRaw)
            .forceChange(user.isMustChangePassword())
            .role(user.getRole())
            .fullName(user.getFullName())
            .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        AppUser user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordResetAt(OffsetDateTime.now());
        userRepo.save(user);
        refreshRepo.revokeAllByUserId(userId);
        log.info("Mot de passe change pour userId={}", userId);
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken stored = refreshRepo.findByTokenHash(hash)
            .orElseThrow(() -> new IllegalArgumentException("Refresh token invalide"));
        if (stored.getRevokedAt() != null) {
            throw new IllegalStateException("Refresh token revoque");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Refresh token expire");
        }
        AppUser user = stored.getUser();
        stored.setRevokedAt(OffsetDateTime.now());
        refreshRepo.save(stored);

        String newAccessToken = jwtUtils.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());
        String newRefreshRaw  = jwtUtils.generateRefreshToken(user.getId());

        RefreshToken newRt = RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(newRefreshRaw))
            .issuedAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenMs() / 1000))
            .build();
        refreshRepo.save(newRt);

        return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshRaw)
            .forceChange(user.isMustChangePassword())
            .role(user.getRole())
            .fullName(user.getFullName())
            .build();
    }

    private String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Erreur hachage token", e);
        }
    }
}