package com.architecture.admin.controllers.v1.check;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.check.CheckService;
import com.architecture.admin.services.member.MemberService;
import kcb.module.v3.exception.OkCertException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/v1/check")
@RequiredArgsConstructor
public class CheckV1Controller extends BaseController {
    //Cache-Control 추가
    /*@ModelAttribute
    public void setResponseHeader(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
    }*/

    private final CheckService checkService;

    private final MemberService memberService;

    /**
     * 회원가입 본인인증 팝업
     *
     * @return 본인인증 URL으로 이동
     * @throws Exception
     */
    @GetMapping("/popup")
    public void popup(@RequestParam(value = "url", defaultValue = "/") String url,
                      HttpServletResponse response) throws Exception {

        response.sendRedirect(checkService.getUrl("join", url));
    }

    /**
     * 아이디 찾기 본인인증 팝업
     *
     * @return 본인인증 URL으로 이동
     * @throws Exception
     */
    @GetMapping("/popup/id")
    public void popupFindId(@RequestParam(value = "url", defaultValue = "/") String url,
                            HttpServletResponse response) throws Exception {

        response.sendRedirect(checkService.getUrl("find/id", url));
    }

    /**
     * 비밀번호 찾기 - 입력한 아이디 유효성 검사
     *
     * @param memberDto : 입력한 아이디
     * @return 유효성 검사 결과 반환
     */
    @PostMapping("/id")
    public String checkInputId(@RequestBody MemberDto memberDto) {

        // 회원이 입력한 아이디
        String inputId = memberDto.getId();

        // 회원이 입력한 아이디 정보가 없는 경우
        if (inputId == null || inputId.isEmpty()) {
            throw new CustomException(CustomError.ID_EMPTY); // 아이디를 입력해주세요.
        }

        // DB에 존재하지 않는 아이디를 입력한 경우
        int idCount = memberService.getCountById(inputId);
        if (idCount < 1) {
            throw new CustomException(CustomError.ID_CORRESPOND_ERROR); // 입력하신 아이디를 찾을 수 없습니다.
        }

        // 결과 메시지 처리
        String message = super.langMessage("lang.member.success.find.id"); // 아이디를 찾았습니다.

        // return value
        return displayJson(true, "1000", message);
    }

    /**
     * 비밀번호 찾기 본인인증 팝업
     *
     * @return 본인인증 URL으로 이동
     * @throws Exception
     */
    @GetMapping("/popup/password")
    public void popupFindPassword(@RequestParam(value = "url", defaultValue = "/") String url,
                                  @RequestParam(value = "inputId") String inputId,
                                  HttpServletResponse response) throws Exception {

        response.sendRedirect(checkService.getOkCertUrl("find/password", url, inputId));
    }

