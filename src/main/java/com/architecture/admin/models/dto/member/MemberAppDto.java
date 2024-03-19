package com.architecture.admin.models.dto.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberAppDto {
    private Long idx;             // 고유번호
    private Long memberIdx;       // 회원번호
    private String token;         // 토큰
    private String adid;          // 광고아이디
    private String modifyDate;    // 등록일
    private String modifyDateTz;  // 등록일 타임존
    private String regdate;       // 등록일
    private String regdateTz;     // 등록일 타임존
}
