package com.architecture.admin.libraries;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/*****************************************************
 * Server 라이브러리
 ****************************************************/
@Component
public class ServerLibrary {
    public static HttpServletRequest getCurrReq() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
