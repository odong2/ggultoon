package com.architecture.admin.controllers.v1.login;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.auth.SocialDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.auth.KakaoAuthService;
import com.architecture.admin.services.auth.NaverAuthService;
import com.architecture.admin.services.login.JoinService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;
import java.util.Objects;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/join")
public class JoinV1Controller extends BaseController {
    private final NaverAuthService naverAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final JoinService joinService;
    private String joinDisabledMsg = "lang.login.exception.shutdown"; // 서비스 종료로 회원가입이 불가능해요

    /**
     * 회원 가입
     *
     * @param memberDto (id,pw,isSimple,pwConfirm)
     * @param result
     * @param httpRequest
     * @param httpResponse
     * @return
     * @throws Exception
     */
    @PostMapping("")
    public String join(@RequestBody @Valid MemberDto memberDto,
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
//        // 아이디 중복체크
//        Boolean bDupleId = joinService.checkDupleId(memberDto);
//
//        if (Boolean.TRUE.equals(bDupleId)) {
//            throw new CustomException(CustomError.ID_DUPLE);
//        }
//
//        // 가입 타입 추가
//        if(memberDto.getJoinType() == null){
//            // 디바이스 정보 set
//            String device = httpRequest.getHeader("User-Agent");
//            if (isMobile(device)) {  // 모바일
//                memberDto.setJoinType(MOBILE);
//            } else { // pc
//                memberDto.setJoinType(PC);
//            }
//        } else {
//            memberDto.setJoinType(memberDto.getJoinType());
//        }
//
//        // 회원 정보 등록
//        Long resultRegist = joinService.regist(memberDto, httpResponse);
//
//        // 회원 가입 실패 시
//        if (resultRegist < 0) {
//            throw new CustomException(CustomError.JOIN_FAIL);
//        }
//
//        // set return data
//        JSONObject data = new JSONObject();
//        //data.put("location", "/admin/login");
//
//        // return value
//        String sErrorMessage = "lang.login.success.regist";
//        String message = super.langMessage(sErrorMessage);
//        return displayJson(true, "1000", message, data);
    }
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
        // 본인인증 정보 입력
        memberDto.setTxseq(socialDto.getTxseq());
        memberDto.setMarketing(socialDto.getMarketing());
        memberDto.setPrivacy(socialDto.getPrivacy());
        memberDto.setAge(socialDto.getAge());
        memberDto.setEdata(socialDto.getEdata());
        memberDto.setIsSimple(1);

        // 가입 타입 추가
        if(socialDto.getJoinType() == null){
            // 디바이스 정보 set
            String device = httpRequest.getHeader("User-Agent");
            if (isMobile(device)) {  // 모바일
                memberDto.setJoinType(MOBILE);
            } else { // pc
                memberDto.setJoinType(PC);
            }
        }else {
            memberDto.setJoinType(socialDto.getJoinType());
        }


        Map<String, Object> userInfo;
        if (Objects.equals(social, "kakao")) {
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


        // pushAlarm(memberDto.toString(),"LJH");

        // 회원가입 처리
        Long resultRegist = joinService.regist(memberDto, httpResponse);
        // 회원가입 실패
        if (resultRegist < 0L) {
            throw new CustomException(CustomError.LOGIN_FAIL);
        }

        // set return data
        JSONObject data = new JSONObject();
       // data.put("location", "/");

        // return value
        String sErrorMessage = "lang.login.success.regist";
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }

    /**
     * ott 회원 접속 통계 입력
     *
     * @param memberDto edata 암호화 데이터
     * @param httpResponse
     * @return
     * @throws Exception
     */
    @PostMapping("/ott/visit")
    public String visit(@RequestBody @Valid MemberDto memberDto,
                        HttpServletResponse httpResponse) throws Exception {

        // 접속 통계 입력
        joinService.insertEventOtt(memberDto,"visit",0, httpResponse);

        // return value
        String sErrorMessage = "lang.common.success.regist";
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message);
    }


    @PostMapping("/test/join")
    public String testJoin(@RequestBody MemberDto memberDto) throws Exception {
        memberDto.setInsertedIdx(1111L);
        return joinService.eventCheck(memberDto).toString();
    }

    @GetMapping("/test/join2")
    public String testJoin2() throws Exception {
        //coinService.addMileage(205L,111);
        /*Map<String,String> map = new HashMap<>();
        map.put("userid","lpgux@ME@");
        map.put("gtUserid","test");
        map.put("eventType","join");
        map.put("bannerCode","test");
        map.put("isGive","N");
        map.put("isGive","old");
        return joinService.encode(new JSONObject(map).toString());*/

        return joinService.encode("{\"site\":\"me2disk\",\"returnUrl\":\"https:\\/\\/dev.me2disk.com\\/api\\/ggultoons\\/join_api.php\",\"userid\":\"lpgux@ME@\",\"bannerCode\":\"pc_main_banner\"}");
    }
}

