package com.spotline.api.service;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.entity.UserFollow;
import com.spotline.api.domain.repository.UserFollowRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.response.FollowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    public FollowResponse follow(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다");
        }
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 팔로우 중입니다");
        }

        userFollowRepository.save(UserFollow.builder()
            .followerId(followerId).followingId(followingId).build());

        User following = userRepository.findById(followingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다"));
        following.setFollowersCount(following.getFollowersCount() + 1);
        userRepository.save(following);

        User follower = userRepository.findById(followerId).orElseThrow();
        follower.setFollowingCount(follower.getFollowingCount() + 1);
        userRepository.save(follower);

        return new FollowResponse(true, following.getFollowersCount());
    }

    public FollowResponse unfollow(String followerId, String followingId) {
        UserFollow uf = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팔로우 관계가 없습니다"));
        userFollowRepository.delete(uf);

        User following = userRepository.findById(followingId).orElseThrow();
        following.setFollowersCount(Math.max(0, following.getFollowersCount() - 1));
        userRepository.save(following);

        User follower = userRepository.findById(followerId).orElseThrow();
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
        userRepository.save(follower);

        return new FollowResponse(false, following.getFollowersCount());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String followerId, String followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public Page<User> getFollowers(String userId, Pageable pageable) {
        return userFollowRepository.findByFollowingIdOrderByCreatedAtDesc(userId, pageable)
            .map(uf -> userRepository.findById(uf.getFollowerId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<User> getFollowing(String userId, Pageable pageable) {
        return userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable)
            .map(uf -> userRepository.findById(uf.getFollowingId()).orElse(null));
    }
}
