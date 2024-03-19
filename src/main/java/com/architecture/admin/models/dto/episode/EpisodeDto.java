package com.architecture.admin.models.dto.episode;

import com.architecture.admin.models.dto.content.AuthorDto;
import com.architecture.admin.models.dto.content.BadgeDto;
import com.architecture.admin.models.dto.content.ContentImgDto;
import com.architecture.admin.models.dto.content.WeeklyDto;
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
public class EpisodeDto {

    /**
     * episode
     */
    private Long idx;                   // 웹툰 회차 idx
    private Integer contentIdx;         // contents.idx
    private Integer coin;               // 판매 코인 가격(소장)
    private Integer coinRent;           // 판매 코인 가격(대여)
    private Integer episodeNumber;      // 회차 번호
    private String  episodeNumTitle;    // 회차 번호 문자 변환
    private String title;               // 회차 제목
    private Integer sort;               // 순서
    private Integer completeTypeIdx;    // complete_type.idx
    private Integer episodeTypeIdx;     // episode_type.idx
    private Integer checkLogin;         // 로그인 유무(0:일반, 1:로그인필요)
    private Integer checkArrow;         // 체제방식(0:좌-우, 1:우-좌)
    private Integer commentCnt;         // 댓글 개수
    private Integer state;              // 상태값
    private String pubdate;             // 발행일
    private String pubdateTz;           // 발행일 타임존
    private String regdate;             // 등록일
    private String regdateTz;           // 등록일 타임존
    private String modifydate;          // 수정일
    private String modifydateTz;        // 수정일 타임존

    /**
     * episode_type
     */
    private String typeName;            // 타입명(일반, 프롤로그, 에필로그, 휴재, 공지)

    /**
     * member
     */
    private Long memberIdx;             // 회원 idx

    /**
     * contents
     */
    private String contentsTitle;        // 컨텐츠 제목
    private Integer adult;               // 성인(0: 비성인, 1: 성인)
    private Integer sellType;            // 판매종류(1:소장/대여,2:소장,3:대여)

    /**
     * contents_event_free
     */
    private Integer freeEpisodeCnt;       // 무료 회차 수
    private Integer eventFreeEpisodeCnt;  // 이벤트 무료 회차 수
    private Integer eventFreeUsed;        // 이벤트 무료 사용 여부
    private String startdate;             // 이벤트 시작일
    private String startdateTz;           // 이벤트 시작일 타임존
    private String enddate;               // 이벤트 종료일
    private String enddateTz;             // 이벤트 종료일 타임존

    /**
     * category
     */
    private Integer categoryIdx;         // 카테고리 idx
    private String category;             // 카테고리 이름(name)

    /**
     * genre
     */
    private String genre;                // 장르(name)

    /**
     * episode_info
     */
    private Integer view;               // 조회수
    private String rating;              // 별점

    /**
     * episode_event_coin
     */
    private Integer eventCoin;          // 이벤트(할인) 소장코인
    private Integer eventCoinRent;      // 이벤트(할인) 대여코인

    /**
     * episode_img
     **/
    private String url;             // 이미지 url
    private Integer width;          // 이미지 가로 사이즈
    private Integer height;         // 이미지 세로 사이즈

    /**
     * member_purchase
     */
    private Integer type;               // 구매 타입(1:대여 / 2:소장)
    private String expiredate;          // 구매 만료일
    private String expiredateTz;        // 구매 만료일 타임존

    /**
     * member_contents_ticket_used
     */
    private Integer restTicketCnt;      // 해당 작품에서 사용 가능한 이용권 잔여 개수

    /**
     * 더미 데이터
     */
    private String device;               // 요청 기기정보
    private String route;                // 진입 경로 (rent : 대여 / have : 소장 / null : 알수없음)

    /**
     * 기타
     */
    private String nowDate;                 // UTC 기준 현재 시간
    private Boolean isMemberView;           // 최근 본 회차 여부
    private Boolean isMemberRent;           // 회차 대여 여부
    private Boolean isMemberHave;           // 회차 소장 여부
    private Boolean isMemberPurchase;       // 대여 소장 포함 여부
    private Boolean isEpisodeFree;          // 무료 회차 여부
    private Boolean isEpisodeEventFree;     // 이벤트 무료 회차 여부
    private Boolean isEpisodeEventDiscount; // 이벤트 할인 회차 여부
    private Boolean isEpisodeTicketFree;    // 무료 이용권 사용 가능 회차 여부
    private String convertExpireDate;       // 대여 후 잔여 사용 가능 기간 표시용
    private Integer buyCoinPrice;           // coin(소장가격), coin_rent(대여가격)공통
    private Boolean includeFree;            // 무료 회차 포함 여부(전체 구매에서 사용)
    private Boolean showViewer;             // 뷰어 바로 실행 가능 여부
    private String lastEpisodeText;         // 최종화 텍스트

    /**
     * List
     */
    private List<Long> idxList;
    private List<AuthorDto> writerList;               // 글작가 리스트
    private List<AuthorDto> painterList;              // 그림작가 리스트
    private List<WeeklyDto> weeklyList;               // 연재 요일 리스트
    private List<BadgeDto> badgeList;                 // 배지 리스트
    private List<ContentImgDto> contentHeightImgList; // 컨텐츠 세로 이미지 리스트
    private List<ContentImgDto> contentWidthImgList;  // 컨텐츠 가로 이미지 리스트
    private List<EpisodeImgDto> episodeWidthImgList;  // 회차 가로 이미지 리스트
}
