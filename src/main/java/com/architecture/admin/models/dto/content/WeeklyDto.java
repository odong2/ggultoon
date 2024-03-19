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
public class WeeklyDto {

    /**
     * weekly
     **/
    private Integer weeklyIdx;      //weekly.idx
    private String name;            //연재요일
    private Integer state;          //상태값
    private String regdate;         //등록일
    private String regdateTz;       //등록일 타임존

}