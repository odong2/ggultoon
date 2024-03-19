package com.architecture.admin.services.auth;

import com.architecture.admin.libraries.ServerLibrary;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.services.BaseService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


/*****************************************************
 * 구글 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class GoogleAuthService extends BaseService {

    @Value("${auth.google.clientId}")
    private String googleClientId;

    @Value("${auth.google.clientSecret}")
    private String googleClientSecret;

    @Value("${auth.google.callbackUrl}")
    private String googleCallbackUrl;

    @Value("${auth.google.authUrl}")
    private String googleAuthUrl;

    @Value("${auth.google.tokenUrl}")
    private String googleTokenUrl;

    @Value("${auth.google.scope}")
    private String googleScope;

    @Value("${auth.google.userUrl}")
    private String googleUserUrl;
    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 연동 URL 생성
     *
     * @return 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String scheme = request.getScheme(); // http / https
        String serverName = request.getServerName();// 도메인만
        Integer serverPort = request.getServerPort();// 포트
        String currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인

        return googleAuthUrl + "?response_type=code&client_id=" + googleClientId + "&scope=" + googleScope + "&redirect_uri=" + currentDomain+googleCallbackUrl;
    }

    /**
     * 토큰 받아오기
     *
     * @param socialDto
     * @return accessToken 토큰 값
     */
    public String getToken(SocialDto socialDto) {
        // 도메인 받아오기
        String currentDomain = super.getCurrentDomain();

        if (socialDto == null) {
            throw new CustomException(CustomError.JOIN_SOCIAL_ERROR);
        }

        String code = socialDto.getCode();
        String accessToken = "";
        String sResultData;

        // 구글 토큰 요청
        String tokenUrl = googleTokenUrl;

        HashMap<String, String> parmas = new HashMap<>();
        parmas.put("client_id", googleClientId);
        parmas.put("client_secret", googleClientSecret);
        parmas.put("code", code);
        parmas.put("grant_type", "authorization_code");
        parmas.put("redirect_uri", currentDomain+googleCallbackUrl);

        // API에서 데이터 가져오기
        sResultData = super.postCurl(tokenUrl, parmas);

        // JSON파싱 객체 생성
        JSONObject data = new JSONObject(sResultData);

        // accessToken 추출
        accessToken = data.get("access_token").toString();

        return accessToken;
    }

    /**
     * 구글 회원 정보 받아오기
     * 
     * @param socialDto (accessToken)
     * @return
     */
    public Map<String, Object> getInfo(SocialDto socialDto) {
        // 데이터 초기화
        String sResultData;
        HashMap<String, Object> userInfo = new HashMap<>();

        // 네이버 api url
        String userUrl = googleUserUrl;
        // 토큰값 header로 전달
        String header = "Bearer " + socialDto.getAccessToken();

        // API에서 데이터 가져오기
        sResultData = super.getCurl(userUrl, header);

        String id = "";
        String email = "";
        // 조회 성공
        if ( sResultData != null ){
            JsonElement response = JsonParser.parseString(sResultData);

            // 구글 고유 아이디
            if(response.getAsJsonObject().get("id") != null) {
                id = response.getAsJsonObject().get("id").getAsString();
            }
            // 구글 이메일
            if(response.getAsJsonObject().get("email") != null) {
                email = response.getAsJsonObject().get("email").getAsString();
            }

            userInfo.put("id", id);
            userInfo.put("email", email);
        }else{
            throw new CustomException(CustomError.GOOGLE_SOCIAL_ERROR);
        }
        return userInfo;
    }
}
