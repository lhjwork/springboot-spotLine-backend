package com.spotline.api.controller;

import com.spotline.api.domain.enums.SpotLineTheme;
import com.spotline.api.dto.request.CreateCollectionRequest;
import com.spotline.api.dto.request.UpdateCollectionRequest;
import com.spotline.api.dto.request.UpdateItemOrderRequest;
import com.spotline.api.dto.response.CollectionDetailResponse;
import com.spotline.api.dto.response.CollectionPreviewResponse;
import com.spotline.api.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Collection", description = "컬렉션 CRUD + 탐색")
@RestController
@RequestMapping("/api/v2/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    // --- Public ---

    @Operation(summary = "Featured 컬렉션 (피드 캐러셀)")
    @GetMapping("/featured")
    public ResponseEntity<List<CollectionPreviewResponse>> featured() {
        return ResponseEntity.ok(collectionService.getFeatured());
    }

    @Operation(summary = "컬렉션 목록 (공개)")
    @GetMapping
    public ResponseEntity<Page<CollectionPreviewResponse>> list(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) SpotLineTheme theme,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return ResponseEntity.ok(collectionService.getAdminList(keyword, pageable)
                    .map(d -> CollectionPreviewResponse.builder()
                            .id(d.getId()).slug(d.getSlug()).title(d.getTitle())
                            .description(d.getDescription()).coverImageUrl(d.getCoverImageUrl())
                            .theme(d.getTheme()).area(d.getArea())
                            .itemCount(d.getItemCount()).viewsCount(d.getViewsCount())
                            .build()));
        }
        return ResponseEntity.ok(collectionService.getPublicList(area, theme, pageable));
    }

    @Operation(summary = "컬렉션 상세 조회 (slug)")
    @GetMapping("/{slug}")
    public ResponseEntity<CollectionDetailResponse> getBySlug(@PathVariable String slug) {
        collectionService.incrementViews(slug);
        return ResponseEntity.ok(collectionService.getBySlug(slug));
    }

    // --- Admin ---

    @Operation(summary = "컬렉션 생성")
    @PostMapping
    public ResponseEntity<CollectionDetailResponse> create(
            @Valid @RequestBody CreateCollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionService.create(request));
    }

    @Operation(summary = "컬렉션 수정")
    @PutMapping("/{slug}")
    public ResponseEntity<CollectionDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateCollectionRequest request) {
        return ResponseEntity.ok(collectionService.update(slug, request));
    }

    @Operation(summary = "컬렉션 삭제 (soft)")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        collectionService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    // --- Item Management ---

    @Operation(summary = "컬렉션 아이템 추가")
    @PostMapping("/{slug}/items")
    public ResponseEntity<CollectionDetailResponse> addItem(
            @PathVariable String slug,
            @Valid @RequestBody CreateCollectionRequest.ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionService.addItem(slug, request));
    }

    @Operation(summary = "아이템 순서 변경 (벌크)")
    @PutMapping("/{slug}/items/order")
    public ResponseEntity<Void> updateItemOrder(
            @PathVariable String slug,
            @Valid @RequestBody UpdateItemOrderRequest request) {
        collectionService.updateItemOrder(slug, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "컬렉션 아이템 제거")
    @DeleteMapping("/{slug}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable String slug, @PathVariable UUID itemId) {
        collectionService.removeItem(slug, itemId);
        return ResponseEntity.noContent().build();
    }
}
