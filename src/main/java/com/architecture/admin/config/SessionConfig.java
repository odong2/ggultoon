package com.architecture.admin.config;

public abstract class SessionConfig {
    // 회원 기본 정보
    public static final String LOGIN_ID = "id";
    public static final String IDX = "idx";
    public static final String LOGIN_NICK = "nick";
    public static final String MEMBER_INFO = "memberInfo";
    public static final String ADULT = "adult";

    // 회원 세팅 정보
    public static final String MEMBER_SETTING = "memberSetting";
    public static final String COIN_ALARM = "coinAlarm";
    public static final Integer EXPIRED_TIME = 60 * 60 * 24 * 59; // 세션 만료 시간 3일 -> 59일로 변경

    // 로그인 체크 제외 항목
    public static final String[] WHITELIST = {
            "/v1/login/session",              // 로그인 세션 에러 페이지 [0] 번째 위치 고정
            "/v1/login/nick",                 // 닉네임 등록 에러 페이지 [1] 번째 위치 고정
            "/v1/login/naver",                // 네이버 로그인
            "/v1/login/google",               // 구글 로그인
            "/v1/login/kakao",                // 카카오 로그인
            "/v1/login",                      // 로그인
            "/v1/auth/naver",                 // 네이버 인증
            "/v1/auth/google",                // 구글 인증
            "/v1/auth/kakao",                 // 카카오 인증
            "/v1/join/**",                    // 회원가입
            "/v1/join",
            "/v1/check/**",                   // 본인인증
            "/v1/contents/curation",          // 메인 작품 큐레이션
            "/v1/contents/rank",              // 메인 작품 랭킹 리스트
            "/v1/contents/new",               // 메인 최신작 리스트
            "/v1/contents",                   // 작품 카테고리 리스트
            "/v1/contents/*",                 // 작품 상세
            "/v1/contents/*/episodes",        // 작품 회차 리스트
            "/v1/episodes/*",                 // 회차 뷰어
            "/v1/community/*/comments",       // 커뮤니티 댓글 리스트
            "/v1/community/*/comments/*",     // 커뮤니티 대댓글 리스트
            "/v1/community/contents",         // 커뮤니티 게시물 리스트 조회
            "/v1/community/contents/*",       // 커뮤니티 게시물 조회
            "/v1/logout",                     // 로그아웃
            "/v1/product/list",               // 충전소 페이지
            "/v1/login/**",                   // 로그인
            "/v1/payment/receiveNoti",        // 결제
            "/v1/board/**",                   // 고객센터
            "/v1/banner",                     // 배너 목록
            "/v1/banner/visit/*",             // 배너 유입 통계
            "/v1/member/check",               // 로그인 및 접속 경로 체크
            "/v1/member/cookie"               // OTT 접속 토큰 정보로 구운 쿠키 제거
    };

    // 허용 IP 항목
    public static final String[] ALLOW_IP_LIST = {
            "0:0:0:0:0:0:0:1"       // localhost
            ,"127.0.0.1"            // localhost
            ,"218.145.84.162"       // 2층 사무실
            ,"218.145.84.164"       // 2층 사무실
            ,"218.145.84.165"       // 2층 사무실
            ,"121.138.58.134"       // 4층 사무실
            ,"121.138.58.135"       // 5층 사무실 / VPN
            ,"121.140.12.62"        // 어플파트 강삼민님
            ,"121.140.12.207"       // 어플파트 강삼민님
            ,"210.105.46.123"       // 프론트엔드 유민님
            ,"112.145.122.38"       // 더잼미디어 나영주대표님
            ,"10.10.10.*"           // toon-www.devlabs.co.kr
            ,"172.16.4*"            // toon-www.devlabs.co.kr
            ,"10.0.*"               // prd 배포 확인
            ,"14.32.45.129"         // 백엔드파트 김덕모파트장님
            ,"1.229.111.142"        // 김정윤대표님
            ,"222.112.150.125"      // 프론트엔드 유민님
    };

    // IP 체크없이 접근가능 URL 리스트
    public static final String[] ALLOW_IP_URL_LIST = {
            "/v1/payment/receiveNoti" // 결제 결과 페이지
    };
}