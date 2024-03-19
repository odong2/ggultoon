package com.architecture.admin.libraries.filter;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.TelegramLibrary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.PatternMatchUtils;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/*****************************************************
 * IP 체크 필터 - 허용 IP 이외 차단
 ****************************************************/
@Slf4j
public class IpCheckFilterLibrary implements Filter {

    // 체크 제외 URL
    private static final String[] whitelist = SessionConfig.ALLOW_IP_URL_LIST;

    // 허용 IP 리스트
    private static final String[] iplist = SessionConfig.ALLOW_IP_LIST;


    private boolean bUseLog = false;
    private boolean bUseTelegram = false;

    private final TelegramLibrary telegramLibrary;
    // 생성자 추가
    public IpCheckFilterLibrary() {
        this.telegramLibrary = new TelegramLibrary();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            logInfo("IP 체크 필터 시작{}", requestURI);
            // preflight 통신 시 제외 처리 - && !httpRequest.getMethod().equals("OPTIONS")
            if (isCheckPath(requestURI) && !httpRequest.getMethod().equals("OPTIONS")) {
                logInfo("IP 체크 로직 실행 {}", requestURI);

                /*****************************************************
                 * ip 값 가져오기
                 ****************************************************/
                String ip = httpRequest.getHeader("X-FORWARDED-FOR");
                if (ip == null) {
                    ip = httpRequest.getHeader("Proxy-Client-IP");
                }
                if (ip == null) {
                    ip = httpRequest.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null) {
                    ip = httpRequest.getHeader("HTTP_CLIENT_IP");
                }
                if (ip == null) {
                    ip = httpRequest.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ip == null) {
                    ip = httpRequest.getRemoteAddr();
                }

                //telegramLibrary.sendMessage("아이피확인 : "+ip,"LJH");

                if (isCheckIp(ip)){
                    telegramLibrary.sendMessage(" 차단아이피 : "+ip,"LJH");
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

            }
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.info("error {}", requestURI);
            throw e;

        } finally {
            logInfo("IP 체크 필터 종료 {}", requestURI);
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
     *  IP 체크 제외 URL
     */
    private boolean isCheckPath(String requestURI) {
        boolean isCheckPath = false;
        // whitelist에는 없는 항목
        if(!PatternMatchUtils.simpleMatch(whitelist, requestURI) ){
            isCheckPath  = true;
        }
        return isCheckPath;
    }

    /**
     *  허용 IP 확인
     */
    private boolean isCheckIp(String requestURI) {
        boolean isCheckIp = false;
        if(!PatternMatchUtils.simpleMatch(iplist, requestURI) ){
            isCheckIp  = true;
        }
        return isCheckIp;
    }

}
