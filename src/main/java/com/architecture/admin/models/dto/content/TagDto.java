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
public class TagDto {

    /**
     * tag
     **/
    private Integer idx;            //tag.idx
    private Integer tagGroupIdx;    //태그 그룹 idx
    private String name;            //태그명
    private Integer state;          //상태값
    private String regdate;         //등록일
    private String regdateTz;       //등록일 타임존

    /**
     * tag_mapping 테이블
     */
    private Integer contentsIdx;    // 등록일 타임존

}