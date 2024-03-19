package com.architecture.admin.models.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class BadgeDto {

    /**
     * badge
     **/
    private Integer idx;        //badge.idx
    private String code;        // 배지 코드
    private String name;        //배지 이름
    private Integer state;      //상태값
    private String regdate;     //등록일
    private String regdateTz;   //등록일 타임존

}