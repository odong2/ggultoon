package com.architecture.admin.controllers.v1.login;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.auth.GoogleAuthService;
import com.architecture.admin.services.auth.KakaoAuthService;
import com.architecture.admin.services.auth.NaverAuthService;
import com.architecture.admin.services.login.LoginService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;
import java.util.Objects;

import static com.architecture.admin.libraries.utils.CommonUtils.notEmpty;


@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/login")
public class LoginV1Controller extends BaseController {
    private final LoginService loginService;
    private final NaverAuthService naverAuthService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;

    /**
     * 로그인 세션 없이 페이지 호출 시 처리
     *
     * @return
     */
    @RequestMapping("/session")
    public String lostSession() {
        // set return data
        JSONObject data = new JSONObject();

        // return value
        String sErrorMessage = "lang.login.exception.login_again";
        String message = super.langMessage(sErrorMessage);
        return displayJson(false, "9999", message, data);
    }

    /**
     * 로그인
     *
     * @param memberDto (id , pw)
     * @param result
     * @param httpRequest
     * @param httpResponse
     * @return
     * @throws Exception
     */
    @PostMapping("")
    public String login(@RequestBody @Valid MemberDto memberDto,
                        BindingResult result,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) throws Exception {

        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 회원 로그인 처리
        JSONObject bIsLogin = loginService.login(memberDto, httpRequest, httpResponse);

        if (Boolean.FALSE.equals(bIsLogin.get("result"))) {
            throw new CustomException(CustomError.LOGIN_FAIL); // 로그인이 실패하였습니다.
        }

        // 로그인한 회원 정보 set
        JSONObject data = getMemberInfo();
        memberDto.setIdx(data.getLong("idx")); // 회원 idx set
        data.put("auto",loginService.auto(memberDto)); // 자동 로그인 set

        // 로그인 마일리지 이벤트가 진행 중일 경우에만
        if (bIsLogin.has("loginMileage")) {
            data.put("loginMileage", bIsLogin.get("loginMileage")); // 로그인 마일리지 지급 상태 set
        }

        // return value
        String sErrorMessage = "lang.login.success.login";
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }

    /**
     * 소셜 로그인 테스트
     *
     * @param social 소셜 로그인 종류(naver/kakao)
     * @return return_url 로그인 URL
     */
    @GetMapping("/test/{social}")
    public ResponseEntity<Object> socialLogin(@PathVariable String social) {
        String return_url = "/";
        //네이버
        if (Objects.equals(social, "naver")) {
            return_url = naverAuthService.getUrl();
        }// 구글
        else if (Objects.equals(social, "google")) {
            return_url = googleAuthService.getUrl();
        }//카카오
        else if (Objects.equals(social, "kakao")) {
                return_url = kakaoAuthService.getUrl();
            } else {
            // 없는 경로 진입 시 메인으로
            return redirect(hmServer.get("currentDomain") + "/");
        }
        return redirect(return_url);
    }
    
    /**
     * 소셜 로그인 URL 이동
     *
     * @param social 소셜 로그인 종류(naver/kakao)
     * @return return_url 로그인 URL
     */
    @PostMapping("/{social}")
    public String joinSocial(@PathVariable String social,
                             @RequestBody @Valid SocialDto socialDto,
                             BindingResult result,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) throws Exception {

        if (result.hasErrors()) {
            return super.displayError(result);
        }

        MemberDto memberDto = new MemberDto();
        memberDto.setIsSimple(1);
        // 로그인 타입 추가
        memberDto.setLoginType(socialDto.getLoginType());

        Map<String, Object> userInfo;

        if(notEmpty(socialDto.getAuto())){
            memberDto.setAuto(socialDto.getAuto());
        }else if (Objects.equals(social, "kakao")) {
            // 카카오 회원 정보
            userInfo = kakaoAuthService.getInfo(socialDto);
            memberDto.setId((String) userInfo.get("id"));
            memberDto.setEmail((String) userInfo.get("email"));
            memberDto.setSimpleType("kakao");
            // 아이디 암호화하여 pw로 등록
            memberDto.setPassword((String) userInfo.get("id"));
        }else if (Objects.equals(social, "naver")) {
            //네이버 회원정보
            userInfo = naverAuthService.getInfo(socialDto);
            memberDto.setId((String) userInfo.get("id"));
            memberDto.setEmail((String) userInfo.get("email"));
            memberDto.setSimpleType("naver");
            // 아이디 암호화하여 pw로 등록
            memberDto.setPassword((String) userInfo.get("id"));
        }

       // 로그인 처리
        JSONObject bIsLogin = loginService.login(memberDto, httpRequest, httpResponse);

        // 로그인 처리 실패 시
        if (Boolean.FALSE.equals(bIsLogin.get("result"))) {
            throw new CustomException(CustomError.LOGIN_FAIL);
        }

        // 로그인한 회원 정보 set
        JSONObject data = getMemberInfo();
        memberDto.setIdx(data.getLong("idx")); // 회원 idx set
        data.put("auto",loginService.auto(memberDto)); // 자동 로그인 set

        // 로그인 마일리지 이벤트가 진행 중일 경우에만
        if (bIsLogin.has("loginMileage")) {
            data.put("loginMileage", bIsLogin.get("loginMileage")); // 로그인 마일리지 지급 상태 set
        }

        // return value
        String sErrorMessage = "lang.login.success.login";
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }
}

