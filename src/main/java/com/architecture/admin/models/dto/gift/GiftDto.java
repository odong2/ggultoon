package com.architecture.admin.models.dto.gift;

import com.architecture.admin.models.dto.content.BadgeDto;
import com.architecture.admin.models.dto.content.ContentImgDto;
import com.architecture.admin.models.dto.content.TagDto;
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
public class GiftDto {

    /**
     * contents_ticket
     **/
    private Long idx;               // 작품 이용권 idx
    private Integer contentsIdx ;   // 작품 idx
    private Integer exceptCnt;      // 최신 회차 수(이용권 지급 대상 제외)
    private Integer usePeriod;      // 이용권 사용 제한 시간
    private Integer ticketCnt;      // 이용권 지급 개수
    private Integer adult;          // 지급 대상 연령(0:전체 / 1:성인)
    private String adultText;       // 지급 대상 연령 문자변환
    private String startDate;       // 이용권 사용 가능 기간 시작일
    private String startDateTz;     // 이용권 사용 가능 기간 시작일 타임존
    private String endDate;         // 이용권 사용 가능 기간 종료일
    private String endDateTz;       // 이용권 사용 가능 기간 종료일 타임존
    private Integer state;          // 상태값
    private String stateText;       // 상태값 문자변환
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존
    private String convertDateText; // 이용권 사용 가능 기간 텍스트 변환

    /**
     * contents & contents_img
     * tag & tag_mapping
     **/
    private String contentsTitle;   // 작품 제목
    private Integer categoryIdx;    // 카테고리 idx
    private List<ContentImgDto> contentHeightImgList; // 작품 세로 이미지 리스트(선물함 모달 노출용)
    private List<ContentImgDto> contentWidthImgList; // 작품 이미지 리스트(선물함 페이지용)
    private List<TagDto> tagList;       // 작품 태그 리스트
    private List<BadgeDto> badgeList;   // 작품 배지 리스트
    private Integer contentsAdult;  // 성인 컨텐츠(0:전체이용가, 1:성인)
    private Integer progress;       // 진행상황(1:연재, 2:휴재, 3:완결)
    private Integer exclusive;      // 독점 컨텐츠(0:비독점, 1:독점)
    private Integer publication;    // 단행본(0:비단행본, 1:단행본)
    private Integer revision;       // 개정판(0:비개정판, 1:개정판)
    private String contentsPubdate; // 작품 발행일
    private String episodePubdate;  // 회차 발행일

    /**
     * contents_ticket_group_mapping
     **/
    private Long mappingIdx;        // 작품 이용권 - 지급 대상 그룹 매핑 idx

    /**
     * contents_ticket_group
     **/
    private Long groupIdx;          // 지급 대상 그룹 idx
    private String code;            // 그룹 코드
    private String name;            // 그룹 이름
    private String description;     // 그룹 설명

    /**
     * member_contents_ticket_save
     **/
    private Long saveIdx;           // 지급 내역 idx
    private Long memberIdx;         // member.idx
    private Long paymentIdx;        // payment.idx

    /**
     * member_contents_ticket_used
     */
    private Long usedIdx;           // 사용 내역 idx
    private Integer restCnt;        // 이용권 잔여 개수
    private String expireDate;      // 만료일
    private String expireDateTz;    // 만료일 타임존

    /**
     * member_contents_ticket_save_log
     * member_contents_ticket_used_log
     */
    private String title;           // 지급 OR 사용 내용

    /**
     * contents_ticket_stat
     */
    private Integer giveCnt;        // 지급 개수
    private Integer useCnt;         // 사용 개수

    /**
     * episode
     */
    private List<Long> exceptEpisodeIdxList;    // 이용권 사용 불가 회차(최신 회차) 리스트

    /**
     * member_info
     **/
    private String memberCi;        // 회원 CI 정보

    /**
     * 기타
     */
    private String nowDate;         // 현재 시간
    private Integer minExceptCnt;   // 최신 회차 개수 최소값(이용권 지급 대상 제외)
    private Boolean available;      // 지금 사용 가능 여부
    private String searchDateType;  // 날짜 검색 유형
}
