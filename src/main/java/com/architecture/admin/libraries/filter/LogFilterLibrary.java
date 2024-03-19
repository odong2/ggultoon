package com.architecture.admin.libraries.filter;

import com.architecture.admin.libraries.TelegramLibrary;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

/*****************************************************
 * 모든 요청에 대해 로그를 남기는 LogFilter 필터
 ****************************************************/
@Slf4j
public class LogFilterLibrary implements Filter {
    private boolean bUseLog = false;
    private boolean bUseTelegram = false;

    private TelegramLibrary telegramLibrary;

    // 생성자
    public LogFilterLibrary() {
        this.telegramLibrary = new TelegramLibrary();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logInfo("log filter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // HTTP 요청이 오면 doFilter 호출
        logInfo("log filter doFilter");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        try {
            logInfo("REQUEST [{}][{}]", requestURI);
            chain.doFilter(request, response); // 다음 필터가 있으면 다음 필터를 호출하고 필터가 없으면 서블릿을 호출한다. 만약 이 로직을 호출하지 않으면 다음단계로 진행되지 않는다.

        } finally {
            logInfo("REQUEST [{}][{}]", requestURI);
        }
    }

    public void logInfo(String sMessage) {
        if ( bUseLog ) { log.info(sMessage); }
        if ( bUseTelegram ) { this.telegramLibrary.sendMessage(sMessage); }
    }

    private void logInfo(String sMessage, String requestURI) {
        if ( bUseLog ) { log.info(sMessage, requestURI); }
        if ( bUseTelegram ) {
            HashMap<String, String> hMessage = new HashMap<>();
            hMessage.put("sMessage", sMessage);
            hMessage.put("requestURI", requestURI);
            this.telegramLibrary.sendMessage(hMessage.toString());
        }
    }

    @Override
    public void destroy() {
        log.info("log filter destroy");
    }
}