    /**
     * 본인인증 결과페이지
     *
     * @return 본인인증 완료 URL으로 이동
     */
    @GetMapping("join")
    public void result(HttpServletRequest request, HttpServletResponse response) throws OkCertException, IOException {
        String url = "/";
        String res;
        String msg;
        String txseq = "";

        //인증정보 가져오기
        //처리결과 모듈 토큰 정보
        String MDL_TKN = request.getParameter("mdl_tkn");
        JSONObject resJson = checkService.getInfo(MDL_TKN);

        // 본인 인증이 정상적으로 처리된 경우
        if (!resJson.isNull("CI") && !resJson.isNull("DI") && resJson.getString("RSLT_CD").equals("B000")) {

            MemberDto memberCi = new MemberDto();
            memberCi.setCi(resJson.getString("CI"));
            memberCi.setDi(resJson.getString("DI"));

            /*//가입여부 확인
            List<MemberDto> duplicateCi = memberService.getMemberInfoByCiDi(memberCi);

            if(!duplicateCi.isEmpty()) {
                url = resJson.getString("RETURN_MSG");
                msg = URLEncoder.encode("이미 가입한 아이디가 있습니다.",StandardCharsets.UTF_8);

            }else {*/
            //회원정보 입력
            MemberDto memberDto = new MemberDto();
            memberDto.setCi(resJson.getString("CI"));
            memberDto.setTxseq(resJson.getString("TX_SEQ_NO"));
            memberDto.setCi(resJson.getString("CI"));
            memberDto.setDi(resJson.getString("DI"));
            memberDto.setName(resJson.getString("RSLT_NAME"));
            memberDto.setBirth(resJson.getString("RSLT_BIRTHDAY"));
            memberDto.setGender(resJson.getString("RSLT_SEX_CD"));
            memberDto.setCom(resJson.getString("TEL_COM_CD"));
            memberDto.setPhone(resJson.getString("TEL_NO"));
            memberDto.setIp(resJson.getString("REMOTE_IP"));
            memberDto.setRegdate(dateLibrary.getDatetime());
            if (!(resJson.getString("RETURN_MSG")).equals("")) {
                url = resJson.getString("RETURN_MSG");
            }

            if (memberDto.getBirth() != null) {
                // adult 업데이트 - 생년월일 기준으로 성인 여부 확인
                String brith = memberDto.getBirth();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                int today = Integer.parseInt(formatter.format(new Date()));
                int date = Integer.parseInt(brith.replace("-", ""));
                int age = today - date; // 만나이
                if (age < 140000) {
                    // 14세 미만 알림
                    pushAlarm(Integer.toString(age), "LJH");

                    // 14세 미만 회원가입 불가
                    res = "fail";
                    msg = URLEncoder.encode(super.langMessage("lang.member.exception.age14"), StandardCharsets.UTF_8); // 14세 이상 이용 가능 합니다.
                    response.sendRedirect(url + "?res=" + res + "&msg=" + msg + "&txseq=" + txseq);
                    return;
                }
            }
            // 회원가입 본인인증 결과 insert
            memberService.insertMemberInfo(memberDto);

            // 회원가입 본인인증 성공
            res = "success";
            msg = URLEncoder.encode(resJson.getString("RSLT_MSG"), StandardCharsets.UTF_8);
            txseq = resJson.getString("TX_SEQ_NO");
            response.sendRedirect(url + "?res=" + res + "&msg=" + msg + "&txseq=" + txseq);
            return;
            //}
        }
        // 회원가입 본인인증 실패
        url = resJson.getString("RETURN_MSG");
        res = "fail";
        msg = URLEncoder.encode(super.langMessage("lang.member.exception.identification"), StandardCharsets.UTF_8); // 본인인증에 실패하였습니다.

        // 사용자가 본인인증 취소한 경우
        if (resJson.getString("RSLT_CD").equals("B123")) {
            msg = URLEncoder.encode(super.langMessage("lang.member.exception.cancel.identification"), StandardCharsets.UTF_8); // 본인인증을 취소하였습니다.
        }

        response.sendRedirect(url + "?res=" + res + "&msg=" + msg + "&txseq=" + txseq);
    }

    @RequestMapping("cookie")
    public String cookie(HttpServletResponse httpResponse) {
        Cookie refreshToken = new Cookie("TEST", "TEST"); // 쿠키 이름 지정하여 생성( key, value 개념)
        refreshToken.setMaxAge(60 * 60 * 60);
        refreshToken.setPath("/");
        refreshToken.setSecure(true);
        refreshToken.setHttpOnly(true);
        httpResponse.addCookie(refreshToken);

        return hmServer.get("currentDomain").toString();
    }

