package com.architecture.admin.libraries.filter;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.TelegramLibrary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PatternMatchUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;

/*****************************************************
 * 로그인 인증 체크 필터
 ****************************************************/
@Slf4j
public class LoginApiCheckFilterLibrary implements Filter {
    // 해당 리스트에 포함된 경로 처리
    private static final String[] blacklist = {
            // api 로그인 체크 필터 적용 리스트
            "/v1/member/*"
    };

    private static final String[] whitelist = {

    };

    // 닉네임 등록 화이트 리스트
    private static final String[] whiteNicklist = {
            "/v1/join/nick",           // 닉네임 등록
            "/v1/policy/list",         // 가입 시 이용약관 리스트
            "/v1/genre",               // 장르 목록
    };

    private boolean bUseLog = false;
    private boolean bUseTelegram = false;

    private final TelegramLibrary telegramLibrary;
    // 생성자 추가
    public LoginApiCheckFilterLibrary() {
        this.telegramLibrary = new TelegramLibrary();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            logInfo("\"/v1 시작{}", requestURI);
            // preflight 통신 시 제외 처리 - && !httpRequest.getMethod().equals("OPTIONS")
            if (isLoginCheckPath(requestURI) && !httpRequest.getMethod().equals("OPTIONS")) {
                logInfo("인증 체크 로직 실행 {}", requestURI);
                HttpSession session = httpRequest.getSession(false);
                if (session == null) {
                    logInfo("미인증 사용자 요청 {}", requestURI);
                    //로그인으로 리다이렉트
                    httpResponse.sendRedirect(whitelist[0]);
                    return;
                }

                //회원 로그인 상태 확인
                String sMemberInfo = (String) session.getAttribute(SessionConfig.MEMBER_INFO);

                if (sMemberInfo == null) {
                    //로그인으로 리다이렉트
                    httpResponse.sendRedirect(whitelist[0]);
                    return;
                }
            }
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.info("error {}", requestURI);
            throw e;

        } finally {
            logInfo("인증 체크 필터 종료 {}", requestURI);
        }
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
     */
    private boolean isLoginCheckPath(String requestURI) {
        boolean isLoginCheck = false;
        // blacklist에 포함하면서 whitelist에는 없는 항목
        if(PatternMatchUtils.simpleMatch(blacklist, requestURI) && !PatternMatchUtils.simpleMatch(whitelist, requestURI) && !PatternMatchUtils.simpleMatch(whiteNicklist, requestURI)){
            isLoginCheck  = true;
        }
        return isLoginCheck;
    }

}
