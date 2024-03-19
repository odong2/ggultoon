package com.architecture.admin.models.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodDto {
    /**
     * Payment_method 테이블 컬럼
     **/
    private Integer idx;
    private String pgName;          // pg업체명
    private String mchtId;          // 상점아이디
    private String licenseKey;      // 해시체크키
    private String aes256Key;       // 해시체크키
    private String methodType;      // 결제수단 종류
    private String method;          // 결제수단코드
    private String corpPayCode;     // 간편결제코드
    private String methodNoti;      // noti 결제수단코드
    private String mchtName;        // 서비스영문
    private String mchtEName;       // 서비스한글
    private String notiUrl;         // 결제 완료 URL
    private String paymentServer;   // 결제 완료 URL
    private String cancelServer;    // 결제 완료 URL
    private String regdate;         // 등록일

    /**
     * 결제 팝업 정보
     **/
    private String type	= "popup";  // 결제창 타입
    private String trdDt;           // 결제일 ex) 20230416
    private String trdTm;           // 결제시간 ex) 164654
    private String mchtTrdNo;       // 주문번호
    private String pmtPrdtNm;       // 상품명
    private String trdAmt;          // 상품가격
    private String nextUrl;         // 결제상태 표시 URL
    private String cancUrl;         // 결제취소 URL
    private String mchtParam;       // 기타정보 입력필드
    private String mchtCustId;      // 회원아이디
    private String pktHash;         // 암호해시키

}