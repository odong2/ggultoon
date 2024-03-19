package com.architecture.admin.models.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDto {

    /**
     * Payment 테이블 컬럼
     **/
    private Long idx;           // payment.idx
    private Integer productIdx; // product.idx
    private Long memberIdx;     // member.idx
    private Integer coin;       // 지급된 코인
    private Integer coinFree;   // 지급된 보너스 코인
    private Integer mileage;    // 지급된 마일리지
    private String orderNo;     // 주문 번호
    private String pay;         // 결제 금액
    private String currency;    // 사용 화폐
    private String payMethod;   // 결제 수단(mobile, pc, android, ios)
    private String payType;     // 결제 타입(mobile, card, culture)
    private Integer state;      // 상태(0: 취소, 1: 정상)
    private String cpId;        // 상점코드
    private String tid;         // 거래번호
    private String provider;    // 결제업체 통신사, 카드사 은행
    private String regdate;     // 결제일
    private String moddate;     // 취소일
    private Integer first;      // 첫결제 유무

    /**
     * product 테이블 컬럼
     **/
    private String title;       // 상품명

    /**
     * Member 테이블 컬럼
     **/
    private String id;          // member.id(이메일)
    private String nick;        // member.nick

    /**
     * member_mileage_save
     */
    private Long paymentIdx;     // 결제 idx
    private Long achievementIdx; // 업적 idx
    private String position;     // 지급 위치(업적 결제 이벤트 등)

    /**
     * 리스트 구별용 (이용 내역 전체 조회  시 사용) - 결제 및 마일리지
     */
    private String searchType;   //  payment, mileage
    /**
     * Text
     */
    private String stateText;    // 정상, 취소
    private String payTypeText;  // 결제 타입
    
    // sql
    private Integer insertedId;  // 입력된 idx
    private Integer affectedRow; // 처리 row 수


}
