package com.architecture.admin.libraries.utils;
/**
 * 컨텐츠 관련 상수 관리 클래스(외부 생성 X)
 */
public abstract class ContentUtils {

    // 구매 관련
    public static final int EPISODE_RENT = 1; // 대여
    public static final int EPISODE_HAVE = 2; // 소장

    // 카테고리 관련
    public static final String WEBTOON = "webtoon";
    public static final String COMIC = "comic";
    public static final String NOVEL = "novel";
    public static final String ADULT = "adult";
    public static final String WEBTOON_TEXT = "웹툰";
    public static final String COMIC_TEXT = "만화";
    public static final String NOVEL_TEXT = "소설";
    public static final String ADULT_TEXT = "성인";
    public static final int CATEGORY_WEBTOON = 1; // 웹툰
    public static final int CATEGORY_COMIC = 2; // 만화
    public static final int CATEGORY_NOVEL = 3; // 소설

    // 에피소드 관련
    public static final int NOT_NEED_LOGIN = 0;     // 로그인 불필요한 회차
    public static final int NEED_LOGIN = 1;         // 로그인 필요한 회차
    public static final int EPISODE_TYPE_FREE = 1;       // 무료 회차
    public static final int EPISODE_TYPE_EVENT_FREE = 2; // 무료 이벤트 회차
    public static final int EPISODE_TYPE_PURCHASE = 3;   // 구매 회차
    public static final int EPISODE_TYPE_EVENT = 4;      // 이벤트 회차
    public static final int EPISODE_TYPE_TICKET_FREE = 5;// 무료 이용권 회차
    
    // 검색 유형 관련
    public static final String PREVIEW = "preview";  // 미리보기
    public static final String ALL = "all";  // 전체보기
    public static final String RENT = "rent";  // 대여
    public static final String HAVE = "have";  // 소장
    public static final String RANK = "메인 랭킹";
    public static final String NEW = "메인 최신작";
    public static final String CATEGORY = "카테고리";

    private ContentUtils() {
    }


}
