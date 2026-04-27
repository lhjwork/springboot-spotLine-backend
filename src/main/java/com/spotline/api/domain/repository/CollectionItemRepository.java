package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.CollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID> {

    List<CollectionItem> findByCollectionIdAndIsActiveTrueOrderByItemOrderAsc(UUID collectionId);

    void deleteByCollectionIdAndId(UUID collectionId, UUID itemId);
}
