package com.architecture.admin.models.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentNotiDto {
    /**
     * 결제 완료 결과값
     **/
    private String outStatCd;       // 거래상태
    private String trdNo;           // 거래번호
    private String method;          // 결제수단 신용카드[CA]
    private String bizType;         // 업무구분 승인[B0], 취소[C0]
    private String mchtId;          // 상점아이디
    private String mchtTrdNo;       // 상점주문번호
    private String mchtCustNm;      // 상점한글명
    private String pmtprdNm;        // 상품명
    private String trdDtm;          // 거래일시
    private String trdAmt;          // 거래금액
    private String billKey;         // 자동결제키
    private String billKeyExpireDt; // 자동결제키 유효기간
    private String cardCd;          // 카드사코드
    private String cardNm;          // 카드명
    private String bankCd;          // 은행코드
    private String bankNm;          // 은행명
    private String telecomCd;       // 이통사코드
    private String telecomNm;       // 이통사명
    private String vAcntNo;         // 가상계좌번호
    private String expireDt;        // 가상계좌 입금만료일시
    private String AcntPrintNm;     // 통장인자명
    private String dpstrNm;         // 입금자명
    private String email;           // 고객이메일
    private String mchtCustId;      // 상점고객아이디
    private String cardNo;          // 카드번호
    private String cardApprNo;      // 카드승인번호
    private String instmtMon;       // 할부개월수
    private String instmtType;      // 할부타입
    private String orgTrdNo;        // 원거래번호
    private String orgTrdDt;        // 원거래일자
    private String mixTrdNo;        // 복합결제 거래번호
    private String mixTrdAmt;       // 복합결제 금액
    private String payAmt;          // 실 결제금액
    private String cnclType;        // 취소거래타입	00:전체 취소, 10:부분 취소
    private String mchtParam;       // 상점예약필드
    private String pktHash;         // 해쉬값
    private String acntType;        // 기타주문정보
    private String kkmAmt;          // 기타주문정보
    private String coupAmt;         // 기타주문정보
    private String ezpDivCd;        // 간편결제코드 카카오페이[KKP],네이버페이[NVP],페이코[PAC]
}