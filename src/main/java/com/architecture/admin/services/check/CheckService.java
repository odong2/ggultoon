package com.architecture.admin.services.check;

import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.services.BaseService;
import kcb.module.v3.exception.OkCertException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;


/*****************************************************
 * 본인인증
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class CheckService extends BaseService {

    private final MemberDao memberDao;

    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 휴대폰인증 URL 생성
     *
     * @return 네이버 연동 URL
     */
    public String getUrl(String returnType, String returnUrl) throws Exception {
        // 도메인 받아오기
        /**************************************************************************
         * okcert3 휴대폰 본인확인 서비스 파라미터
         **************************************************************************/

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 회원사 사이트명, URL
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String SITE_NAME = "꿀툰";        // 요청사이트명
        String SITE_URL = "ggultoons.com";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' KCB로부터 부여받은 회원사코드(아이디) 설정 (12자리)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String CP_CD = "V61410000000";    // 회원사코드
        //session.setAttribute("PHONE_CP_CD", CP_CD);

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 리턴 URL 설정
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' opener(popup1)의 도메인과 일치하도록 설정해야 함.
        //' (http://www.test.co.kr과 http://test.co.kr는 다른 도메인으로 인식하며, http 및 https도 일치해야 함)

        String RETURN_URL = super.getCurrentDomain() + "/v1/check/" + returnType;// 인증 완료 후 리턴될 URL (도메인 포함 full path)

        // 아이디 찾기일 경우에만 리턴 URL 파라미터 값으로 지정
        if(returnType.equals("find/id")) {
            RETURN_URL = returnUrl;// 인증 완료 후 리턴될 URL (도메인 포함 full path)
        }

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 인증요청사유코드 (가이드 문서 참조)
        //  00 : 회원가입
        //  01 : 성인인증
        //  02 : 회원정보수정
        //  03 : 아이디 찾기(임의로 추가) & 비밀번호 찾기
        //  04 : 상품구매
        //  99 : 기타
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String RQST_CAUS_CD;
        switch(returnType) {
            case "join":
                RQST_CAUS_CD = "00";
                break;
            case "adult":
                RQST_CAUS_CD = "01";
                break;
            case "info":
                RQST_CAUS_CD = "02";
                break;
            case "find/id", "find/password":
                RQST_CAUS_CD = "03";
                break;
            default:
                RQST_CAUS_CD = "99";
                break;
        }

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 채널 코드 (공백가능. 필요한 회원사에서만 입력)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //String CHNL_CD = request.getParameter("CHNL_CD");
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 리턴메시지 (공백가능. returnUrl에서 같이 전달받고자 하는 값이 있다면 설정.)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String RETURN_MSG = returnUrl;

        //' ########################################################################
        //' # 타겟 및 팝업URL : 운영/테스트 전환시 변경 필요
        //' ########################################################################
        String target = "PROD";    // 테스트="TEST", 운영="PROD"
        //String popupUrl = "";	// 테스트 URL
        String popupUrl = "https://safe.ok-name.co.kr/CommonSvl";// 운영 URL

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 라이센스 파일
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

       String license = "/okcert3/" + CP_CD + "_IDS_01_" + target + "_AES_license.dat";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 서비스명 (고정값)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String svcName = "IDS_HS_POPUP_START";

        /**************************************************************************
         okcert3 요청 정보
         **************************************************************************/
        kcb.org.json.JSONObject reqJson = new kcb.org.json.JSONObject();
        reqJson.put("RETURN_URL", RETURN_URL);
        reqJson.put("SITE_NAME", SITE_NAME);
        reqJson.put("SITE_URL", SITE_URL);
        reqJson.put("RQST_CAUS_CD", RQST_CAUS_CD);
        reqJson.put("RETURN_MSG", RETURN_MSG);
        //reqJson.put("CHNL_CD", CHNL_CD);

        //' 거래일련번호는 기본적으로 모듈 내에서 자동 채번되고 채번된 값을 리턴해줌.
        //'	회원사가 직접 채번하길 원하는 경우에만 아래 코드를 주석 해제 후 사용.
        //' 각 거래마다 중복 없는 String 을 생성하여 입력. 최대길이:20
        //reqJson.put("TX_SEQ_NO", "123456789012345");

        String reqStr = reqJson.toString();

        /**************************************************************************
         okcert3 실행
         **************************************************************************/
        kcb.module.v3.OkCert okcert = new kcb.module.v3.OkCert();

        // '************ IBM JDK 사용 시, 주석 해제하여 호출 ************
        // okcert.setProtocol2type("22");
        // '객체 내 license를 리로드해야 될 경우에만 주석 해제하여 호출. (v1.1.7 이후 라이센스는 파일위치를 key로 하여 static HashMap으로 사용됨)
        // okcert.delLicense(license);

        //' callOkCert 메소드호출 : String license 파일 path로 라이센스 로드
        //String resultStr = okcert.callOkCert(target, CP_CD, svcName, license, reqStr);

        // 'OkCert3 내부에서 String license 파일 path로 라이센스를 못 읽어올 경우(Executable Jar 환경 등에서 발생),
        // '메소드 마지막 파라미터에 InputStream를 사용하여 라이센스 로드
        String resultStr = null;
        if ( ! okcert.containsLicense(license) ) {            // 로드된 라이센스 정보가 HashMap에 없는 경우
            //java.io.InputStream is = new java.io.FileInputStream(license);    // 환경에 맞게 InputStream 로드
            //jar파일 빌드 시 경로 문제로 getResourceAsStream 사용
            InputStream is = this.getClass().getResourceAsStream(license);

            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr, is);
        } else {                                            // 로드된 라이센스 정보가 HashMap에 있는 경우
            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr);
        }

        kcb.org.json.JSONObject resJson = new kcb.org.json.JSONObject(resultStr);

        String RSLT_CD = resJson.getString("RSLT_CD");
        String RSLT_MSG = resJson.getString("RSLT_MSG");
        //if(resJson.has("TX_SEQ_NO")) String TX_SEQ_NO = resJson.getString("TX_SEQ_NO"); // 필요 시 거래 일련 번호 에 대하여 DB저장 등의 처리
        String MDL_TKN = "";

        boolean succ = false;

        if ("B000".equals(RSLT_CD) && resJson.has("MDL_TKN")) {
            MDL_TKN = resJson.getString("MDL_TKN");
            succ = true;
        }

        return popupUrl + "?mdl_tkn=" + MDL_TKN + "&cp_cd=" + CP_CD + "&tc=kcb.oknm.online.safehscert.popup.cmd.P931_CertChoiceCmd";
    }

    /**
     * 휴대폰인증 URL 생성
     *
     * @return 네이버 연동 URL
     */
    public String getOkCertUrl(String returnType, String returnUrl, String inputId) throws Exception {
        // 도메인 받아오기
        /**************************************************************************
         * okcert3 휴대폰 본인확인 서비스 파라미터
         **************************************************************************/

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 회원사 사이트명, URL
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String SITE_NAME = "꿀툰";        // 요청사이트명
        String SITE_URL = "ggultoons.com";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' KCB로부터 부여받은 회원사코드(아이디) 설정 (12자리)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String CP_CD = "V61410000000";    // 회원사코드
        //session.setAttribute("PHONE_CP_CD", CP_CD);

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 리턴 URL 설정
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' opener(popup1)의 도메인과 일치하도록 설정해야 함.
        //' (http://www.test.co.kr과 http://test.co.kr는 다른 도메인으로 인식하며, http 및 https도 일치해야 함)
        String RETURN_URL = super.getCurrentDomain() + "/v1/check/" + returnType;// 인증 완료 후 리턴될 URL (도메인 포함 full path)

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 인증요청사유코드 (가이드 문서 참조)
        //  00 : 회원가입
        //  01 : 성인인증
        //  02 : 회원정보수정
        //  03 : 아이디 찾기(임의로 추가) & 비밀번호 찾기
        //  04 : 상품구매
        //  99 : 기타
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String RQST_CAUS_CD;
        switch(returnType) {
            case "join":
                RQST_CAUS_CD = "00";
                break;
            case "adult":
                RQST_CAUS_CD = "01";
                break;
            case "info":
                RQST_CAUS_CD = "02";
                break;
            case "find/id", "find/password":
                RQST_CAUS_CD = "03";
                break;
            default:
                RQST_CAUS_CD = "99";
                break;
        }

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 채널 코드 (공백가능. 필요한 회원사에서만 입력)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //String CHNL_CD = request.getParameter("CHNL_CD");
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 리턴메시지 (공백가능. returnUrl에서 같이 전달받고자 하는 값이 있다면 설정.)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String RETURN_MSG = returnUrl + "&" + inputId;
        
        //' ########################################################################
        //' # 타겟 및 팝업URL : 운영/테스트 전환시 변경 필요
        //' ########################################################################
        String target = "PROD";    // 테스트="TEST", 운영="PROD"
        //String popupUrl = "";	// 테스트 URL
        String popupUrl = "https://safe.ok-name.co.kr/CommonSvl";// 운영 URL

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 라이센스 파일
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String license = "/okcert3/" + CP_CD + "_IDS_01_" + target + "_AES_license.dat";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 서비스명 (고정값)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String svcName = "IDS_HS_POPUP_START";

        /**************************************************************************
         okcert3 요청 정보
         **************************************************************************/
        kcb.org.json.JSONObject reqJson = new kcb.org.json.JSONObject();
        reqJson.put("RETURN_URL", RETURN_URL);
        reqJson.put("SITE_NAME", SITE_NAME);
        reqJson.put("SITE_URL", SITE_URL);
        reqJson.put("RQST_CAUS_CD", RQST_CAUS_CD);
        reqJson.put("RETURN_MSG", RETURN_MSG);
        //reqJson.put("CHNL_CD", CHNL_CD);

        //' 거래일련번호는 기본적으로 모듈 내에서 자동 채번되고 채번된 값을 리턴해줌.
        //'	회원사가 직접 채번하길 원하는 경우에만 아래 코드를 주석 해제 후 사용.
        //' 각 거래마다 중복 없는 String 을 생성하여 입력. 최대길이:20
        //reqJson.put("TX_SEQ_NO", "123456789012345");

        String reqStr = reqJson.toString();

        /**************************************************************************
         okcert3 실행
         **************************************************************************/
        kcb.module.v3.OkCert okcert = new kcb.module.v3.OkCert();

        // '************ IBM JDK 사용 시, 주석 해제하여 호출 ************
        // okcert.setProtocol2type("22");
        // '객체 내 license를 리로드해야 될 경우에만 주석 해제하여 호출. (v1.1.7 이후 라이센스는 파일위치를 key로 하여 static HashMap으로 사용됨)
        // okcert.delLicense(license);

        //' callOkCert 메소드호출 : String license 파일 path로 라이센스 로드
        //String resultStr = okcert.callOkCert(target, CP_CD, svcName, license, reqStr);

        // 'OkCert3 내부에서 String license 파일 path로 라이센스를 못 읽어올 경우(Executable Jar 환경 등에서 발생),
        // '메소드 마지막 파라미터에 InputStream를 사용하여 라이센스 로드
        String resultStr = null;
        if ( ! okcert.containsLicense(license) ) {            // 로드된 라이센스 정보가 HashMap에 없는 경우
            //java.io.InputStream is = new java.io.FileInputStream(license);    // 환경에 맞게 InputStream 로드
            //jar파일 빌드 시 경로 문제로 getResourceAsStream 사용
            InputStream is = this.getClass().getResourceAsStream(license);
            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr, is);
        } else {                                            // 로드된 라이센스 정보가 HashMap에 있는 경우
            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr);
        }

        kcb.org.json.JSONObject resJson = new kcb.org.json.JSONObject(resultStr);

        String RSLT_CD = resJson.getString("RSLT_CD");
        String RSLT_MSG = resJson.getString("RSLT_MSG");
        //if(resJson.has("TX_SEQ_NO")) String TX_SEQ_NO = resJson.getString("TX_SEQ_NO"); // 필요 시 거래 일련 번호 에 대하여 DB저장 등의 처리
        String MDL_TKN = "";

        boolean succ = false;

        if ("B000".equals(RSLT_CD) && resJson.has("MDL_TKN")) {
            MDL_TKN = resJson.getString("MDL_TKN");
            succ = true;
        }
        return popupUrl + "?mdl_tkn=" + MDL_TKN + "&cp_cd=" + CP_CD + "&tc=kcb.oknm.online.safehscert.popup.cmd.P931_CertChoiceCmd";
    }

    /**
     * 휴대폰인증 URL 생성
     *
     * @return 회원 인증 정보
     */
    public JSONObject getInfo(String MDL_TKN) throws OkCertException {

        //**************************************************************************
        // - 팝업페이지
        // 휴대폰 본인확인 인증 결과 화면(return url).
        // 암호화된 인증결과정보를 복호화한다.
        //**************************************************************************

        // 처리결과 모듈 토큰 정보
        //String MDL_TKN = request.getParameter("mdl_tkn");
        
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' KCB로부터 부여받은 회원사코드(아이디) 설정 (12자리)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //String CP_CD = "V06880000000";	// 회원사코드
        String CP_CD = "V61410000000";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 타겟 : 운영/테스트 전환시 변경 필요
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String target = "PROD";    // 테스트="TEST", 운영="PROD"

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 라이센스 파일
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //String license = "C:\\okcert3_license\\" + CP_CD + "_IDS_01_" + target + "_AES_license.dat";
        String license = "/okcert3/" + CP_CD + "_IDS_01_" + target + "_AES_license.dat";

        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        //' 서비스명 (고정값)
        //'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
        String svcName = "IDS_HS_POPUP_RESULT";

        /**************************************************************************
         okcert3 요청 정보
         **************************************************************************/
        JSONObject reqJson = new JSONObject();
        reqJson.put("MDL_TKN", MDL_TKN);
        String reqStr = reqJson.toString();

        /**************************************************************************
         okcert3 실행
         **************************************************************************/
        kcb.module.v3.OkCert okcert = new kcb.module.v3.OkCert();

        // '************ IBM JDK 사용 시, 주석 해제하여 호출 ************
        // okcert.setProtocol2type("22");
        // '객체 내 license를 리로드해야 될 경우에만 주석 해제하여 호출. (v1.1.7 이후 라이센스는 파일위치를 key로 하여 static HashMap으로 사용됨)
        // okcert.delLicense(license);

        //' callOkCert 메소드호출 : String license 파일 path로 라이센스 로드
        //String resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr);

        // 'OkCert3 내부에서 String license 파일 path로 라이센스를 못 읽어올 경우(Executable Jar 환경 등에서 발생),
        // '메소드 마지막 파라미터에 InputStream를 사용하여 라이센스 로드
        String resultStr;
        if ( ! okcert.containsLicense(license) ) {			// 로드된 라이센스 정보가 HashMap에 없는 경우
            //java.io.InputStream is = new java.io.FileInputStream(license);	// 환경에 맞게 InputStream 로드
            //jar파일 빌드 시 경로 문제로 getResourceAsStream 사용
            InputStream is = this.getClass().getResourceAsStream(license);
            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr, is);
        } else {											// 로드된 라이센스 정보가 HashMap에 있는 경우
            resultStr = okcert.callOkCert(target, CP_CD, svcName, license,  reqStr);
        }

        JSONObject resJson = new JSONObject(resultStr);
    /*
    String RSLT_CD =  resJson.getString("RSLT_CD");
    String RSLT_MSG =  resJson.getString("RSLT_MSG");
    String TX_SEQ_NO =  resJson.getString("TX_SEQ_NO");

    String RSLT_NAME = "";
    String RSLT_BIRTHDAY = "";
    String RSLT_SEX_CD = "";
    String RSLT_NTV_FRNR_CD = "";

    String DI = "";
    String CI = "";
    String CI_UPDATE = "";
    String TEL_COM_CD = "";
    String TEL_NO = "";

    String RETURN_MSG= "";
    if(resJson.has("RETURN_MSG")) RETURN_MSG =  resJson.getString("RETURN_MSG");

    if ("B000".equals(RSLT_CD)){
        RSLT_NAME = resJson.getString("RSLT_NAME");
        RSLT_BIRTHDAY = resJson.getString("RSLT_BIRTHDAY");
        RSLT_SEX_CD = resJson.getString("RSLT_SEX_CD");
        RSLT_NTV_FRNR_CD = resJson.getString("RSLT_NTV_FRNR_CD");

        DI = resJson.getString("DI");
        CI = resJson.getString("CI");
        CI_UPDATE = resJson.getString("CI_UPDATE");
        TEL_COM_CD = resJson.getString("TEL_COM_CD");
        TEL_NO = resJson.getString("TEL_NO");
    }
    */
        // 본인인증 로그 테이블에 insert
        if (resJson.has("CI") && resJson.has("DI")) {

            HashMap<String, String> okCertLog = new HashMap<>();
            okCertLog.put("information", resJson.toString());
            okCertLog.put("regdate", dateLibrary.getDatetime());
            memberDao.insertLog(okCertLog);
        }
        return resJson;
    }
}
