package com.architecture.admin.libraries.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtDto {
    public JwtDto jwtDto;
    List<JwtDto> jwtDtoList;
    private String grantType;
    private String secretKeyType;
    private String accessToken;
    private String refreshToken;
    private String key;
    private Long refreshTokenId;
    private String keyEmail;
    private long validTime = 24 * 60 * 60 * 7 * 1000L;
    private long validTimeRefresh = 24 * 60 * 60 * 7 * 1000L; // 유효시간 도 인자값 가능 24 * 60 * 60 * 1000L : 일주일
    //JWT 토큰 쿠키 유효시간
    private int cookieTime = 60 * 60 * 24 * 7;
}