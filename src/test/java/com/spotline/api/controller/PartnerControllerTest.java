package com.spotline.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import com.spotline.api.dto.request.CreatePartnerRequest;
import com.spotline.api.dto.request.CreateQrCodeRequest;
import com.spotline.api.dto.response.PartnerAnalyticsResponse;
import com.spotline.api.dto.response.PartnerQrCodeResponse;
import com.spotline.api.dto.response.PartnerResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.security.JwtTokenProvider;
import com.spotline.api.service.PartnerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PartnerController.class)
class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PartnerService partnerService;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID partnerId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/v2/admin/partners — 파트너 등록 201")
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        CreatePartnerRequest request = new CreatePartnerRequest();
        request.setSpotId(UUID.randomUUID());
        request.setTier(PartnerTier.BASIC);
        request.setContractStartDate(LocalDate.now());
        request.setContractEndDate(LocalDate.now().plusYears(1));

        PartnerResponse response = PartnerResponse.builder()
                .id(partnerId)
                .spotTitle("카페 어니언")
                .status(PartnerStatus.ACTIVE)
                .tier(PartnerTier.BASIC)
                .build();

        given(partnerService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/v2/admin/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(partnerId.toString()));
    }

    @Test
    @DisplayName("GET /api/v2/admin/partners — 파트너 목록 200")
    @WithMockUser(roles = "ADMIN")
    void list_returnsOk() throws Exception {
        PartnerResponse response = PartnerResponse.builder()
                .id(partnerId)
                .spotTitle("카페 어니언")
                .status(PartnerStatus.ACTIVE)
                .build();
        Page<PartnerResponse> page = new PageImpl<>(List.of(response));
        given(partnerService.list(any(), any())).willReturn(page);

        mockMvc.perform(get("/api/v2/admin/partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(partnerId.toString()));
    }

    @Test
    @DisplayName("GET /api/v2/admin/partners/{id} — 파트너 상세 200")
    @WithMockUser(roles = "ADMIN")
    void getById_returnsOk() throws Exception {
        PartnerResponse response = PartnerResponse.builder()
                .id(partnerId)
                .spotTitle("카페 어니언")
                .build();
        given(partnerService.getById(partnerId)).willReturn(response);

        mockMvc.perform(get("/api/v2/admin/partners/" + partnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotTitle").value("카페 어니언"));
    }

    @Test
    @DisplayName("POST /api/v2/admin/partners/{id}/qr-codes — QR 코드 생성 201")
    @WithMockUser(roles = "ADMIN")
    void createQrCode_returnsCreated() throws Exception {
        CreateQrCodeRequest request = new CreateQrCodeRequest();
        request.setLabel("입구 QR");

        PartnerQrCodeResponse response = PartnerQrCodeResponse.builder()
                .id(UUID.randomUUID())
                .qrId("partner-cafe-onion-001")
                .label("입구 QR")
                .build();

        given(partnerService.createQrCode(eq(partnerId), any())).willReturn(response);

        mockMvc.perform(post("/api/v2/admin/partners/" + partnerId + "/qr-codes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrId").value("partner-cafe-onion-001"));
    }

    @Test
    @DisplayName("GET /api/v2/admin/partners/{id}/analytics — 분석 200")
    @WithMockUser(roles = "ADMIN")
    void getAnalytics_returnsOk() throws Exception {
        PartnerAnalyticsResponse response = PartnerAnalyticsResponse.builder()
                .partnerId(partnerId)
                .spotTitle("카페 어니언")
                .period("30d")
                .totalScans(100)
                .uniqueVisitors(80)
                .conversionRate(0.8)
                .build();

        given(partnerService.getAnalytics(partnerId, "30d")).willReturn(response);

        mockMvc.perform(get("/api/v2/admin/partners/" + partnerId + "/analytics")
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(100));
    }
}
