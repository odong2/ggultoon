package com.architecture.admin.models.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ContentDto {

    /**
     * contents
     **/
    private Integer idx;                //작품번호
    private Integer categoryIdx;        //category.idx
    private Integer genreIdx;           //genre.idx
    private String id;                  //아이디
    private String title;               //제목
    private String description;         //설명
    private Long lastEpisodeIdx;        //마지막 회차 idx
    private String lastEpisodeTitle;    //마지막 회차 제목
    private Integer adult;              // 성인 컨텐츠(0:전체이용가, 1:성인)
    private Integer adultPavilion;      // 성인관 유무(0: 일반, 1: 성인관)
    private Integer completeTypeIdx;    //complete_type.idx
    private Integer progress;           //진행상황(1:연재, 2:휴재, 3:완결)
    private Integer exclusive;          //독점 컨텐츠(0:비독점, 1:독점)
    private Integer publication;        //단행본(0:비단행본, 1:단행본)
    private Integer revision;           //개정판(0:비개정판, 1:개정판)
    private Integer sellType;           //판매종류(1:소장/대여,2:소장,3:대여)
    private Integer state;              //상태값
    private String label;               //레이블
    private String code;                //제품코드
    private String pubdate;             //발행일
    private String pubdateTz;           //발행일 타임존
    private String regdate;             //등록일
    private String regdateTz;           //등록일 타임존

    /**
     * member
     */
    private Long memberIdx;     // 회원 idx

    /**
     * category
     **/
    private String category;        //카테고리 이름(웹툰, 만화, 소설, 성인)

    /**
     * genre
     **/
    private String genre;           //장르 이름(무협,액션,판타지,드라마,로맨스,스포츠 등)

    /**
     * complete_type
     **/
    private String completeType;        //완결 타입(완결, 시즌완결, 외전완결 등)

    /**
     * contents_info
     **/
    private Integer contentsIdx;        // contents.idx
    private Integer view;               // 조회 수
    private Integer viewDummy;          // 뷰카운트 더미(관리용 조회수)
    private Integer favorite;           // 관심 수
    private String rating;              // 별점
    private Integer purchase;           // 구매 수

    /**
     * ranking
     **/
    private Integer ranking;            // 랭킹 순위
    private Integer rankingPrev;        // 이전 랭킹 순위
    private Integer variance;           // 랭킹 순위 변화
    private String effectiveDate;       // 랭킹 기준 시간
    private Integer episodeCount;       // 구매수

    /**
     * episode
     */
    private Long episodeIdx;            // episode.idx
    private Integer episodeNumber;      // 회차 번호
    private String lastEpisodeNumber;   // 마지막 회차 번호
    private String contentsPubdate;     // 컨텐츠 출시일
    private String episodePubdate;      // 회차 출시일
    private Integer firstEpisodeIdx;    // 첫번째 회차 번호

    /**
     * episode_event_coin
     */
    private Integer discountEpisodeCnt;    // 이벤트 할인 회차 개수
    private String episodeEventStartDate;  // 회차 이벤트 시작일
    private String episodeEventEndDate;    // 회차 이벤트 종료일

    /**
     * curation
     **/
    private Integer contentsSort;       // 큐레이션 내 작품 노출 순서
    private Integer curationIdx;        // 큐레이션 번호
    private String curationTitle;       // 큐레이션 제목
    private Long areaIdx;               // 큐레이션 노출 영역(1:일반관 메인, 2:성인관 메인, 3:검색, 4: 회차리스트, 5:내서재)
    private String areaText;            // 큐레이션 노출 영역 문자 변환
    private String areaCode;            // 큐레이션 노출 영역 코드
    private Integer curationSort;       // 큐레이션 정렬 순서
    private Integer curationState;      // 상태값

    /**
     * contents_event_free
     **/
    private Integer freeEpisodeCnt;         // 무료 회차 수
    private Integer eventFreeEpisodeCnt;    // 이벤트 무료 회차 수
    private Integer eventFreeUsed;          // 이벤트 무료 회차 제공 여부(0: 제공X, 1: 제공)
    private String contentsEventStartDate;  // 작품 이벤트 시작일
    private String contentsEventEndDate;    // 작품 이벤트 종료일

    /**
     * content_event_free
     */
    private Integer freeCnt;            // 무료 회차 수
    private Integer eventFreeCnt;       // 이벤트 무료 회차 수
    private Integer minPurchaseCnt;     // 전체 소장 최소 구매 수 
    private Integer discount;           // 전체 소장 할인율
    private Integer minPurchaseRentCnt; // 전체 대여 최소 구매 수
    private Integer discountRent;       // 전체 대여 할인율

    /**
     * 더미 데이터
     */
    private String device;      //디바이스 정보

    /**
     * 기타 (프론트단 데이터 형식 맞추기 위해 사용)
     */
    private String contentsTitle;   // 컨텐츠 제목 -> 내 서재에서 사용
    private Integer type;           // 4(관심) -> 내 서재 사용
    private String typeText;        // 관심 -> 내 서재 사용
    private String episodeNumTitle; // 회차 번호 텍스트
    private String nowDate;         // 현재 날짜 및 시간
    private Integer pavilionIdx;    // 이용관 정보(0:일반관 / 1:성인관)

    /**
     * 기타
     */
    private Boolean isNextEpisode;          // 다음 회차 존재 여부
    private Integer nextEpisodeNumber;      // 다음 회차 번호
    private Long nextEpisodeIdx;            // 다음 회차 idx
    private Boolean isNextEpRent;           // 다음 회차 대여 여부(내 서재)
    private Boolean isNextEpHave;           // 다음 회차 소장 여부(내 서재)
    private Boolean isNowEpPurchase;        // 현재 회차 구매여부(true: 구매함, false: 구매 안함)
    private Boolean isNowEpFree;            // 현재 회차 무료 여부(true: 무료, false: 유료)
    private Boolean isNowEpRentFree;        // 현재 회차 대여 무료 여부
    private Boolean isNowEpHaveFree;        // 현재 회차 소장 무료 여부
    private Boolean isNextEpFree;           // 다음 회차 무료 여부(true: 무료, false: 유료)
    private Boolean isNextEpRentFree;       // 다음 회차 대여 무료 여부
    private Boolean isNextEpHaveFree;       // 다음 회차 소장 무료 여부
    private Boolean isMemberLike;           // 회원의 작품 찜 여부(true: 찜한 작품, false: 찜하지 않은 작품)
    private Integer isMemberAdult;          // 회원 성인 여부
    private Boolean haveFreeEpisodes;       // 이벤트 무료 회차 유무

    /**
     * List
     */
    private List<ContentImgDto> contentHeightImgList;   // 컨텐츠 세로 이미지 리스트
    private List<ContentImgDto> contentWidthImgList;    // 컨텐츠 가로 이미지 리스트
    private List<AuthorDto> writerList;                 // 글작가 리스트
    private List<AuthorDto> painterList;                // 그림작가 리스트
    private List<TagDto> tagList;                       // 태그 리스트
    private List<BadgeDto> badgeList;                   // 배지 리스트
    private List<ContentDto> contentList;               // 큐레이션에 들어간 작품 리스트

    // sql
    private Integer insertedIdx;
    private Integer affectedRow;
}
