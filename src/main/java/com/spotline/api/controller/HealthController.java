package com.spotline.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Health", description = "헬스체크")
@RestController
public class HealthController {

    @Operation(summary = "서버 상태 확인")
    @SecurityRequirements
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Spotline API is running",
                "timestamp", Instant.now().toString(),
                "version", "2.0.0",
                "stack", "Spring Boot + PostgreSQL + Supabase"
        ));
    }
}
