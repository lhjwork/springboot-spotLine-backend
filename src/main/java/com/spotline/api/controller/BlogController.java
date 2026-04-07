package com.spotline.api.controller;

import com.spotline.api.dto.request.CreateBlogRequest;
import com.spotline.api.dto.request.SaveBlogBlocksRequest;
import com.spotline.api.dto.request.UpdateBlogRequest;
import com.spotline.api.dto.response.BlogBlockResponse;
import com.spotline.api.dto.response.BlogDetailResponse;
import com.spotline.api.dto.response.BlogListItemResponse;
import com.spotline.api.dto.response.BlogResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Blog", description = "블로그 CRUD + 발행")
@RestController
@RequestMapping("/api/v2/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final AuthUtil authUtil;

    @Operation(summary = "블로그 생성 (초안 + 블록 자동 생성)")
    @PostMapping
    public ResponseEntity<BlogDetailResponse> create(@Valid @RequestBody CreateBlogRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blogService.create(userId, request));
    }

    @Operation(summary = "블로그 상세 조회")
    @GetMapping("/{slug}")
    public ResponseEntity<BlogDetailResponse> getBySlug(@PathVariable String slug) {
        String userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(blogService.getBySlug(slug, userId));
    }

    @Operation(summary = "블로그 메타 수정 (제목, 소개, 커버)")
    @PutMapping("/{slug}")
    public ResponseEntity<BlogResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateBlogRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.ok(blogService.update(slug, userId, request));
    }

    @Operation(summary = "블로그 삭제 (soft delete)")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        String userId = authUtil.requireUserId();
        blogService.delete(slug, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "블로그 발행")
    @PatchMapping("/{slug}/publish")
    public ResponseEntity<BlogResponse> publish(@PathVariable String slug) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.ok(blogService.publish(slug, userId));
    }

    @Operation(summary = "블로그 비공개 전환")
    @PatchMapping("/{slug}/unpublish")
    public ResponseEntity<BlogResponse> unpublish(@PathVariable String slug) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.ok(blogService.unpublish(slug, userId));
    }

    @Operation(summary = "공개 블로그 목록")
    @GetMapping
    public ResponseEntity<Page<BlogListItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String area) {
        return ResponseEntity.ok(blogService.listPublished(page, size, area));
    }

    @Operation(summary = "내 블로그 목록")
    @GetMapping("/me")
    public ResponseEntity<Page<BlogListItemResponse>> myBlogs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.ok(blogService.listMyBlogs(userId, status, page, size));
    }

    @Operation(summary = "블로그 slug 목록 (SSR/sitemap)")
    @GetMapping("/slugs")
    public ResponseEntity<List<String>> slugs() {
        return ResponseEntity.ok(blogService.getAllPublishedSlugs());
    }

    @Operation(summary = "블록 일괄 저장 (자동 저장)")
    @PutMapping("/{blogId}/blocks")
    public ResponseEntity<List<BlogBlockResponse>> saveBlocks(
            @PathVariable UUID blogId,
            @Valid @RequestBody SaveBlogBlocksRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.ok(blogService.saveBlocks(blogId, userId, request));
    }
}
