package com.architecture.admin.config.interceptor;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.TelegramLibrary;
import com.architecture.admin.libraries.jwt.JwtDto;
import com.architecture.admin.libraries.jwt.JwtLibrary;

import com.architecture.admin.models.dao.jwt.JwtDao;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.HashMap;

@Slf4j
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtDao jwtDao;
    private final JwtLibrary jwtLibrary;
    private final TelegramLibrary telegramLibrary;

    private JwtDto jwtDto = new JwtDto();

    @Autowired
    protected HttpSession session;

    // 해당 리스트에 포함된 경로 처리
    private static final String[] blacklist = {
            // api jwt 필터 적용 리스트
            "/v1/*"
    };

    private static final String[] whitelist = SessionConfig.WHITELIST;

    // 닉네임 등록 화이트 리스트
    private static final String[] whiteNicklist = {
            "/v1/member/nick/check",  // 닉네임 사용가능 체크
            "/v1/join/nick",          // 닉네임 등록
            "/v1/policy/list",        // 가입 시 이용약관 리스트
            "/v1/genre"               // 장르 목록
    };

    private boolean bUseLog = false;
    private boolean bUseTelegram = false;

    /**
     * JWT access/refresh 토큰 생성 및 쿠키 저장
     *
     * @param httpRequest
     * @param httpResponse
     */
    public void setJwtToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String secretKeyType = "normal"; // 키타입 정해주기 - 여러 키 사용할 용
        String sId = (String) session.getAttribute(SessionConfig.LOGIN_ID); // id 가져오기
        String ip = getClientIP(httpRequest); // ip 가져오기
        HashMap<String,Object> tokenMap = new HashMap<>(); // 토큰에 담을 정보
        tokenMap.put("ip", ip);
        tokenMap.put("id", sId);

        // 토큰생성
        JwtDto jwt = jwtLibrary.createToken(secretKeyType, tokenMap);
        // 생성된 리프레쉬 토큰 db에 입력하기 (식별을위해 id와 ip 입력(ip는 중복가능,id는 유니크), 대조용 리프레쉬토큰자체 저장)
        tokenMap.put("refreshToken",jwt.getRefreshToken());
        jwtDao.insertRefreshToken(tokenMap);

        Cookie accessToken = new Cookie("accessToken", jwt.getAccessToken()); // 쿠키 이름 지정하여 생성( key, value 개념)
        Cookie refreshToken = new Cookie("refreshToken", jwt.getRefreshToken()); // 쿠키 이름 지정하여 생성( key, value 개념)
        accessToken.setMaxAge(jwtDto.getCookieTime()); // 쿠키 유효 기간: 7일로 설정(168시간)
        refreshToken.setMaxAge(jwtDto.getCookieTime()); // 쿠키 유효 기간: 7일로 설정(168시간)
        accessToken.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        refreshToken.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        accessToken.setSecure(true);
        refreshToken.setSecure(true);
        refreshToken.setHttpOnly(true);
        //accessToken.setDomain(httpRequest.getServerName());
        //refreshToken.setDomain(httpRequest.getServerName());
        httpResponse.addCookie(accessToken);
        httpResponse.addCookie(refreshToken);
    }


    @Override
    public boolean preHandle(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Object handler) throws Exception {
        String requestURI = httpRequest.getRequestURI();
        String ip = getClientIP(httpRequest);

        //  preflight 통신 시 제외 처리
        if(httpRequest.getMethod().equals("OPTIONS")){
            return true;
        }

        try {
            logInfo("클래스명 : " + Thread.currentThread().getStackTrace()[2].getClassName(), requestURI);
            logInfo("메소드명 : " + Thread.currentThread().getStackTrace()[2].getMethodName(), requestURI);
            logInfo("줄번호 : " + Thread.currentThread().getStackTrace()[2].getLineNumber(), requestURI);
            logInfo("파일명 : " + Thread.currentThread().getStackTrace()[2].getFileName().toString(), requestURI);
            logInfo("jwt 인증 체크 필터 시작{}" + isJwtCheckPath(requestURI), requestURI);
            logInfo("jwt CheckPath : " + isJwtCheckPath(requestURI), requestURI);
            String secretKeyType = "normal"; // 키타입 정해주기 -여러 키 사용할 용

            if (isJwtCheckPath(requestURI)) { // jwt토큰 검증 (블랙리스트)
                logInfo("jwt 인증 체크 로직 실행 {}", requestURI);
                JwtDto jwtDto = new JwtDto();
                Cookie[] cookies = httpRequest.getCookies(); // 모든 쿠키 가져오기
                Integer accessChk = 0;

                if (cookies != null) {
                    for (Cookie cKey : cookies) {
                        String name = cKey.getName(); // 쿠키 이름 가져오기
                        String value = cKey.getValue(); // 쿠키 값 가져오기
                        if (name.equals("refreshToken")) {
                            jwtDto.setRefreshToken(value);
                        }
                        if (name.equals("accessToken")) {
                            jwtDto.setAccessToken(value);
                        }
                    }
                }

                if (jwtDto.getAccessToken() != null) {
                    // access Token 검증 : 1이 리턴되면 정상, 2가 리턴되면 기간만료 -> 리프레쉬 확인절차
                    accessChk = jwtLibrary.validateAccessToken(secretKeyType, jwtDto);

                    /* 모바일 환경 IP 변경이슈로 제외
                    if(accessChk == 1) { // 유효한 경우
                        // ip 확인
                        String tokenIp = jwtLibrary.getIpFromToken(secretKeyType, jwtDto.getAccessToken());
                        if (!ip.equals(tokenIp)) {
                            accessChk = 0;
                        }
                    }
                    */
                }

                // access token이 유효하지 않을경우 -> 블락처리, 다시 발급받아야함
                if (accessChk == null || accessChk == 0) {
                    logInfo("jwt 토큰 만료, 신규 발급 요청 {}", requestURI);
                    httpResponse.sendRedirect(whitelist[0]);
                    return false;
                }

                // access token 유효하나 기간이 만료일경우 -> refresh token 확인
                if (accessChk == 2) {
                    Integer refreshChk = 0;
                    // Refresh Token 검증 : 1일 경우 에만 새 access token가져옴
                    refreshChk = jwtLibrary.validateRefreshToken(secretKeyType, jwtDto);

                    if (refreshChk == 1) { // refresh가 유효하며 db와 일치하고 기간도 살아있을경우 access만 새로 쿠키에 등록

                        // ip 확인
                        String tokenIp = jwtLibrary.getIpFromToken(secretKeyType, jwtDto.getRefreshToken());
                        if (!ip.equals(tokenIp)) {
                            logInfo("jwt 토큰 만료, 신규 발급 요청 {}", requestURI);
                            httpResponse.sendRedirect(whitelist[0]);
                            return false;
                        }

                        // db 확인절차 (db데이터와 일치되면 재발급)
                        HashMap tokenMap = (HashMap) jwtLibrary.getAllClaims(secretKeyType, jwtDto.getRefreshToken()).get("tokenMap");
                        String id = (String) tokenMap.get("id");
                        // db 조회하여 기존 refreshToken 및 id가 동일한지 확인
                        tokenMap.put("refreshToken" , jwtDto.getRefreshToken());
                        tokenMap.put("id" , id);
                        Integer verifyRefreshToken = jwtDao.verifyRefreshToken(tokenMap); // 일치:1, 불일치:0

                        if(verifyRefreshToken != 1) { // 불일치한 경우 블락처리
                            logInfo("jwt 토큰 만료, 신규 발급 요청 {}", requestURI);
                            httpResponse.sendRedirect(whitelist[0]);
                            return false;
                        }

                        // RefreshToken 등록시간
                        int tokenReg = jwtLibrary.getRegdateFromToken(secretKeyType, jwtDto.getRefreshToken());
                        Timestamp time = new Timestamp(System.currentTimeMillis());
                        // RefreshToken 재 발급 기준 시간 (1일)
                        long refreshTime = (time.getTime() / 1000L) - (60 * 60 * 24);

                        // Token 재발급
                        if(tokenReg < refreshTime) {
                            // RefreshToken 발급시간이 하루 이상 지난경우 모두 재발급
                            setJwtToken(httpRequest, httpResponse);
                        }else{
                            // accessToken 재발급
                            Claims refreshClaim = jwtLibrary.getAllClaims(secretKeyType, jwtDto.getRefreshToken());
                            String newAccessToken = jwtLibrary.recreationAccessToken(secretKeyType, refreshClaim.get("tokenMap"));
                            Cookie accessToken = new Cookie("accessToken", newAccessToken); // 쿠키 이름 지정하여 생성( key, value 개념)
                            accessToken.setMaxAge(jwtDto.getCookieTime()); // 쿠키 유효 기간: 7일
                            accessToken.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
                            accessToken.setSecure(true);
                            //accessToken.setDomain(httpRequest.getServerName());
                            httpResponse.addCookie(accessToken);
                        }

                    }
                    else { // refresh 가 유효하지않거나 만료일경우 (새로 발급받아야함)
                        logInfo("jwt 토큰 만료, 신규 발급 요청 {}", requestURI);
                        httpResponse.sendRedirect(whitelist[0]);
                        return false;
                    }
                }
                // access, refresh 둘다 살아있을 경우 패스
            }

        } catch (Exception e) {
            log.info("error {}", requestURI);
            throw e;

        } finally {
            logInfo("인증 체크 필터 종료 {}", requestURI);
        }

        return true;
    }


    private void logInfo(String sMessage, String requestURI) {
        if (bUseLog) {
            log.info(sMessage, requestURI);
        }
        if (bUseTelegram) {
            HashMap<String, String> hMessage = new HashMap<>();
            hMessage.put("sMessage", sMessage);
            hMessage.put("requestURI", requestURI);
            telegramLibrary.sendMessage(hMessage.toString());
        }
    }

    /**
     * blackList 인증 체크
     * whiteList 목록 제외
     */
    private boolean isJwtCheckPath(String requestURI) {
        return PatternMatchUtils.simpleMatch(blacklist, requestURI) &&
              !PatternMatchUtils.simpleMatch(whitelist, requestURI) &&
              !PatternMatchUtils.simpleMatch(whiteNicklist, requestURI);
    }


    private static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        log.info("> X-FORWARDED-FOR : " + ip);

        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
            log.info("> Proxy-Client-IP : " + ip);
        }

        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP");
            log.info(">  WL-Proxy-Client-IP : " + ip);
        }

        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
            log.info("> HTTP_CLIENT_IP : " + ip);
        }

        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            log.info("> HTTP_X_FORWARDED_FOR : " + ip);
        }

        if (ip == null) {
            ip = request.getRemoteAddr();
            log.info("> getRemoteAddr : " + ip);
        }

        log.info("> Result : IP Address : " + ip);

        return ip;
    }
}