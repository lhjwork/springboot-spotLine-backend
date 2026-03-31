package com.spotline.api.service;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String userId, String email) {
        return userRepository.findById(userId)
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .id(userId)
                    .email(email != null ? email : userId + "@unknown")
                    .nickname(email != null ? email.split("@")[0] : "user")
                    .build()
            ));
    }
}
