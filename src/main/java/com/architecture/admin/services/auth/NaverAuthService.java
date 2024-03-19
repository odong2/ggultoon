package com.architecture.admin.services.auth;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.services.BaseService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*****************************************************
 * 네이버 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class NaverAuthService extends BaseService {

    @Value("${auth.naver.clientId}")
    private String naverClientId;
    
    @Value("${auth.naver.clientSecret}")
    private String naverClientSecret;

    @Value("${auth.naver.callbackUrl}")
    private String naverCallbackUrl;

    @Value("${auth.naver.authUrl}")
    private String naverAuthUrl;

    @Value("${auth.naver.tokenUrl}")
    private String naverTokenUrl;

    @Value("${auth.naver.userUrl}")
    private String naverUserUrl;
    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 네이버 연동 URL 생성
     *
     * @return 네이버 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        String currentDomain = super.getCurrentDomain();

        // state용 난수 생성
        SecureRandom random = new SecureRandom();
        String naverState = new BigInteger(130, random).toString(32);

        return naverAuthUrl + "?response_type=code&client_id=" + naverClientId + "&redirect_uri=" + currentDomain+naverCallbackUrl + "&state=" + naverState;
    }

    /**
     * 네이버 토큰 받아오기
     *
     * @param socialDto (code,state)
     * @return accessToken 토큰 값
     */
    public String getToken(SocialDto socialDto) {
        if (socialDto == null) {
            throw new CustomException(CustomError.JOIN_SOCIAL_ERROR);
        }

        String code = socialDto.getCode();
        String state = socialDto.getState();
        String accessToken = "";
        String sResultData;

        // 네이버 토큰 요청
        String tokenUrl = naverTokenUrl + "?grant_type=authorization_code&client_id=" + naverClientId + "&client_secret=" + naverClientSecret + "&code=" + code + "&state=" + state;

        // API에서 데이터 가져오기
        sResultData = super.getCurl(tokenUrl, "");

        // JSON파싱 객체 생성
        JSONObject data = new JSONObject(sResultData);

        // accessToken 추출
        accessToken = data.get("access_token").toString();

        return accessToken;
    }

    /**
     * 네이버에서 회원 정보 받아오기
     * 
     * @param socialDto (accessToken)
     * @return
     */
    public Map<String, Object> getInfo(SocialDto socialDto) {
        // 데이터 초기화
        String sResultData;
        JsonObject response;
        HashMap<String, Object> userInfo = new HashMap<>();

        // 네이버 api url
        String userUrl = naverUserUrl;
        // 토큰값 header로 전달
        String header = "Bearer " + socialDto.getAccessToken();

        // API에서 데이터 가져오기
        sResultData = super.getCurl(userUrl, header);

        // JSON파싱 객체 생성
        JSONObject data = new JSONObject(sResultData);
        // 조회 성공
        if (Objects.equals(data.get("resultcode").toString(), "00") && Objects.equals(data.get("message").toString(), "success")) {
            JsonElement element = JsonParser.parseString(sResultData);
            // response 데이터만 받아오기
            response = element.getAsJsonObject().get("response").getAsJsonObject();

            String id = "";
            String email = "";
            String name = "";
            String mobile = "";
            String gender = "";
            String birthyear = "";
            String birthday = "";

            // 네이버 고유 아이디
            if(response.getAsJsonObject().get("id") != null) {
                id = response.getAsJsonObject().get("id").getAsString();
            }
            // 네이버 이메일
            if(response.getAsJsonObject().get("email") != null) {
                email = response.getAsJsonObject().get("email").getAsString();
            }
            // 네이버 회원 이름
            if(response.getAsJsonObject().get("name") != null) {
                name = response.getAsJsonObject().get("name").getAsString();
            }
            // 네이버  핸드폰 번호
            if(response.getAsJsonObject().get("mobile") != null) {
                mobile = response.getAsJsonObject().get("mobile").getAsString();
            }
            // 네이버 성별
            if(response.getAsJsonObject().get("gender") != null) {
                gender = response.getAsJsonObject().get("gender").getAsString();
            }
            // 네이버 생일연도
            if(response.getAsJsonObject().get("birthyear") != null) {
                birthyear = response.getAsJsonObject().get("birthyear").getAsString();
            }
            // 네이버 생일
            if(response.getAsJsonObject().get("birthday") != null) {
               birthday = response.getAsJsonObject().get("birthday").getAsString();
            }

            userInfo.put("id", id);
            userInfo.put("name", name);
            userInfo.put("email", email);
            userInfo.put("phone", mobile);
            userInfo.put("gender", gender);
            if(!Objects.equals(birthyear, "") && !Objects.equals(birthday, "")) {
                userInfo.put("birthday", birthyear + "-" + birthday);
            }
        }else{
            throw new CustomException(CustomError.NAVER_SOCIAL_ERROR);
        }
        return userInfo;
    }
}
