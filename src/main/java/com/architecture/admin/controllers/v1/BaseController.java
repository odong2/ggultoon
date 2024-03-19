package com.architecture.admin.controllers.v1;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.*;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.architecture.admin.config.SessionConfig.*;


/*****************************************************
 * 코어 컨트롤러
 ****************************************************/
public class BaseController {
    // 서버 환경 변수
    protected HashMap<String, Object> hmServer;
    protected String sSever;

    // 뷰에서 포함할 파일 데이터
    protected HashMap<String, String> hmImportFile;

    // 뷰에 전달할 데이터
    protected HashMap<String, Object> hmDataSet;

    /**
     * 메시지 가져오는 라이브러리
     */
    @Autowired
    protected MessageSource messageSource;

    /**
     * 텔레그램
     */
    @Autowired(required = false)
    protected TelegramLibrary telegramLibrary;

    /**
     * 암호화 라이브러리
     */
    @Autowired(required = false)
    protected SecurityLibrary securityLibrary;

    /**
     * 세션
     */
    @Autowired(required = false)
    protected HttpSession session;

    /**
     * 시간 라이브러리 참조
     */
    @Autowired(required = false)
    protected DateLibrary dateLibrary;

    /**
     * Redis 라이브러리 참조
     */
    @Autowired(required = false)
    protected RedisLibrary redisLibrary;

    /*****************************************************
     * 생성자
     ****************************************************/
    public BaseController() {
        // 뷰에서 포함할 파일 데이터
        hmImportFile = new HashMap<>();

        // 뷰에 전달할 데이터
        hmDataSet = new HashMap<>();

        // 서버 환경 변수
        hmServer = new HashMap<>();
        sSever = System.getProperty("spring.profiles.active");
        sSever = sSever == null ? "local" : sSever;
        hmServer.put("sSever", sSever);
    }

    /*****************************************************
     * 회원정보 레디스 불러오기
     ****************************************************/
    public JSONObject getMemberInfo() {
        // 로그인한 회원 정보
        return new JSONObject(session.getAttribute(MEMBER_INFO).toString());
    }

    /**
     * 회원 기본 정보
     *
     * @param key
     * @return
     */
    public String getMemberInfo(String key) {
        // 로그인한 회원 정보
        JSONObject json = new JSONObject(session.getAttribute(MEMBER_INFO).toString());

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
        }
        return jsonString;
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
     * 뷰에 전달할 데이터 셋팅
     ****************************************************/
    // 서버 환경변수 가져오기
    @ModelAttribute
    public void init(Model model) {
        String scheme = "";
        String currentDomain = "";
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String requestUrl = String.valueOf(request.getRequestURL());// 전체 경로
        String serverName = request.getServerName();// 도메인만
        if (request.getServerName().equals("localhost")) {
            scheme = "http";
        } else {
            scheme = "https";
        } // http / https
        Integer serverPort = request.getServerPort();// 포트

        if(serverPort.equals(80) || serverPort.equals(443)){
            currentDomain = scheme + "://" + serverName; // 전체 도메인
        }else{
            currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인
        }
        String requestURI = request.getRequestURI();// 경로+파일
        // String segment = request.getContextPath();// 경로만
        // String filename = request.getServletPath();// 파일만
        String clientIp = getClientIP(request);

        hmServer.put("requestUrl", requestUrl);
        hmServer.put("serverName", serverName);
        hmServer.put("serverPort", String.valueOf(serverPort));
        hmServer.put("currentDomain", currentDomain);
        hmServer.put("requestUri", requestURI);
        hmServer.put("clientIp", clientIp);

        model.addAttribute("dataSet", hmDataSet);
        model.addAttribute("importFile", hmImportFile);
        model.addAttribute("SERVER", hmServer);
    }

    private String getClientIP(HttpServletRequest request) {
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
     * Language 값 가져오기
     ****************************************************/
    public String langMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public String langMessage(String code, @Nullable Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
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
     * 세션 값 가져오기
     ****************************************************/
    public String getSession(String id) {
        return (String) session.getAttribute(id);
    }

    /*****************************************************
     * 리다이렉트 처리
     ****************************************************/
    public ResponseEntity<Object> redirect(String sUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(sUrl));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
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

    public String displayError(BindingResult result) {
        JSONObject obj = new JSONObject();
        String errorCode = "2000"; // 사용자가 유효하지 않은 값 입력
        obj.put("result", false);

        result.getAllErrors().forEach(objectError -> {

            // 에러메시지 Language 값 가져오기
            String message = objectError.getDefaultMessage();

            if (!obj.has("message") || obj.getString("message") == null) {
                obj.put("code", errorCode);
                obj.put("message", message);
            }
        });

        return obj.toString();
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

    /**
     * 세션에 저장된 회원 IDX 유효성 체크
     */
    public void checkSession() {

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 세션에서 찾은 memberIdx가 없을 때
        if (memberInfo == null) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);    // 로그인 후 이용해주세요.
        }
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
                    if (value != null && !value.isEmpty()) {

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

    /**
     * OTT에서 접속한 회원 토큰으로 구운 쿠키 제거
     */
    public void removeOttVisitTokenCookie(HttpServletRequest request, HttpServletResponse response) {

        // 모든 쿠키 조회
        Cookie[] cookies = request.getCookies();

        // 조회한 쿠키가 있는 경우
        if (cookies != null) {
            for (Cookie cookie : cookies) {

                // 쿠키 이름으로 검색
                if (cookie.getName().equals("ottVisitToken")) {

                    // 기존 쿠키 강제 만료
                    cookie.setMaxAge(0);
                    
                    // OTT 접속 토큰 쿠키 제거(빈값으로 재생성)
                    Cookie ottVisitToken = new Cookie("ottVisitToken", "");
                    ottVisitToken.setPath("/");
                    ottVisitToken.setSecure(true);
                    ottVisitToken.setHttpOnly(true);
                    response.addCookie(ottVisitToken); // 응답에 추가
                }
            }
        }
    }
}
