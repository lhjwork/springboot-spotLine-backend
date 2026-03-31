package com.spotline.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 30, message = "닉네임은 1~30자여야 합니다")
    private String nickname;

    @Size(max = 200, message = "자기소개는 200자 이하여야 합니다")
    private String bio;

    @Size(max = 50, message = "인스타그램 ID는 50자 이하여야 합니다")
    private String instagramId;
}
