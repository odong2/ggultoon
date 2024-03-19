package com.architecture.admin.services;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.architecture.admin.config.SessionConfig.*;

/*****************************************************
 * 코어 서비스
 ****************************************************/
@Service
public class BaseService {

    // 시간 라이브러리 참조
    @Autowired
    protected DateLibrary dateLibrary;

    // 암호화 라이브러리
    @Autowired
    protected SecurityLibrary securityLibrary;

    // 세션
    @Autowired
    protected HttpSession session;

    // 텔레그램
    @Autowired
    protected TelegramLibrary telegramLibrary;

    // Redis 라이브러리
    @Autowired
    protected RedisLibrary redisLibrary;

    // Curl 라이브러리
    @Autowired
    protected CurlLibrary curlLibrary;

    /**
     * 메시지 가져오는 라이브러리
     */
    @Autowired
    protected MessageSource messageSource;

    /*****************************************************
     * 세션 값 가져오기
     ****************************************************/
    public String getSession(String id) {
        return (String) session.getAttribute(id);
    }

    /*****************************************************
     * 회원 정보 불러오기
     ****************************************************/

    public String getMemberInfo(String key) {

        // 로그인한 회원 정보
        JSONObject json = new JSONObject(session.getAttribute(SessionConfig.MEMBER_INFO).toString());

        String jsonString = "";
        // 회원 정보
        if (json.has(MEMBER_INFO) && key.equals(MEMBER_INFO)) {
            jsonString = json.getString(MEMBER_INFO);
            // 회원 닉네임
        } else if (json.has(LOGIN_NICK) && key.equals(LOGIN_NICK)) {
            jsonString = json.getString(LOGIN_NICK);
            //회원 아이디
        } else if (json.has(LOGIN_ID) && key.equals(LOGIN_ID)) {
            jsonString = json.getString(LOGIN_ID);
            // 성인 여부
        } else if (json.has(ADULT) && key.equals(ADULT)) {
            jsonString = String.valueOf(json.getInt(ADULT));
            // 회원 idx
        } else if (json.has(IDX) && key.equals(IDX)) {
            jsonString = String.valueOf(json.getLong(IDX));
        }

        return jsonString;
    }

    /**
     * 회원 알람 세팅 정보
     *
     * @param key
     * @return
     */
    public String getMemberSetting(String key) {
        // 로그인한 회원 정보
        JSONObject json = new JSONObject(session.getAttribute(MEMBER_SETTING).toString());

        String jsonString = "";

        // 회원 정보
        if (key.equals(MEMBER_SETTING)) {
            jsonString = json.getString(json.getString(MEMBER_SETTING));
            // 코인 차감 안내
        } else if (key.equals(COIN_ALARM)) {
            jsonString = String.valueOf(json.getInt(COIN_ALARM));
            // 뷰 연속 보기
        }
        return jsonString;
    }

    /**
     * OTT에서 접속한 회원 토큰 정보
     */
    public String getOttVisitToken(HttpServletRequest request) {

        // 모든 쿠키 조회
        Cookie[] cookies = request.getCookies();
        String isAdult = "";

        // 조회한 쿠키가 있는 경우
        if (cookies != null) {
            for (Cookie cookie : cookies) {

                // 쿠키 이름으로 검색
                if (cookie.getName().equals("ottVisitToken")) {

                    // 쿠키 값 조회(UTF-8 디코딩)
                    String value = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                    if (value != null && !value.equals("")) {

                        // String to JSON
                        JSONObject jsonObject = new JSONObject(value);

                        // 성인 여부 구분값 조회
                        if (jsonObject.has("authCheck")) {
                            isAdult = jsonObject.getString("authCheck");
                            if (isAdult != null && !isAdult.isEmpty()) {
                                return isAdult;
                            }
                        }
                    }
                }
            }
        }
        return isAdult;
    }

    /*****************************************************
     * 레디스
     ****************************************************/
    // 레디스 값 생성
    public void setRedis(String key, String value, Integer expiredSeconds) {
        redisLibrary.setData(key, value, expiredSeconds);
    }

    // 레디스 값 불러오기
    public String getRedis(String key) {
        return redisLibrary.getData(key);
    }

    // 레디스 값 삭제하기
    public void removeRedis(String key) {
        redisLibrary.deleteData(key);
    }

