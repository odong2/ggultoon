package com.architecture.admin.models.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocialDto {
    private String code;        // 코드값
    private String state;       // 상태
    private String accessToken; // 소셜에서 받는 토큰 값
    private String txseq;       // 본인인증거래번호
    private String joinType;    // 가입 구분 app,pc,mobile
    private String loginType;   // 로그인 구분 app,pc,mobile

    /**
     * 약관동의
     **/
    @Pattern(regexp = "[Y]", message = "{lang.login.exception.privacy}")
    private String privacy;
    @Pattern(regexp = "[Y]", message = "{lang.login.exception.txseq}")
    private String age;
    private String marketing;

    /**
     * OTT 회원연동 데이터
     */
    private String edata;     //OTT 회원연동 데이터

    /**
     * 자동로그인정보
     */
    private String auto;              // 자동로그인정보
}
