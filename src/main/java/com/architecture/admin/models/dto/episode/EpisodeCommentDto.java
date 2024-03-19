package com.architecture.admin.models.dto.episode;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpisodeCommentDto {

    private Long idx;               // 댓글 idx
    private Long episodeIdx;        // 에피소드 idx
    private Long parentIdx;         // 부모 댓글번호 (댓글이면 0)
    private Long groupIdx;          // 그룹 번호
    private Long memberIdx;         // 회원 idx
    private String content;         // 댓글 내용
    private Integer commentCnt;     // 대댓글 개수
    private Integer replyCnt;       // 대댓글 개수
    private Integer likeCnt;        // 좋아요 개수
    private Integer view;           // 노출 여부 (0: 비노출 , 1:노출) 
    private Integer state;          // 상태값
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존
    private String modifyDate;      // 수정일
    private String modifyDateTz;    // 수정일 타임존

    /**
     * 기타
     */
    private String nowDate;         // UTC 기준 현재시간
    private Boolean isLogin;         // 로그인 여부

    /**
     * 문자 변환
     */
    private String typeText; // 댓글, 대댓글
    private String dateText; // 몇분 전
    private String isModify; // 수정여부

    /**
     * ResultMap
     */
    private String writerNick;    // 댓글 작성한 회원 닉네임
    private Integer memberLike;   // 회원 댓글 좋아요 여부(0: 좋아요 안함, : 1: 좋아요 함)
    private Boolean isMemberLike; // 댓글 좋아요 여부(false : 좋아요 안함, true : 좋아요 함)

}
