package com.architecture.admin.models.dto.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberOttDto {
    private Long idx;           // 고유번호
    private Long memberIdx;     // 회원번호
    private String id;          // 꿀툰아이디
    private String ottId;       // OTT아이디
    private String ci;          // 인증정보
    private String site;        // OTT사이트
    private String bannerCode;  // 배너코드
    private String eventType;   // 이벤트 타입
    private String coupon;      // 쿠폰
    private String sendMsg;     // 보낸메세지
    private String returnMsg;   // 결과메세지
    private String returnUrl;   // 결과URL
    private String regdate;     // 등록일
    private String regdateTz;   // 등록일 타임존
    private String today;       // 통계일
    private Integer visit=0;    // 접속
    private Integer join=0;     // 가입
    private Integer point=0;    // 포인트
    private Integer couponCnt=0;// 쿠폰수
    private Integer state;      // 상태
}
