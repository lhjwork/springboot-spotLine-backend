package com.spotline.api.service;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserFollow;
import com.spotline.api.domain.enums.BlogStatus;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.UserFollowRepository;
import com.spotline.api.dto.response.FollowingFeedItemResponse;
import com.spotline.api.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final UserFollowRepository userFollowRepository;
    private final SpotLineRepository spotLineRepository;
    private final BlogRepository blogRepository;
    private final S3Service s3Service;

    public Page<FollowingFeedItemResponse> getFollowingFeed(String userId, Pageable pageable) {
        // 1. Get following user IDs
        List<String> followingIds = userFollowRepository
                .findByFollowerIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .map(UserFollow::getFollowingId)
                .getContent();

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. Fetch SpotLines and Blogs from followed users
        List<SpotLine> spotLines = spotLineRepository
                .findByCreatorIdInAndIsActiveTrueOrderByCreatedAtDesc(followingIds);

        List<Blog> blogs = blogRepository
                .findByUserIdInAndStatusAndIsActiveTrueOrderByPublishedAtDesc(
                        followingIds, BlogStatus.PUBLISHED);

        // 3. Merge and sort by createdAt DESC
        String s3BaseUrl = getS3BaseUrl();
        List<FollowingFeedItemResponse> merged = new ArrayList<>();

        for (SpotLine sl : spotLines) {
            merged.add(FollowingFeedItemResponse.fromSpotLine(sl, s3BaseUrl));
        }
        for (Blog blog : blogs) {
            merged.add(FollowingFeedItemResponse.fromBlog(blog));
        }

        merged.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 4. Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), merged.size());

        if (start >= merged.size()) {
            return new PageImpl<>(List.of(), pageable, merged.size());
        }

        return new PageImpl<>(merged.subList(start, end), pageable, merged.size());
    }

    private String getS3BaseUrl() {
        return s3Service.getPublicUrl("").replaceAll("/$", "");
    }
}
