package com.architecture.admin.services.login;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.models.dao.login.LoginDao;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
@RequiredArgsConstructor
@Service
@Transactional
public class LogoutService extends BaseService {
    private final LoginDao loginDao;

    /**
     * 로그아웃
     *
     * @return true/false
     */
    public Boolean logout(HttpServletResponse httpResponse) {

        // jwt 삭제
        Cookie accessToken = new Cookie("accessToken", null);
        Cookie refreshToken = new Cookie("refreshToken", null);
        accessToken.setMaxAge(0);
        refreshToken.setMaxAge(0);
        accessToken.setPath("/");
        refreshToken.setPath("/");
        accessToken.setSecure(true);
        refreshToken.setSecure(true);
        refreshToken.setHttpOnly(true);
        httpResponse.addCookie(accessToken);
        httpResponse.addCookie(refreshToken);

        // 세션 아이디 가져오기
        String sId = (String) session.getAttribute(SessionConfig.LOGIN_ID);
        loginDao.deleteLoginKey(sId);

        if (!Objects.equals(sId, "")) {
            // 레디스 세션 정보 삭제
            String sKey = "session_" + sId;
            super.removeRedis(sKey);

            // 세션 비활성화
            session.invalidate();
            return true;
        } else {
            return false;
        }
    }
}
