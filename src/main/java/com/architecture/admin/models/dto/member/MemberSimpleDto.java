package com.architecture.admin.models.dto.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberSimpleDto {
    private Long idx;
    private Long memberIdx;    // 회원 idx
    private String simpleType; // 간편가입 타입(google, naver, kakao 등)
    private String authToken;  // 가입 토큰
}
