package com.architecture.admin.models.dto.setting;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingDto {

    /**
     * setting
     **/
    private Integer idx;            // 환경설정 IDX
    private String title;           // 환경설정 옵션
    private String description;     // 환경설정 설명
    private Integer state;          // 상태값
    private String stateText;       // 상태값 문자변환
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존
    private String modifyDate;      // 설정 변경일
    private String modifyDateTz;    // 설정 변경일 타임존

    /**
     * member_setting
     **/
    private Long memberIdx;       // member.idx
    private Integer settingIdx;   // setting.idx

}
