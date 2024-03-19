package com.architecture.admin.controllers.v1.login;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.services.login.LogoutService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/logout")
public class LogoutV1Controller extends BaseController {
    private final LogoutService logoutService;

    /**
     * 로그아웃
     * 
     * @return 로그아웃 페이지
     */
    @GetMapping()
    public String logout(HttpServletResponse httpResponse) {
        // 회원 로그아웃 처리
        Boolean bIsLogout = logoutService.logout(httpResponse);

        if (Boolean.FALSE.equals(bIsLogout)) {
            throw new CustomException(CustomError.LOGOUT_FAIL);
        }

        // set return data
        JSONObject data = new JSONObject();
        data.put("location", "/");

        // return value
        String sErrorMessage = "lang.login.success.logout";
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }
}
