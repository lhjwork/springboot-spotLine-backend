package com.spotline.api.infrastructure.s3;

import java.util.Set;

public final class MediaConstants {

    private MediaConstants() {}

    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB
    public static final int MAX_MEDIA_PER_SPOT = 10;
    public static final int MAX_VIDEO_DURATION_SEC = 30;

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm"
    );

    public static boolean isImageType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    public static boolean isVideoType(String contentType) {
        return ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    public static boolean isAllowedType(String contentType) {
        return isImageType(contentType) || isVideoType(contentType);
    }
}
