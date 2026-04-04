package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.CommentTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "댓글 작성 요청")
public class CreateCommentRequest {

    @NotNull(message = "대상 타입은 필수입니다")
    private CommentTargetType targetType;

    @NotNull(message = "대상 ID는 필수입니다")
    private UUID targetId;

    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 500, message = "댓글은 500자 이내로 작성해주세요")
    private String content;

    private UUID parentId;
}