    /*****************************************************
     * 뷰 Json
     ****************************************************/
    public String displayJson(Boolean result, String code, String message) {
        JSONObject obj = new JSONObject();
        obj.put("result", result);
        obj.put("code", code);
        obj.put("message", message);

        return obj.toString();
    }

    public String displayJson(Boolean result, String code, String message, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("result", result);
        obj.put("code", code);
        obj.put("message", message);
        obj.put("data", data);

        return obj.toString();
    }

    /*****************************************************
     * Curl
     ****************************************************/
    // get
    public String getCurl(String url, String header) {
        return curlLibrary.get(url, header);
    }

    // post
    public String postCurl(String url, Map dataset) {
        return curlLibrary.post(url, dataset);
    }

    /*****************************************************
     * 암호화 처리
     ****************************************************/
    // 양방향 암호화 암호화
    public String encrypt(String str) throws Exception {
        return securityLibrary.aesEncrypt(str);
    }

    // 양방향 암호화 복호화
    public String decrypt(String str) throws Exception {
        return securityLibrary.aesDecrypt(str);
    }

    // 단방향 암호화
    public String md5encrypt(String str) {
        return securityLibrary.md5Encrypt(str);
    }

    /*****************************************************
     * 디버깅
     ****************************************************/
    public void d() {
        int iSeq = 2;
        System.out.println("======================================================================");
        System.out.println("클래스명 : " + Thread.currentThread().getStackTrace()[iSeq].getClassName());
        System.out.println("메소드명 : " + Thread.currentThread().getStackTrace()[iSeq].getMethodName());
        System.out.println("줄번호 : " + Thread.currentThread().getStackTrace()[iSeq].getLineNumber());
        System.out.println("파일명 : " + Thread.currentThread().getStackTrace()[iSeq].getFileName());
    }

    public void pushAlarm(String sendMessage) {
        telegramLibrary.sendMessage(sendMessage);
    }

    public void pushAlarm(String sendMessage, String sChatId) {
        telegramLibrary.sendMessage(sendMessage, sChatId);
    }

    /*****************************************************
     * Language 값 가져오기
     ****************************************************/
    public String langMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public String langMessage(String code, @Nullable Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /*****************************************************
     * get locale Language 현재 언어 값
     ****************************************************/
    public String getLocaleLang() {
        String localLang = LocaleContextHolder.getLocale().toString().toLowerCase();

        switch (localLang) {
            case "ko_kr", "ko", "kr":
                return "ko";
            case "en":
                return "en";
            default:
                return "en";
        }
    }

    /*****************************************************
     * ip 값 가져오기
     * private => public 으로 변환
     ****************************************************/
    public String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /*****************************************************
     * email값인지 체크하기
     ****************************************************/
    public boolean isEmail(String email) {
        boolean validation = false;

        if (Objects.equals(email, "") || email == null) {
            return false;
        }

        String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(email);
        if (m.matches()) {
            validation = true;
        }

        return validation;
    }

    /*****************************************************
     * 현재 도메인
     ****************************************************/
    public String getCurrentDomain() {
        // 도메인 받아오기
        String scheme = "";
        String currentDomain = "";
        HttpServletRequest request = ServerLibrary.getCurrReq();
        if (request.getServerName().equals("localhost")) {
            scheme = "http";
        } else {
            scheme = "https";
        } // http / https
        String serverName = request.getServerName();// 도메인만
        Integer serverPort = request.getServerPort();// 포트
        if(serverPort.equals(80) || serverPort.equals(443)){
            currentDomain = scheme + "://" + serverName; // 전체 도메인
        }else{
            currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인
        }
        return currentDomain; // 전체 도메인
    }

    /**
     * error log 알림
     *
     * @param params
     *              입력/수정 전달값
     * @param arrStackTrace
     *              실행 메소드 정보
     */
    public void sendError(Object params,StackTraceElement[] arrStackTrace) {
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String referrer = request.getHeader("Referer");
        String sClass = arrStackTrace[1].getClassName();
        String method = arrStackTrace[1].getMethodName();

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("referrer", referrer);
        hm.put("sClass", sClass);
        hm.put("method", method);
        hm.put("params", params);
        pushAlarm(hm.toString(),"LJH");
    }
}