    /**
     * 아이디 찾기
     *
     * @param mdl_tkn : 본인인증 모듈 토큰 정보
     */
    @GetMapping("/find/id")
    public String findMemberId(@RequestParam(value = "mdl_tkn") String mdl_tkn) throws OkCertException {

        // return value
        String code = CustomError.FIND_MEMBER_ID_NOT_EXIST.getCode(); // EFIN-9999
        String message = CustomError.FIND_MEMBER_ID_NOT_EXIST.getMessage(); // 아이디를 찾을 수 없습니다.

        // 본인인증 처리결과 모듈 토큰 정보가 있을 경우
        if (mdl_tkn != null && !mdl_tkn.isEmpty()) {

            // 인증정보 조회
            JSONObject resJson = checkService.getInfo(mdl_tkn);

            // 본인 인증이 정상적으로 처리된 경우
            if (!resJson.isNull("CI") && !resJson.isNull("DI") && resJson.getString("RSLT_CD").equals("B000")) {

                MemberDto member = new MemberDto();
                member.setCi(resJson.getString("CI"));
                member.setDi(resJson.getString("DI"));

                // 가입여부 확인(CI, DI 정보로 DB 조회)
                List<MemberDto> findMember = memberService.getMemberInfoByCiDi(member);

                // 가입한 회원을 찾았다면
                if (!findMember.isEmpty()) {

                    // return value
                    JSONObject data = new JSONObject();
                    List<MemberDto> findMemberList = new ArrayList<>();

                    // 가입한 회원 계정 목록 순회
                    for (MemberDto dto : findMember) {

                        MemberDto findDto = new MemberDto();

                        // 일반 가입 회원이라면
                        if (dto.getIsSimple() == 0) {

                            findDto.setSimpleType("일반"); // 일반 가입 구분
                            findDto.setId(dto.getId()); // 일반 가입 아이디
                            findDto.setEmail(dto.getEmail()); // 일반 가입 이메일
                            findDto.setRegdate(dateLibrary.getDay(dto.getRegdate())); // 회원 일반 계정 등록일

                            // 소셜 가입 회원이라면
                        } else if (dto.getIsSimple() == 1) {

                            findDto.setSimpleType(dto.getSimpleType()); // 소셜 가입 경로(naver/kakao)
                            findDto.setId(dto.getId()); // 소셜 가입 아이디
                            findDto.setEmail(dto.getEmail()); // 소셜 가입 이메일
                            findDto.setRegdate(dateLibrary.getDay(dto.getRegdate())); // 회원 소셜 계정 등록일
                        }
                        // 리턴할 리스트에 담기
                        findMemberList.add(findDto);
                    }
                    // 찾은 아이디 목록
                    data.put("findMemberList", findMemberList);

                    // 아이디 찾기 성공
                    message = super.langMessage("lang.member.success.find.id"); // 아이디를 찾았습니다.
                    return displayJson(true, "1000", message, data);
                }
            } else {

                // 사용자가 본인인증 취소한 경우
                if (resJson.getString("RSLT_CD").equals("B123")) {
                    message = super.langMessage("lang.member.exception.cancel.identification"); // 본인인증을 취소하였습니다.
                }
            }
        }
        // 아이디 찾기 실패
        return displayJson(false, code, message);
    }

    /**
     * 본인인증 결과페이지
     * 비밀번호 찾기
     *
     * @return 본인인증 완료 URL으로 이동
     */
    @GetMapping("find/password")
    public void findMemberPwd(HttpServletRequest request,
                              HttpServletResponse response) throws OkCertException, IOException {

        // 리다이렉트할 데이터
        String url = "/";
        String res;
        String msg;
        String code;
        StringBuilder sb = new StringBuilder();

        // 인증정보 가져오기
        // 처리결과 모듈 토큰 정보
        String MDL_TKN = request.getParameter("mdl_tkn");
        JSONObject resJson = checkService.getInfo(MDL_TKN);

        // 입력받은 아이디 값 & 리턴 url 정보 가져오기
        int index = resJson.getString("RETURN_MSG").indexOf("&");
        String inputId = resJson.getString("RETURN_MSG").substring(index + 1);
        String returnUrl = resJson.getString("RETURN_MSG").substring(0, index);

        // 본인 인증이 정상적으로 처리된 경우
        if (!resJson.isNull("CI") && !resJson.isNull("DI") && resJson.getString("RSLT_CD").equals("B000")) {

            MemberDto member = new MemberDto();
            member.setCi(resJson.getString("CI"));
            member.setDi(resJson.getString("DI"));

            // 가입여부 확인(CI, DI 정보로 DB 조회)
            List<MemberDto> findMember = memberService.getMemberInfoByCiDi(member);

            // 가입한 회원을 찾았다면
            if (!findMember.isEmpty()) {

                // 가입한 회원 계정 목록 순회
                for (MemberDto dto : findMember) {

                    // 입력받은 아이디와 DB에 저장된 아이디가 동일하다면
                    if (inputId.equals(dto.getId())) {

                        // 일반 가입 회원인 경우
                        if (dto.getIsSimple() == 0) {

                            // 찾은 아이디 & ci & di 정보 반환
                            String id = dto.getId(); // 회원 아이디
                            String ci = dto.getCi(); // 회원 ci 정보
                            String di = dto.getDi(); // 회원 di 정보
                            sb.append("&id=" + id);
                            sb.append("&ci=" + ci);
                            sb.append("&di=" + di);

                            // 소셜 가입 회원인 경우
                        } else if (dto.getIsSimple() == 1) {

                            // 소셜 가입 구분값(naver/kakao) 반환
                            String simpleType = dto.getSimpleType();
                            sb.append("&simpleType=" + simpleType);
                        }
                        // 비밀번호 찾기 성공
                        if (!(resJson.getString("RETURN_MSG")).equals("")) {
                            url = returnUrl;
                        }
                        // return value
                        res = "success";
                        code = "1000";
                        msg = URLEncoder.encode(super.langMessage("lang.member.success.find.password"), StandardCharsets.UTF_8); // 비밀번호를 찾았습니다.
                        String paramUrl = sb.toString();
                        response.sendRedirect(url + "?res=" + res + "&code=" + code + "&msg=" + msg + paramUrl);
                        return;
                    }
                }
            }
        }
        // 비밀번호 찾기 실패
        url = returnUrl;
        res = "fail";
        code = URLEncoder.encode(CustomError.FIND_MEMBER_ID_NOT_EXIST.getCode(), StandardCharsets.UTF_8);  // EFIN-9998
        msg = URLEncoder.encode(CustomError.FIND_MEMBER_PASSWORD_NOT_EXIST.getMessage(), StandardCharsets.UTF_8);// 비밀번호를 찾을 수 없습니다.

        // 사용자가 본인인증 취소한 경우
        if (resJson.getString("RSLT_CD").equals("B123")) {
            msg = URLEncoder.encode(super.langMessage("lang.member.exception.cancel.identification"), StandardCharsets.UTF_8); // 본인인증을 취소하였습니다.
        }
        response.sendRedirect(url + "?res=" + res + "&code=" + code + "&msg=" + msg);
    }

