package com.chesst.user;

import com.chesst.common.exception.BusinessException;
import com.chesst.game.GameRepository;
import com.chesst.user.dto.ProfileResponse;
import com.chesst.user.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final GameRepository games;

    public UserService(UserRepository users, GameRepository games) {
        this.users = users;
        this.games = games;
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", 404));
        long count = games.countByOwnerId(userId);
        return new ProfileResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(),
                u.getBio(), u.getAvatarUrl(), u.getRating(), u.isEmailVerified(),
                u.getLichessUsername(), u.getChesscomUsername(), count
        );
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest req) {
        User u = users.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", 404));
        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.bio() != null) u.setBio(req.bio());
        if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl());
        users.save(u);
        return profile(userId);
    }
}
