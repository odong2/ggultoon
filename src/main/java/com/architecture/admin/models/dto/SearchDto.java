package com.architecture.admin.models.dto;

import com.architecture.admin.libraries.PaginationLibray;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import javax.validation.constraints.Max;
import java.util.List;

/**
 * 공통 페이징, 검색 Dto
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchDto {

    // 검색 관련 property
    /**
     * idx
     */
    private Long idx;               // idx
    private Long memberIdx;         // member.idx
    private Integer pavilionIdx;    // 이용관 idx
    private Integer categoryIdx;    // 카테고리 idx
    private Integer genreIdx;       // 장르 idx
    private Integer sortType;       // 정렬 타입
    private Integer contentsIdx;    // 컨텐츠 idx
    private Long episodeIdx;        // 에피소드 idx
    private Long communityIdx;      // 커뮤니티 컨텐츠 idx (community_contents_idx)
    private Integer monograph;      // 단행본(0:비단행본, 1:단행본)
    private Integer complete;       // (1:완결, 0 전체)
    private Integer period;         // (0: 실시간, 1: 일간, 2: 주간, 3: 월간

    /**
     * common
     */
    private String searchType;  // 검색 종류
    private String searchDateType;  // 날짜 검색 종류
    private String searchWord;  // 검색어
    private Integer type;       // 유형
    private String device;      // 기기정보
    private String lang;        // 언어
    private Integer adult;      // 성인 여부 (0:비성인 / 1:성인)
    private String isLogin;     // 로그인 여부 (0:비로그인 / 1:로그인)
    private Integer count;      // 개수

    /**
     * 알림 관련
     */
    private Integer unreadCount;// 미확인 개수

    /**
     * 코인 관련
     */
    private String coinType;   // 코인 유형(coin, coinFree, mileage)

    /**
     * 날짜 관련
     */
    private String startDate;     // 시작 날짜
    private String endDate;       // 종료 날짜
    private String nowDate;       // 현재 날짜

    /**
     * 결제 관련
     */
    private String saveType;      // 충전 유형(결제: payment, 마일리지: mileage)

    /**
     * 컨텐츠 관련
     */
    private String contentsTitle; // 컨텐츠 제목

    /**
     * 랭킹 관련
     */
    private Integer rankingType; // 랭킹 타입 = (장르 idx * 100) + (카테고리 idx * 10) + (성인 유무 adultPavilion);

    /**
     * 선물함 관련
     */
    private Integer minExceptCnt;  // 이용권 지급 제외 회차(최신 회차)개수 최소값

    /**
     * selectBox 검색
     */
    private String conditionFirst;
    private String conditionSecond;

    /**
     * list
     */
    private List<Long> idxList;
    private List<Integer> contentsIdxList;

    // 페이징
    private PaginationLibray pagination;  // 페이징
    // 현재 페이지
    @Setter(AccessLevel.PROTECTED)
    private int page;
    // 시작위치
    private int offset;
    // 리스트 갯수
    private int limit;
    @Max(value = 500, message = "{lang.common.exception.record.size.max}")
    @Setter(AccessLevel.PROTECTED)
    private int recordSize;
    // 한 페이지 리스트 수
    // 최대 표시 페이징 갯수
    private int pageSize;

    // default paging
    public SearchDto() {
        this.page = 1;
        // 시작번호
        this.offset = 0;
        // DB 조회 갯수
        this.limit = 10;
        // 한 페이지 리스트 수
        this.recordSize = this.limit;
        // 최대 표시 페이징 갯수
        this.pageSize = 5;
    }

    public int getOffset() {
        return (page - 1) * recordSize;
    }

    /**
     * 레코드 사이즈 setter 재정의
     *
     * @param recordSize
     */
    public void setRecordSize(Integer recordSize) {
        if (recordSize == null || recordSize < 1) {
            this.recordSize = this.limit;
        } else {
            this.recordSize = recordSize;
        }
    }

    /**
     * 페이지 setter 재정의
     *
     * @param page
     */
    public void setPage(Integer page) {
        if (page == null || page < 1) {
            this.page = 1;
        } else {
            this.page = page;
        }
    }
}


