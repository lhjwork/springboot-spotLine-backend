package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.Collection;
import com.spotline.api.domain.entity.CollectionItem;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.enums.SpotLineTheme;
import com.spotline.api.domain.repository.CollectionItemRepository;
import com.spotline.api.domain.repository.CollectionRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreateCollectionRequest;
import com.spotline.api.dto.request.UpdateCollectionRequest;
import com.spotline.api.dto.request.UpdateItemOrderRequest;
import com.spotline.api.dto.response.CollectionDetailResponse;
import com.spotline.api.dto.response.CollectionPreviewResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    // --- Read ---

    public CollectionDetailResponse getBySlug(String slug) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));
        return CollectionDetailResponse.from(c);
    }

    public List<CollectionPreviewResponse> getFeatured() {
        return collectionRepository
                .findByIsFeaturedTrueAndIsPublishedTrueAndIsActiveTrueOrderByDisplayOrderAsc()
                .stream().map(CollectionPreviewResponse::from).toList();
    }

    public Page<CollectionPreviewResponse> getPublicList(String area, SpotLineTheme theme, Pageable pageable) {
        return collectionRepository.findByFilters(area, theme, pageable)
                .map(CollectionPreviewResponse::from);
    }

    public Page<CollectionDetailResponse> getAdminList(String keyword, Pageable pageable) {
        return collectionRepository.findByKeyword(keyword, pageable)
                .map(CollectionDetailResponse::from);
    }

    // --- Write ---

    @Transactional
    public CollectionDetailResponse create(CreateCollectionRequest req) {
        String slug = generateUniqueSlug(req.getTitle());
        Collection c = Collection.builder()
                .title(req.getTitle()).slug(slug)
                .description(req.getDescription()).coverImageUrl(req.getCoverImageUrl())
                .theme(req.getTheme()).area(req.getArea())
                .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                .isPublished(req.getIsPublished() != null ? req.getIsPublished() : true)
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .createdBy(req.getCreatedBy())
                .build();

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            addItemsToCollection(c, req.getItems());
        }

        return CollectionDetailResponse.from(collectionRepository.save(c));
    }

    @Transactional
    public CollectionDetailResponse update(String slug, UpdateCollectionRequest req) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));

        if (req.getTitle() != null) c.setTitle(req.getTitle());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getCoverImageUrl() != null) c.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getTheme() != null) c.setTheme(req.getTheme());
        if (req.getArea() != null) c.setArea(req.getArea());
        if (req.getIsFeatured() != null) c.setIsFeatured(req.getIsFeatured());
        if (req.getIsPublished() != null) c.setIsPublished(req.getIsPublished());
        if (req.getDisplayOrder() != null) c.setDisplayOrder(req.getDisplayOrder());

        return CollectionDetailResponse.from(collectionRepository.save(c));
    }

    @Transactional
    public void delete(String slug) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));
        c.setIsActive(false);
        collectionRepository.save(c);
    }

    // --- Items ---

    @Transactional
    public CollectionDetailResponse addItem(String slug, CreateCollectionRequest.ItemRequest req) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));

        CollectionItem item = buildItem(c, req);
        c.getItems().add(item);
        c.setItemCount(c.getItemCount() + 1);

        return CollectionDetailResponse.from(collectionRepository.save(c));
    }

    @Transactional
    public void updateItemOrder(String slug, UpdateItemOrderRequest req) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));

        Map<UUID, Integer> orderMap = req.getItems().stream()
                .collect(Collectors.toMap(UpdateItemOrderRequest.ItemOrder::getId,
                        UpdateItemOrderRequest.ItemOrder::getItemOrder));

        c.getItems().forEach(item -> {
            Integer newOrder = orderMap.get(item.getId());
            if (newOrder != null) item.setItemOrder(newOrder);
        });

        collectionRepository.save(c);
    }

    @Transactional
    public void removeItem(String slug, UUID itemId) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));
        c.getItems().removeIf(i -> i.getId().equals(itemId));
        c.setItemCount(Math.max(0, c.getItemCount() - 1));
        collectionRepository.save(c);
    }

    // --- Views ---

    @Transactional
    public void incrementViews(String slug) {
        Collection c = collectionRepository.findBySlugAndIsActiveTrue(slug).orElse(null);
        if (c != null) {
            c.setViewsCount(c.getViewsCount() + 1);
            collectionRepository.save(c);
        }
    }

    // --- Helpers ---

    private String generateUniqueSlug(String title) {
        String base = slugify.slugify(title);
        if (base.isEmpty()) base = UUID.randomUUID().toString().substring(0, 8);
        String slug = base;
        int counter = 1;
        while (collectionRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private void addItemsToCollection(Collection c, List<CreateCollectionRequest.ItemRequest> items) {
        for (int i = 0; i < items.size(); i++) {
            var req = items.get(i);
            if (req.getItemOrder() == null) req.setItemOrder(i + 1);
            c.getItems().add(buildItem(c, req));
        }
        c.setItemCount(items.size());
    }

    private CollectionItem buildItem(Collection c, CreateCollectionRequest.ItemRequest req) {
        CollectionItem.CollectionItemBuilder b = CollectionItem.builder()
                .collection(c).itemOrder(req.getItemOrder()).itemNote(req.getItemNote());

        if (req.getSpotId() != null) {
            Spot spot = spotRepository.findById(req.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Spot", req.getSpotId().toString()));
            b.spot(spot);
        } else if (req.getSpotLineId() != null) {
            SpotLine sl = spotLineRepository.findById(req.getSpotLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("SpotLine", req.getSpotLineId().toString()));
            b.spotLine(sl);
        }
        return b.build();
    }
}
