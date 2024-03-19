package com.architecture.admin.models.dto.coin;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoinDto {

    private Long idx;
    private Long memberIdx;      // 회원 번호
    private String id;           // 회원 아이디
    private String nick;         // 회원 닉네임
    private Integer paymentIdx;  // 주문 번호
    private Long achievementIdx; // 업적 번호
    private Integer productIdx;  // 상품 번호
    private Integer coin = 0;    // 유료 코인
    private Integer type;        // 코인 유형(1: 코인, 2: 보너스 코인, 3: 마일리지)
    private Integer coinFree = 0;// 보너스 코인 (결제 시 추가지급 코인)
    private Integer mileage = 0; // 마일리지 (업적 달성시 지급되는 코인)
    private Integer usedCoin;    // 사용 코인
    private Integer restCoin;    // 남은 코인
    private Integer usedCoinFree;// 사용 보너스 코인
    private Integer restCoinFree;// 남은 보너스 코인
    private Integer usedMileage; // 사용 마일리지
    private Integer restMileage; // 남은 마일리지
    private Integer usedTicket = 0;  // 사용 이용권
    private Integer restTicket = 0;  // 남은 이용권
    private Integer ticket = 0;      // 이용권 개수
    private Integer period;      // 이용권 유효 기간(대여 시간)
    private String position;     // 지급 위치
    private Integer state;       // 상태
    private String expiredate;   // 만료일
    private String expireDateTz; // 만료일 타임존
    private String regdate;
    private String regdateTz;

    // 코인 차감시 이용
    private String coinType;      // coin, coinFree, mileage
    private Integer subResultCoin; // 차감된 결과 코인
    private Long mileageSaveIdx;  // member_mileage_save.idx

    /**
     * episode
     */
    private Integer coinRent; // 대여 코인

    /**
     * 만료일
     */
    private String mileageExpireDate;
    private String coinExpireDate;
    private String coinFreeExpireDate;

    /**
     * contents
     */
    private String title;

    /**
     * 리스트 구별용 (이용 내역 전체 조회 시 사용) - 코인 및 마일리지 구별
     */
    private String searchType;    // coin , mileage
    /**
     * 상태 문자열
     */
    private String typeText; // 코인, 보너스 코인

    /**
     * 프론트 단에 변환해서 넘겨줄 값
     */
    private Integer value; // mileage, coin, coinFree
    private Integer expireType;    // 1: 소멸, 2: 소멸 예정
    private String expireTypeText; // 소멸, 소멸 예정

    /**
     * 기타
     */
    private Long memberCoinSaveIdx;
    private Long mileageUsedIdx;
    private Long coinUsedIdx;
    private String nowDate;

}
