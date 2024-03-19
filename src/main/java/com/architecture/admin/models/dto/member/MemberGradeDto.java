package com.architecture.admin.models.dto.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberGradeDto {
    private Long idx;                // member.idx
    private Long memberIdx;          // member.idx
    private Integer amount;          // 결제 금액
    private Integer grade;           // 등급
    private Integer addMileage;      // 결제 추가 마일리지
    private Integer payback;         // 페이백

    // 등급 최소 금액
    private Integer condition;

}
