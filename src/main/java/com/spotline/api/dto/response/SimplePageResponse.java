package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "간단 페이지네이션 응답")
@Data
@AllArgsConstructor
public class SimplePageResponse<T> {
    private List<T> items;
    private boolean hasMore;

    public static <T> SimplePageResponse<T> from(Page<T> page) {
        return new SimplePageResponse<>(page.getContent(), page.hasNext());
    }
}
