package com.surfthetask.service;

import com.surfthetask.domain.entity.NotificationPreference;
import com.surfthetask.domain.entity.User;
import com.surfthetask.dto.request.LoginReqDto;
import com.surfthetask.dto.request.RegisterReqDto;
import com.surfthetask.dto.response.LoginResDto;
import com.surfthetask.exception.DuplicateResourceException;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.exception.UnauthorizedException;
import com.surfthetask.repository.NotificationPreferenceRepository;
import com.surfthetask.repository.UserRepository;
import com.surfthetask.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User register(RegisterReqDto req) {
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new DuplicateResourceException("loginId already exists");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("email already exists");
        }

        User user = userRepository.save(new User(req.loginId(), hash(req.password()), req.name(), req.email()));
        notificationPreferenceRepository.save(new NotificationPreference(user));
        return user;
    }

    public LoginResDto login(LoginReqDto req) {
        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new UnauthorizedException("loginId or password is invalid"));

        if (!user.getPasswordHash().equals(hash(req.password()))) {
            throw new UnauthorizedException("loginId or password is invalid");
        }
        return LoginResDto.of(user, jwtTokenProvider.issue(user));
    }

    public void logout(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user not found: " + userId);
        }
    }

    private String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
