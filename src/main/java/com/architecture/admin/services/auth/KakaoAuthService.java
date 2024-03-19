package com.architecture.admin.services.auth;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/*****************************************************
 * 카카오 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class KakaoAuthService extends BaseService {

    @Value("${auth.kakao.clientId}")
    private String kakaoClientId;
    
    @Value("${auth.kakao.clientSecret}")
    private String kakaoClientSecret;

    @Value("${auth.kakao.callbackUrl}")
    private String kakaoCallbackUrl;

    @Value("${auth.kakao.authUrl}")
    private String kakaoAuthUrl;

    @Value("${auth.kakao.tokenUrl}")
    private String kakaoTokenUrl;

    @Value("${auth.kakao.userUrl}")
    private String kakaoUserUrl;
    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 카카오 연동 URL 생성
     *
     * @return 카카오 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        String currentDomain = super.getCurrentDomain();

        // state용 난수 생성
        SecureRandom random = new SecureRandom();
        //String kakaoState = new BigInteger(130, random).toString(32);

        return kakaoAuthUrl + "?response_type=code&client_id=" + kakaoClientId + "&redirect_uri=" + currentDomain+kakaoCallbackUrl + "&response_type=code";
    }

    /**
     * 카카오 토큰 받아오기
     *
     * @param socialDto (code,state)
     * @return accessToken 토큰 값
     */
    public String getToken(SocialDto socialDto) {
        if (socialDto == null) {
            throw new CustomException(CustomError.JOIN_SOCIAL_ERROR);
        }

        String code = socialDto.getCode();
        //String state = socialDto.getState();
        String accessToken = "";
        String sResultData;

        // 카카오 토큰 요청
        String tokenUrl = kakaoTokenUrl + "?grant_type=authorization_code&client_id=" + kakaoClientId + "&client_secret=" + kakaoClientSecret + "&code=" + code;

        // API에서 데이터 가져오기
        sResultData = super.getCurl(tokenUrl, "");

        // JSON파싱 객체 생성
        JSONObject data = new JSONObject(sResultData);

        // accessToken 추출
        accessToken = data.get("access_token").toString();

        return accessToken;
    }

    /**
     * 카카오에서 회원 정보 받아오기
     * 
     * @param socialDto (accessToken)
     * @return
     */
    public Map<String, Object> getInfo(SocialDto socialDto) {
        // 데이터 초기화
        String sResultData;
        HashMap<String, Object> userInfo = new HashMap<>();

        // 카카오 api url
        String userUrl = kakaoUserUrl;
        // 토큰값 header로 전달
        String header = "Bearer " + socialDto.getAccessToken();

        // API에서 데이터 가져오기
        sResultData = super.getCurl(userUrl, header);

        String id = "";
        String email = "";
        // 조회 성공
        if (sResultData != null) {

            JSONObject response = new JSONObject(sResultData);
            //pushAlarm(response.toString(),"LJH");

            // 카카오 고유 아이디
            if (response.has("id")) {
                long a = response.getLong("id");
                id = Long.toString(a);
                userInfo.put("id", id);
            }
            // 카카오 이메일
            if (response.getJSONObject("kakao_account").has("email")) {
                email = response.getJSONObject("kakao_account").getString("email");
                userInfo.put("email", email);
            }
        } else {
            throw new CustomException(CustomError.GOOGLE_SOCIAL_ERROR);
        }
        return userInfo;
    }
}
