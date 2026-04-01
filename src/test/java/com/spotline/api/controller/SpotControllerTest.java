package com.spotline.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.response.SpotDetailResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.security.JwtTokenProvider;
import com.spotline.api.service.SpotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpotController.class)
class SpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SpotService spotService;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v2/spots/{slug} - 200 OK")
    @WithMockUser
    void getBySlug_returnsSpot() throws Exception {
        SpotDetailResponse response = SpotDetailResponse.builder()
                .id(UUID.randomUUID())
                .slug("test-cafe")
                .title("테스트 카페")
                .category(SpotCategory.CAFE)
                .source(SpotSource.CREW)
                .area("성수")
                .build();

        given(spotService.getBySlug("test-cafe")).willReturn(response);

        mockMvc.perform(get("/api/v2/spots/test-cafe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-cafe"))
                .andExpect(jsonPath("$.title").value("테스트 카페"));
    }

    @Test
    @DisplayName("GET /api/v2/spots/{slug} - 404 Not Found")
    @WithMockUser
    void getBySlug_notFound_returns404() throws Exception {
        given(spotService.getBySlug("invalid"))
                .willThrow(new ResourceNotFoundException("Spot", "invalid"));

        mockMvc.perform(get("/api/v2/spots/invalid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Spot을(를) 찾을 수 없습니다: invalid"));
    }

    @Test
    @DisplayName("POST /api/v2/spots - 201 Created")
    @WithMockUser
    void create_returnsCreated() throws Exception {
        CreateSpotRequest request = new CreateSpotRequest();
        request.setTitle("새 카페");
        request.setCategory(SpotCategory.CAFE);
        request.setSource(SpotSource.CREW);
        request.setAddress("서울 성동구");
        request.setLatitude(37.5);
        request.setLongitude(127.0);
        request.setArea("성수");

        SpotDetailResponse response = SpotDetailResponse.builder()
                .id(UUID.randomUUID())
                .slug("sae-kaphe")
                .title("새 카페")
                .category(SpotCategory.CAFE)
                .build();

        given(spotService.create(any(CreateSpotRequest.class), any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v2/spots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 카페"));
    }

    @Test
    @DisplayName("POST /api/v2/spots - 400 Validation 실패")
    @WithMockUser
    void create_validationFails_returns400() throws Exception {
        CreateSpotRequest request = new CreateSpotRequest();
        // title, category, source 누락

        mockMvc.perform(post("/api/v2/spots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v2/spots/{slug} - 204 No Content")
    @WithMockUser
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v2/spots/test-cafe").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
