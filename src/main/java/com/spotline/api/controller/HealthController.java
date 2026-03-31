package com.spotline.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

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
