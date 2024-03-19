package com.architecture.admin.models.dto.community;

import com.architecture.admin.models.dto.content.AuthorDto;
import com.architecture.admin.models.dto.content.ContentImgDto;
import com.architecture.admin.models.dto.content.TagDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityContentDto {

    private Long idx;               // 커뮤니티 게시물 idx
    private Integer contentsIdx;    // 컨텐츠 idx
    private Integer categoryIdx;    // 카테고리 idx
    private Long memberIdx;         // 회원 idx
    private String title;           // 제목
    private String content;         // 내용
    private Integer adult;          // 성인(0: 전체 이용, 1: 성인)
    private Integer state;          // 상태값
    private String regdate;         // 등록일
    private String modifyDate;      // 수정일
    private String modifyDateTz;    // 수정일 타임존

    /**
     * community_contents_info 테이블
     */

    private Integer viewCnt;        // 조회 수
    private Integer likeCnt;        // 좋아요 수
    private Integer commentCnt;     // 댓글 수
    private Integer reportCnt;      // 신고 수

    /**
     * 이미지
     */
    private List<MultipartFile> uploadFiles;   // 앞단에서 받는 이미지
    
    private List<CommunityImageDto> imageList; // 이미지 리스트

    /**
     * member
     */
    private String nick;                      // 작성자 닉네임

    /**
     * community_category
     */
    private String category;                  // 카테고리 이름

    /**
     * 기타
     */
    private String nowDate; // UTC 기준 현재시간
    private Boolean isMemberLike;              // 게시물 좋아요 여부 (true: 좋아요 o , false: 좋아요 X)
    private Integer memberLike;                // 회원 게시물 좋아요 여부(0: 좋아요 안함, : 1: 좋아요 함)

    /**
     * sql
     */
    private Long insertedIdx;

    /**
     * List
     */
    private List<ContentImgDto> contentHeightImgList;   // 컨텐츠 세로 이미지 리스트
    private List<AuthorDto> writerList;                 // 글작가 리스트
    private List<AuthorDto> painterList;                // 그림작가 리스트
    private List<TagDto> tagList;                       // 태그 리스트
}
