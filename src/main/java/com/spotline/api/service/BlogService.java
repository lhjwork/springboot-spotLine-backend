package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.*;
import com.spotline.api.domain.enums.BlogBlockType;
import com.spotline.api.domain.enums.BlogStatus;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.request.CreateBlogRequest;
import com.spotline.api.dto.request.SaveBlogBlocksRequest;
import com.spotline.api.dto.request.UpdateBlogRequest;
import com.spotline.api.dto.response.BlogBlockResponse;
import com.spotline.api.dto.response.BlogDetailResponse;
import com.spotline.api.dto.response.BlogListItemResponse;
import com.spotline.api.dto.response.BlogResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogService {

    private final BlogRepository blogRepository;
    private final SpotLineRepository spotLineRepository;
    private final SpotRepository spotRepository;
    private final UserRepository userRepository;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    /**
     * 블로그 생성 (초안) + SpotLine 기반 블록 자동 생성
     */
    @Transactional
    public BlogDetailResponse create(String userId, CreateBlogRequest request) {
        SpotLine spotLine = spotLineRepository.findById(request.getSpotLineId())
                .orElseThrow(() -> new ResourceNotFoundException("SpotLine", request.getSpotLineId().toString()));

        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getNickname() : "익명";
        String userAvatar = user != null ? user.getAvatar() : null;

        Blog blog = Blog.builder()
                .slug(generateUniqueSlug(request.getTitle()))
                .spotLine(spotLine)
                .userId(userId)
                .userName(userName)
                .userAvatarUrl(userAvatar)
                .title(request.getTitle())
                .build();

        // 블록 자동 생성: INTRO + (SPOT + TRANSITION) × N + OUTRO
        List<BlogBlock> blocks = new ArrayList<>();
        int order = 0;

        blocks.add(BlogBlock.builder()
                .blog(blog).blockType(BlogBlockType.INTRO).blockOrder(order++).build());

        List<SpotLineSpot> spotLineSpots = spotLine.getSpots();
        for (int i = 0; i < spotLineSpots.size(); i++) {
            if (i > 0) {
                blocks.add(BlogBlock.builder()
                        .blog(blog).blockType(BlogBlockType.TRANSITION).blockOrder(order++).build());
            }
            blocks.add(BlogBlock.builder()
                    .blog(blog)
                    .spot(spotLineSpots.get(i).getSpot())
                    .blockType(BlogBlockType.SPOT)
                    .blockOrder(order++)
                    .build());
        }

        blocks.add(BlogBlock.builder()
                .blog(blog).blockType(BlogBlockType.OUTRO).blockOrder(order).build());

        blog.setBlocks(blocks);
        blogRepository.save(blog);

        return BlogDetailResponse.from(blog);
    }

    /**
     * 블로그 상세 조회
     */
    public BlogDetailResponse getBySlug(String slug, String userId) {
        Blog blog = blogRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", slug));

        // DRAFT는 소유자만 조회 가능
        if (blog.getStatus() == BlogStatus.DRAFT) {
            if (userId == null || !blog.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "블로그를 찾을 수 없습니다");
            }
        }

        return BlogDetailResponse.from(blog);
    }

    /**
     * 블로그 메타 수정 (제목, 소개, 커버)
     */
    @Transactional
    public BlogResponse update(String slug, String userId, UpdateBlogRequest request) {
        Blog blog = findBySlugAndOwner(slug, userId);

        if (request.getTitle() != null) blog.setTitle(request.getTitle());
        if (request.getSummary() != null) blog.setSummary(request.getSummary());
        if (request.getCoverImageUrl() != null) blog.setCoverImageUrl(request.getCoverImageUrl());

        return BlogResponse.from(blogRepository.save(blog));
    }

    /**
     * 블로그 삭제 (soft delete)
     */
    @Transactional
    public void delete(String slug, String userId) {
        Blog blog = findBySlugAndOwner(slug, userId);
        blog.setIsActive(false);
        blogRepository.save(blog);
    }

    /**
     * 블로그 발행
     */
    @Transactional
    public BlogResponse publish(String slug, String userId) {
        Blog blog = findBySlugAndOwner(slug, userId);

        // 최소 1개 SPOT 블록에 content가 있어야 발행 가능
        boolean hasContent = blog.getBlocks().stream()
                .anyMatch(b -> b.getBlockType() == BlogBlockType.SPOT
                        && b.getContent() != null
                        && !b.getContent().isBlank());

        if (!hasContent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "최소 1개 Spot에 글을 작성해야 발행 가능합니다");
        }

        blog.setStatus(BlogStatus.PUBLISHED);
        blog.setPublishedAt(LocalDateTime.now());
        return BlogResponse.from(blogRepository.save(blog));
    }

    /**
     * 블로그 비공개 전환
     */
    @Transactional
    public BlogResponse unpublish(String slug, String userId) {
        Blog blog = findBySlugAndOwner(slug, userId);
        blog.setStatus(BlogStatus.DRAFT);
        blog.setPublishedAt(null);
        return BlogResponse.from(blogRepository.save(blog));
    }

    /**
     * 공개 블로그 목록
     */
    public Page<BlogListItemResponse> listPublished(int page, int size, String area) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Blog> blogs = (area != null && !area.isBlank())
                ? blogRepository.findPublishedByArea(area, pageable)
                : blogRepository.findByStatusAndIsActiveTrueOrderByPublishedAtDesc(BlogStatus.PUBLISHED, pageable);

        return blogs.map(BlogListItemResponse::from);
    }

    /**
     * 내 블로그 목록
     */
    public Page<BlogListItemResponse> listMyBlogs(String userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Blog> blogs;
        if (status != null && !status.isBlank()) {
            BlogStatus blogStatus = BlogStatus.valueOf(status.toUpperCase());
            blogs = blogRepository.findByUserIdAndStatusAndIsActiveTrueOrderByUpdatedAtDesc(userId, blogStatus, pageable);
        } else {
            blogs = blogRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId, pageable);
        }

        return blogs.map(BlogListItemResponse::from);
    }

    /**
     * 블로그 slug 목록 (SSR/sitemap)
     */
    public List<String> getAllPublishedSlugs() {
        return blogRepository.findAllPublishedSlugs();
    }

    /**
     * 블록 일괄 저장 (자동 저장)
     */
    @Transactional
    public List<BlogBlockResponse> saveBlocks(UUID blogId, String userId, SaveBlogBlocksRequest request) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", blogId.toString()));

        if (!blog.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 블로그만 수정 가능합니다");
        }

        // 기존 블록 제거 후 재생성 (orphanRemoval)
        blog.getBlocks().clear();

        for (SaveBlogBlocksRequest.BlockRequest blockReq : request.getBlocks()) {
            Spot spot = blockReq.getSpotId() != null
                    ? spotRepository.findById(blockReq.getSpotId()).orElse(null)
                    : null;

            BlogBlock block = BlogBlock.builder()
                    .blog(blog)
                    .spot(spot)
                    .blockType(blockReq.getBlockType())
                    .blockOrder(blockReq.getBlockOrder())
                    .content(blockReq.getContent())
                    .build();

            if (blockReq.getMediaItems() != null) {
                for (SaveBlogBlocksRequest.MediaRequest mediaReq : blockReq.getMediaItems()) {
                    block.getMediaItems().add(BlogBlockMedia.builder()
                            .blogBlock(block)
                            .mediaUrl(mediaReq.getMediaUrl())
                            .mediaOrder(mediaReq.getMediaOrder() != null ? mediaReq.getMediaOrder() : 0)
                            .caption(mediaReq.getCaption())
                            .build());
                }
            }

            blog.getBlocks().add(block);
        }

        blogRepository.save(blog);

        return blog.getBlocks().stream()
                .map(BlogBlockResponse::from)
                .toList();
    }

    // ---- Private helpers ----

    private Blog findBySlugAndOwner(String slug, String userId) {
        Blog blog = blogRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", slug));

        if (!blog.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 블로그만 수정/삭제할 수 있습니다");
        }

        return blog;
    }

    private String generateUniqueSlug(String title) {
        String base = slugify.slugify(title);
        if (base.isEmpty()) {
            base = UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = base;
        int counter = 1;
        while (blogRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