    /**
     * 비밀번호 찾기 성공 > 비밀번호 재설정
     *
     * @param memberDto 비밀번호 찾기를 통해 리턴받은 회원의 id, ci, di 정보 + 입력받은 newPassword(변경할 비밀번호), newPasswordConfirm(비밀번호 확인)
     * @return
     */
    @PutMapping("/reset/password")
    public String modifyPasswordNonLogin(@RequestBody MemberDto memberDto) {

        // displayJson 기본값 설정(실패값)
        String message = CustomError.FIND_MEMBER_PASSWORD_RESET_FAIL.getMessage(); // 비밀번호를 재설정할 수 없습니다.
        String code = CustomError.FIND_MEMBER_PASSWORD_RESET_FAIL.getCode(); // EFIN-9997

        // 인증정보의 CI, DI 정보 확인
        if (!memberDto.getCi().isEmpty() && !memberDto.getDi().isEmpty()) {

            // DI 정보 변환 : 공백 -> +
            String convertDi = memberDto.getDi().replaceAll(" ", "+");
            memberDto.setDi(convertDi);

            // 가입여부 확인 (CI, DI 정보로 DB 조회)
            List<MemberDto> findMember = memberService.getMemberInfoByCiDi(memberDto);

            // 가입된 회원을 찾았다면
            if (!findMember.isEmpty()) {

                // 가입된 회원 계정 목록 순회
                for (MemberDto dto : findMember) {

                    // 입력받은 아이디와 DB에 저장된 아이디가 동일하다면
                    if (memberDto.getId().equals(dto.getId())) {

                        // 일반 가입 회원인 경우
                        if (dto.getIsSimple() == 0) {

                            // 비밀번호 재설정
                            memberService.modifyPasswordNonLogin(memberDto);

                            // 결과 메세지 처리
                            message = super.langMessage("lang.member.success.reset.password"); // 비밀번호를 재설정하였습니다.

                            // 비밀번호 재설정 성공
                            return displayJson(true, "1000", message);

                            // 소셜 가입 회원인 경우
                        } else if (dto.getIsSimple() == 1) {

                            // 소셜 가입 구분값(naver/kakao) 반환
                            String simpleType = dto.getSimpleType();
                            JSONObject data = new JSONObject();
                            data.put("simpleType", simpleType);

                            // 결과 코드 및 메세지 처리
                            code = CustomError.SIMPLE_MEMBER_PASSWORD_ERROR.getCode(); // EMMF-3994
                            message = CustomError.SIMPLE_MEMBER_PASSWORD_ERROR.getMessage(); // 소셜 가입 회원은 비밀번호를 변경할 수 없습니다.

                            // 비밀번호 재설정 실패
                            return displayJson(false, code, message, data);
                        }
                    }
                }
            }
            // 비밀번호 재설정 실패
            return displayJson(false, code, message);
        }
        // 비밀번호 재설정 실패
        return displayJson(false, code, message);
    }
}