package com.spotline.api.service;

import com.spotline.api.domain.entity.*;
import com.spotline.api.domain.enums.NotificationType;
import com.spotline.api.domain.repository.*;
import com.spotline.api.dto.response.SocialStatusResponse;
import com.spotline.api.dto.response.SocialToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {

    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final SpotVisitRepository spotVisitRepository;
    private final SpotLineLikeRepository spotLineLikeRepository;
    private final SpotLineSaveRepository spotLineSaveRepository;
    private final BlogRepository blogRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogSaveRepository blogSaveRepository;
    private final NotificationService notificationService;

    public SocialToggleResponse toggleSpotLike(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLike> existing = spotLikeRepository.findByUserIdAndSpot(userId, spot);
        boolean liked;
        if (existing.isPresent()) {
            spotLikeRepository.delete(existing.get());
            spot.setLikesCount(Math.max(0, spot.getLikesCount() - 1));
            liked = false;
        } else {
            spotLikeRepository.save(SpotLike.builder().userId(userId).spot(spot).build());
            spot.setLikesCount(spot.getLikesCount() + 1);
            liked = true;
        }
        spotRepository.save(spot);
        if (liked && spot.getCreatorId() != null) {
            try {
                notificationService.create(userId, spot.getCreatorId(), NotificationType.SPOT_LIKE,
                    "SPOT", spotId.toString(), spot.getSlug());
            } catch (Exception ignored) {}
        }
        return new SocialToggleResponse(liked, null, null, spot.getLikesCount(), spot.getSavesCount(), spot.getVisitedCount());
    }

    public SocialToggleResponse toggleSpotSave(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotSave> existing = spotSaveRepository.findByUserIdAndSpot(userId, spot);
        boolean saved;
        if (existing.isPresent()) {
            spotSaveRepository.delete(existing.get());
            spot.setSavesCount(Math.max(0, spot.getSavesCount() - 1));
            saved = false;
        } else {
            spotSaveRepository.save(SpotSave.builder().userId(userId).spot(spot).build());
            spot.setSavesCount(spot.getSavesCount() + 1);
            saved = true;
        }
        spotRepository.save(spot);
        return new SocialToggleResponse(null, saved, null, spot.getLikesCount(), spot.getSavesCount(), spot.getVisitedCount());
    }

    public SocialToggleResponse toggleSpotVisit(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotVisit> existing = spotVisitRepository.findByUserIdAndSpot(userId, spot);
        boolean visited;
        if (existing.isPresent()) {
            spotVisitRepository.delete(existing.get());
            spot.setVisitedCount(Math.max(0, spot.getVisitedCount() - 1));
            visited = false;
        } else {
            spotVisitRepository.save(SpotVisit.builder().userId(userId).spot(spot).build());
            spot.setVisitedCount(spot.getVisitedCount() + 1);
            visited = true;
        }
        spotRepository.save(spot);
        return new SocialToggleResponse(null, null, visited, spot.getLikesCount(), spot.getSavesCount(), spot.getVisitedCount());
    }

    public SocialToggleResponse toggleSpotLineLike(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLineLike> existing = spotLineLikeRepository.findByUserIdAndSpotLine(userId, spotLine);
        boolean liked;
        if (existing.isPresent()) {
            spotLineLikeRepository.delete(existing.get());
            spotLine.setLikesCount(Math.max(0, spotLine.getLikesCount() - 1));
            liked = false;
        } else {
            spotLineLikeRepository.save(SpotLineLike.builder().userId(userId).spotLine(spotLine).build());
            spotLine.setLikesCount(spotLine.getLikesCount() + 1);
            liked = true;
        }
        spotLineRepository.save(spotLine);
        if (liked && spotLine.getCreatorId() != null) {
            try {
                notificationService.create(userId, spotLine.getCreatorId(), NotificationType.SPOTLINE_LIKE,
                    "SPOTLINE", spotLineId.toString(), spotLine.getSlug());
            } catch (Exception ignored) {}
        }
        return new SocialToggleResponse(liked, null, null, spotLine.getLikesCount(), spotLine.getSavesCount(), null);
    }

    public SocialToggleResponse toggleSpotLineSave(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLineSave> existing = spotLineSaveRepository.findByUserIdAndSpotLine(userId, spotLine);
        boolean saved;
        if (existing.isPresent()) {
            spotLineSaveRepository.delete(existing.get());
            spotLine.setSavesCount(Math.max(0, spotLine.getSavesCount() - 1));
            saved = false;
        } else {
            spotLineSaveRepository.save(SpotLineSave.builder().userId(userId).spotLine(spotLine).build());
            spotLine.setSavesCount(spotLine.getSavesCount() + 1);
            saved = true;
        }
        spotLineRepository.save(spotLine);
        return new SocialToggleResponse(null, saved, null, spotLine.getLikesCount(), spotLine.getSavesCount(), null);
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse getSpotSocialStatus(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            spotLikeRepository.existsByUserIdAndSpot(userId, spot),
            spotSaveRepository.existsByUserIdAndSpot(userId, spot),
            spotVisitRepository.existsByUserIdAndSpot(userId, spot)
        );
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse getSpotLineSocialStatus(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            spotLineLikeRepository.existsByUserIdAndSpotLine(userId, spotLine),
            spotLineSaveRepository.existsByUserIdAndSpotLine(userId, spotLine),
            false
        );
    }

    // ==================== Blog Social ====================

    public SocialToggleResponse toggleBlogLike(String userId, UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<BlogLike> existing = blogLikeRepository.findByUserIdAndBlog(userId, blog);
        boolean liked;
        if (existing.isPresent()) {
            blogLikeRepository.delete(existing.get());
            blog.setLikesCount(Math.max(0, blog.getLikesCount() - 1));
            liked = false;
        } else {
            blogLikeRepository.save(BlogLike.builder().userId(userId).blog(blog).build());
            blog.setLikesCount(blog.getLikesCount() + 1);
            liked = true;
        }
        blogRepository.save(blog);
        if (liked && blog.getUserId() != null) {
            try {
                notificationService.create(userId, blog.getUserId(), NotificationType.BLOG_LIKE,
                    "BLOG", blogId.toString(), blog.getSlug());
            } catch (Exception ignored) {}
        }
        return new SocialToggleResponse(liked, null, null, blog.getLikesCount(), blog.getSavesCount(), null);
    }

    public SocialToggleResponse toggleBlogSave(String userId, UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<BlogSave> existing = blogSaveRepository.findByUserIdAndBlog(userId, blog);
        boolean saved;
        if (existing.isPresent()) {
            blogSaveRepository.delete(existing.get());
            blog.setSavesCount(Math.max(0, blog.getSavesCount() - 1));
            saved = false;
        } else {
            blogSaveRepository.save(BlogSave.builder().userId(userId).blog(blog).build());
            blog.setSavesCount(blog.getSavesCount() + 1);
            saved = true;
        }
        blogRepository.save(blog);
        return new SocialToggleResponse(null, saved, null, blog.getLikesCount(), blog.getSavesCount(), null);
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse getBlogSocialStatus(String userId, UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            blogLikeRepository.existsByUserIdAndBlog(userId, blog),
            blogSaveRepository.existsByUserIdAndBlog(userId, blog),
            false
        );
    }
}
