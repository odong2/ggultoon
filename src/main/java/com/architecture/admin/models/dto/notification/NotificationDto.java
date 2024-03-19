package com.architecture.admin.models.dto.notification;

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
public class NotificationDto {

    /**
     * member_notification
     **/
    private Long idx;                   // 알림 idx
    private Long memberIdx;             // member.idx
    private String category;            // 알림 카테고리
    private String type;                // 알림 보낼 테이블명
    private Long typeIdx;               // 알림 보낼 테이블의 idx
    private String title;               // 알림 내용 변수
    private String url;                 // 알림 링크 url
    private Integer state;              // 상태값
    private String regdate;             // 전송일
    private String regdateTz;           // 전송일 타임존
    private String checkDate;           // 확인일
    private String checkDateTz;         // 확인일 타임존
    private String delDate;             // 삭제일
    private String delDateTz;           // 삭제일 타임존

    /**
     * 기타
     **/
    private Boolean isChecked;           // 알림 확인 여부

    // list
    List<Long> idxList;                  // 선택한 알림 목록
}
