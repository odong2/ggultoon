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
public class AuthorDto {

    /**
     * author
     **/
    private Integer authorIdx;  //author.idx
    private Integer authorType; //작가 구분(1:글, 2:그림)
    private String name;        //작가 이름
    private Integer state;      //상태값
    private String regdate;     //등록일
    private String regdateTz;   //등록일 타임존

}