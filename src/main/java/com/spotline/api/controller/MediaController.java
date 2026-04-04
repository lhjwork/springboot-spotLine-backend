package com.spotline.api.controller;

import com.spotline.api.dto.request.PresignedUrlRequest;
import com.spotline.api.dto.response.PresignedUrlResponse;
import com.spotline.api.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Media", description = "미디어 업로드 (S3 Presigned URL)")
@RestController
@RequestMapping("/api/v2/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "S3 업로드용 Presigned URL 생성")
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(mediaService.generatePresignedUrl(request));
    }

    @Operation(summary = "S3 미디어 삭제")
    @DeleteMapping
    public ResponseEntity<Void> deleteMedia(@RequestParam String s3Key) {
        mediaService.deleteMedia(s3Key);
        return ResponseEntity.noContent().build();
    }
}
