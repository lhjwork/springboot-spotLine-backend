package com.spotline.api.service;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.response.UserAdminResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final BlogRepository blogRepository;

    public Page<UserAdminResponse> list(String status, String keyword, Pageable pageable) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<User> users;
        if ("SUSPENDED".equals(status)) {
            users = userRepository.findSuspendedWithKeyword(kw, pageable);
        } else if ("ACTIVE".equals(status)) {
            users = userRepository.findActiveWithKeyword(kw, pageable);
        } else {
            users = userRepository.findAllWithKeyword(kw, pageable);
        }

        return users.map(user -> {
            long spots = spotRepository.countByCreatorId(user.getId());
            long spotLines = spotLineRepository.countByCreatorId(user.getId());
            long blogs = blogRepository.countByUserId(user.getId());
            return UserAdminResponse.from(user, spots, spotLines, blogs);
        });
    }

    @Transactional
    public UserAdminResponse suspend(String userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 정지된 유저입니다");
        }
        user.setSuspended(true);
        user.setSuspendedAt(LocalDateTime.now());
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserAdminResponse unsuspend(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!Boolean.TRUE.equals(user.getSuspended())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정지 상태가 아닌 유저입니다");
        }
        user.setSuspended(false);
        user.setSuspendedAt(null);
        userRepository.save(user);
        return toResponse(user);
    }

    private UserAdminResponse toResponse(User user) {
        long spots = spotRepository.countByCreatorId(user.getId());
        long spotLines = spotLineRepository.countByCreatorId(user.getId());
        long blogs = blogRepository.countByUserId(user.getId());
        return UserAdminResponse.from(user, spots, spotLines, blogs);
    }
}
