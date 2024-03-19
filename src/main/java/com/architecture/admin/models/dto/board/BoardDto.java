package com.architecture.admin.models.dto.board;

import com.architecture.admin.models.dto.content.BadgeDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardDto {

    private Long idx;               // 공지사항 idx
    private Integer type;           // 타입 구분값(1: TEXT, 2: HTML)
    private String typeText;        // 타입 구분값 문자변환(1: TEXT, 2: HTML)
    private String title;           // 제목
    private String content;         // 내용
    private Integer mustRead;       // 필독 여부(0: 해당없음, 1: 필독)
    private Integer state;          // 상태값
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존

    // list
    List<BadgeDto> badgeList;       // 배지 리스트
}
