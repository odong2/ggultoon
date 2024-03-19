package com.architecture.admin.controllers.v1.auth;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.auth.GoogleAuthService;
import com.architecture.admin.services.auth.KakaoAuthService;
import com.architecture.admin.services.auth.NaverAuthService;
import com.architecture.admin.services.login.JoinService;
import com.architecture.admin.services.login.LoginService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthV1Controller extends BaseController {

    private final NaverAuthService naverAuthService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final JoinService joinService;
    private final LoginService loginService;
    private String joinDisabledMsg = "lang.login.exception.shutdown"; // 서비스 종료로 회원가입이 불가능해요

    /**
     * 소셜 회원가입
     *
     * @param social 소셜 로그인 종류(naver/google)
     * @param socialDto (accessToken)
     * @param result
     * @param httpRequest
     * @param httpResponse
     * @return
     * @throws Exception
     */
    @GetMapping("/{social}")
    public String socialAuth(@PathVariable String social,
                             @Valid SocialDto socialDto,
                             BindingResult result,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) throws Exception {

        /** 꿀툰 서비스 종료 -> 회원가입 불가 처리 **/
        String message = super.langMessage(joinDisabledMsg); // 서비스 종료로 회원가입이 불가능해요
        return displayJson(false, "1000", message);

//        if (result.hasErrors()) {
//            return super.displayError(result);
//        }
//
//        MemberDto memberDto = new MemberDto();
//
//        // 네이버
//        if (Objects.equals(social, "naver")) {
//
//            if(true){
//                return naverAuthService.getToken(socialDto);
//            }
//            //네이버 토큰 요청
//            String accessToken = naverAuthService.getToken(socialDto);
//            // 받아온 accessToken 세팅
//            socialDto.setAccessToken(accessToken);
//            // 네이버에서 받아 온 회원 정보
//            Map<String, Object> userInfo = naverAuthService.getInfo(socialDto);
//
//            memberDto.setId((String) userInfo.get("id"));
//            memberDto.setEmail((String) userInfo.get("email"));
//            memberDto.setGender((String) userInfo.get("gender"));
//            memberDto.setBirth((String) userInfo.get("birthday"));
//            memberDto.setPhone((String) userInfo.get("phone"));
//            memberDto.setIsSimple(1);
//            memberDto.setSimpleType("naver");
//            // 아이디 암호화하여 pw로 등록
//            memberDto.setPassword((String) userInfo.get("id"));
//        }
//        // 카카오
//        else if (Objects.equals(social, "kakao")) {
//            if(true){
//                // 토큰 리턴
//                return kakaoAuthService.getToken(socialDto);
//            }
//            //카카오 토큰 요청
//            String accessToken = kakaoAuthService.getToken(socialDto);
//            // 받아온 accessToken 세팅
//            socialDto.setAccessToken(accessToken);
//            // 카카오에서 받아 온 회원 정보
//            Map<String, Object> userInfo = kakaoAuthService.getInfo(socialDto);
//
//            memberDto.setId((String) userInfo.get("id"));
//            memberDto.setEmail((String) userInfo.get("email"));
//            memberDto.setGender((String) userInfo.get("gender"));
//            memberDto.setBirth((String) userInfo.get("birthday"));
//            memberDto.setPhone((String) userInfo.get("phone"));
//            memberDto.setIsSimple(1);
//            memberDto.setSimpleType("kakao");
//            // 아이디 암호화하여 pw로 등록
//            memberDto.setPassword((String) userInfo.get("id"));
//        }
//        // 구글
//        else if (Objects.equals(social, "google")) {
//            //구글 토큰 요청
//            String accessToken = googleAuthService.getToken(socialDto);
//
//            // 받아온 accessToken 세팅
//            socialDto.setAccessToken(accessToken);
//
//            // 구글에서 받아 온 회원 정보
//            Map<String, Object> userInfo = googleAuthService.getInfo(socialDto);
//
//            memberDto.setId((String) userInfo.get("id"));
//            memberDto.setEmail((String) userInfo.get("email"));
//            memberDto.setIsSimple(1);
//            memberDto.setSimpleType("google");
//            // 아이디 암호화하여 pw로 등록
//            memberDto.setPassword((String) userInfo.get("id"));
//        } else {
//            httpResponse.sendRedirect("/");
//        }
//
//        // 회원 가입 된 아이디인지 체크
//        Boolean bDupleId = joinService.checkDupleId(memberDto);
//
//        // 가입되어있지 않으면
//        if (Boolean.FALSE.equals(bDupleId)) {
//            // 회원가입 처리
//            Long resultRegist = joinService.regist(memberDto, httpResponse);
//            // 회원가입 실패
//            if (resultRegist < 0L) {
//                throw new CustomException(CustomError.LOGIN_FAIL);
//            } else {
//                // 로그인 프로세스를 태우기 위해 비밀번호 암호화 안 된 값으로 다시 초기화
//                memberDto.setPassword(memberDto.getId());
//            }
//        }
//
//        // 로그인처리
//        JSONObject bIsLogin = loginService.login(memberDto, httpRequest, httpResponse);
//
//        // 로그인 처리 실패 시
//        if (Boolean.FALSE.equals(bIsLogin.get("result"))) {
//            throw new CustomException(CustomError.LOGIN_FAIL);
//        }
//
//        // set return data
//        JSONObject data = new JSONObject();
//        data.put("location", "/");
//
//        // return value
//        String sErrorMessage = "lang.login.success.login";
//        String message = super.langMessage(sErrorMessage);
//        return displayJson(true, "1000", message, data);
    }

    @GetMapping("/info/{social}")
    public String getInfo(SocialDto socialDto, @PathVariable String social){
        // 받아온 accessToken 세팅
        //socialDto.setAccessToken(token);
        // 회원 정보
        Map<String, Object> userInfo = null;
        if (Objects.equals(social, "kakao")) {
            userInfo = kakaoAuthService.getInfo(socialDto);
        }else if (Objects.equals(social, "naver")) {
            userInfo = naverAuthService.getInfo(socialDto);
        }else if (Objects.equals(social, "google")) {
            // TODO 구글 로그인 적용 시 추가
        }

        JSONObject data = new JSONObject(userInfo);
        return data.toString();
    }

    @GetMapping("/token/{social}")
    public String getToken(SocialDto socialDto, @PathVariable String social){
        // 받아온 accessToken 세팅
        //socialDto.setAccessToken(token);
        // 회원 정보
        String accessToken = null;
        if (Objects.equals(social, "kakao")) {
            accessToken = kakaoAuthService.getToken(socialDto);
        }else if (Objects.equals(social, "naver")) {
            accessToken = naverAuthService.getToken(socialDto);
        }else if (Objects.equals(social, "google")) {
            // TODO 구글 로그인 적용 시 추가
        }

        return accessToken;
    }
}
