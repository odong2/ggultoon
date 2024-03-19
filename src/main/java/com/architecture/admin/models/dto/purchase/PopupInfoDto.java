package com.architecture.admin.models.dto.purchase;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

/**
 * 프런트단에 값 실어주기 위해 사용
 * 소장, 대여 팝업 정보
 */
public class PopupInfoDto {

    private Integer totalHaveCoin;   // 전체 소장 코인
    private Integer totalRentCoin;   // 전체 대여 코인
    private String totalEpisodeCnt; // 전체 회차 수
    private String haveCnt;         // 소장할 회차 수
    private String rentCnt;         // 대여할 회차 수
    private boolean hasDiscount;     // 할인 여부
    private Integer disCountPercent; // 할인율
    private Integer disCountHaveCoin;// 할인된 금액
}
