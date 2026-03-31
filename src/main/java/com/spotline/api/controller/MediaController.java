package com.spotline.api.controller;

import com.spotline.api.dto.request.PresignedUrlRequest;
import com.spotline.api.dto.response.PresignedUrlResponse;
import com.spotline.api.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(mediaService.generatePresignedUrl(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMedia(@RequestParam String s3Key) {
        mediaService.deleteMedia(s3Key);
        return ResponseEntity.noContent().build();
    }
}
