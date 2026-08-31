package com.chesst.user;

import com.chesst.common.exception.BusinessException;
import com.chesst.security.AuthContext;
import com.chesst.user.dto.ProfileResponse;
import com.chesst.user.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ProfileResponse me(HttpServletRequest req) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return userService.profile(uid);
    }

    @PatchMapping
    public ProfileResponse update(HttpServletRequest req, @Valid @RequestBody UpdateProfileRequest body) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return userService.update(uid, body);
    }
}
