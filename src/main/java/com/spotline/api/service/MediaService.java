package com.spotline.api.service;

import com.spotline.api.domain.enums.MediaType;
import com.spotline.api.dto.request.PresignedUrlRequest;
import com.spotline.api.dto.response.PresignedUrlResponse;
import com.spotline.api.infrastructure.s3.MediaConstants;
import com.spotline.api.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final S3Service s3Service;

    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        validateMedia(request);

        String s3Key = s3Service.generateS3Key(request.getFilename());
        String uploadUrl = s3Service.generatePresignedUploadUrl(s3Key, request.getContentType());
        String publicUrl = s3Service.getPublicUrl(s3Key);

        MediaType mediaType = MediaConstants.isImageType(request.getContentType())
                ? MediaType.IMAGE : MediaType.VIDEO;

        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .s3Key(s3Key)
                .publicUrl(publicUrl)
                .mediaType(mediaType)
                .build();
    }

    public void deleteMedia(String s3Key) {
        s3Service.deleteObject(s3Key);
    }

    private void validateMedia(PresignedUrlRequest request) {
        if (!MediaConstants.isAllowedType(request.getContentType())) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다. 이미지(JPEG, PNG, WebP) 또는 영상(MP4, WebM)만 업로드 가능합니다.");
        }

        if (MediaConstants.isImageType(request.getContentType())) {
            if (request.getContentLength() > MediaConstants.MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("이미지 파일은 10MB 이하만 업로드 가능합니다.");
            }
        } else {
            if (request.getContentLength() > MediaConstants.MAX_VIDEO_SIZE) {
                throw new IllegalArgumentException("영상 파일은 50MB 이하만 업로드 가능합니다.");
            }
            if (request.getDurationSec() != null && request.getDurationSec() > MediaConstants.MAX_VIDEO_DURATION_SEC) {
                throw new IllegalArgumentException("영상은 30초 이하만 업로드 가능합니다.");
            }
        }
    }
}
