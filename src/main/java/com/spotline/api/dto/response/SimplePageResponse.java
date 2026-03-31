package com.spotline.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
public class SimplePageResponse<T> {
    private List<T> items;
    private boolean hasMore;

    public static <T> SimplePageResponse<T> from(Page<T> page) {
        return new SimplePageResponse<>(page.getContent(), page.hasNext());
    }
}
