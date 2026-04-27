package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "아이템 순서 벌크 변경")
public class UpdateItemOrderRequest {

    @NotEmpty
    @Schema(description = "아이템 순서 목록")
    private List<ItemOrder> items;

    @Data
    @Schema(description = "개별 아이템 순서")
    public static class ItemOrder {
        @NotNull
        @Schema(description = "아이템 ID")
        private UUID id;

        @NotNull
        @Schema(description = "새 순서")
        private Integer itemOrder;
    }
}
