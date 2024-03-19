package com.architecture.admin.models.dto.purchase;

import com.architecture.admin.models.dto.content.*;
import com.architecture.admin.models.dto.episode.EpisodeDto;
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
public class PurchaseDto {
    /**
     * member_purchase
     */
    private Long idx;               // 구매회차 번호
    private Long memberIdx;         // member.idx
    private Integer contentsIdx;    // contents.idx
    private Long episodeIdx;        // episode.idx
    private Integer coin;           // 회차 구매에 사용한 코인
    private Integer coinFree;       // 회차 구매에 사용한 보너스 코인
    private Integer mileage;        // 회차 구매에 사용한 마일리지
    private Integer usedTicket;     // 회차 구매에 사용한 이용권
    private Integer type;           // 유형(1:대여, 2:소장)
    private String title;           // Title(내용)
    private Integer route;          // 구매경로(1: 웹, 2: 앱)
    private String asp;             // asp 구매 구분값(임시)
    private Long buyAllIdx;         // 전체 구매 idx(0:개별 구매)
    private Integer state;          // 상태값
    private String expiredate;      // 만료일
    private String expiredateTz;    // 만료일 타임존
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존

    /**
     * contents
     */
    private String contentsTitle;   // 컨텐츠 제목
    private Long lastEpisodeIdx;    // 마지막 회차 idx
    private String contentsPubdate; // 컨텐츠 출시일
    private String episodePubdate;  // 회차 출시일
    private Integer exclusive;      // 독점 여부
    private Integer adult;          // 성인 여부
    private Integer adultPavilion;  // 성인관 여부
    private Integer progress;       // 작품 완결 여부
    private Integer publication;    // 작품 단행본 여부
    private Integer revision;       // 작품 개정판 여부

    /**
     * episode
     */
    private String episodeTitle;       // 회차 제목
    private Integer buyCoin;           // 회차 판매 코인(episode 테이블에서는 coin)
    private Integer buyCoinRent;       // 회차 대여 코인(episode 테이블에서는 coin_rent)
    private Integer buyCoinPrice;      // 회차 판매,대여 공통
    private Integer episodeNumber;     // 회차 번호
    private Integer sort;              // 회차 순서
    private String lastEpisodeNumber; // 마지막 회차 번호

    /**
     * category
     */
    private String category;        // 카테고리 이름(웹툰, 만화, 소설, 성인)

    /**
     * coin
     */
    private Integer restCoin;     // coin
    private Integer restCoinFree; // coin_free
    private Integer restMileage;  // mileage
    private Integer restTicket;   // ticket
    private Integer coinType;     // 코인 유형(1:코인, 2:보너스 코인)

    /**
     * contents_event_free
     */
    private Integer freeEpisodeCnt; // 무료 회차 개수
    private Integer eventFreeEpisodeCnt; // 이벤트 무료 회차 개수
    private Integer eventFreeUsed; // 이벤트 회차 제공 여부(0:제공 안함, 1:제공)
    private String startDate; // 이벤트 시작일
    private String endDate; // 이벤트 종료일

    /**
     * episode_event_coin
     */
    private Integer discountEpisodeCnt;    // 이벤트 할인 회차 개수

    /**
     * genre
     */
    private String genre;          // 장르(name)

    /**
     * event
     */
    private Integer viewCnt;        // 조회수
    private Integer userType;       // 회원 구분(1: OTT 토큰, 2: OTT 가입 회원)

    /**
     * 문자 변환
     */
    private String typeText;        // 구매유형 문자 변환
    private String stateText;       // 정상, 만료
    private String episodeNumTitle; // 회차 번호 텍스트

    /**
     * 더미 데이터
     */
    private String device;         // 구매 경로

    /**
     * 알림 유무
     */
    private Boolean isNotSendAlarm;  // 코인 차감 안내 유무(true: 받지않음, false: 받음)

    /**
     * 기타
     */
    private String nowDate;          // 현재 시간 (내 서재 사용)
    private List<Long> freeIdxList;  // 무료 회차 idx 리스트
    private Boolean includeFree;     // 무료 회차 포함 여부(전체 구매에서 사용)
    private Integer period;          // 유효 기간(선물함 사용)

    /**
     * List
     */
    private List<Long> idxList;
    private List<EpisodeDto> lastEpisodeList;               // 마지막 회차 리스트
    private List<AuthorDto> writerList;                     // 글작가 리스트
    private List<AuthorDto> painterList;                    // 그림작가 리스트
    private List<TagDto> tagList;                           // 태그 리스트
    private List<ContentImgDto> contentHeightImgList;       // 컨텐츠 세로 이미지 리스트
    private List<ContentImgDto> contentWidthImgList;        // 컨텐츠 가로 이미지 리스트
    private List<BadgeDto> badgeList;                       // 배지 리스트

}
