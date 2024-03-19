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
public class PurchaseBuyAllDto {

    private Long idx;              // 회원 전체 회차 구매 idx
    private Long memberIdx;        // 회원 idx
    private Integer contentsIdx;   // 컨텐츠 idx
    private Integer totalCoin;     // 전체 코인
    private Integer episodeCount;  // 전체 소장 or 대여 회차 수
    private Integer type;          //  구매 유형(1:대여, 2:소장)
    private Integer state;         // 상태값 
    private String regdate;        // 등록일
    private String expiredate;     // 만료일

    /**
     * SQL
     */
    private Long insertedIdx;

}
