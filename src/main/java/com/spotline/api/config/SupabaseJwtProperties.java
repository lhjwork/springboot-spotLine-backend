package com.spotline.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseJwtProperties {
    private String url;
    private String anonKey;
    private String jwtSecret;
}
