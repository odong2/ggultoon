package com.architecture.admin.services.login;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.config.interceptor.JwtInterceptor;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.libraries.utils.pg.EncryptUtil;
import com.architecture.admin.models.dao.coin.CoinDao;
import com.architecture.admin.models.dao.login.LoginDao;
import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.models.daosub.coin.CoinDaoSub;
import com.architecture.admin.models.daosub.login.LoginDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.coin.CoinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;
import static com.architecture.admin.libraries.utils.EventUtils.*;

/*****************************************************
 * 로그인 모델러
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class LoginService extends BaseService {
    private final LoginDao loginDao;
    private final LoginDaoSub loginDaoSub;
    private final MemberDao memberDao;
    private final CoinDao coinDao;
    private final CoinDaoSub coinDaoSub;
    private final MemberDaoSub memberDaoSub;
    private final JwtInterceptor jwtInterceptor;
    private final CoinService coinService;

    @Value("${auth.autologin.key}")
    private String autologinKey;

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 로그인 처리
     *
     * @param memberDto    : 회원이 입력한 아이디 & 비밀번호
     * @param httpRequest
     * @param httpResponse
     * @return
     * @throws Exception
     */
    public JSONObject login(MemberDto memberDto,
                         HttpServletRequest httpRequest,
                         HttpServletResponse httpResponse) throws Exception {

        // return value
        JSONObject data = new JSONObject();

        // 아이디 & 패스워드 조회
        String id = memberDto.getId();
        String password = memberDto.getPassword();

        if (memberDto.getAuto() != null && !memberDto.getAuto().isEmpty()) {

            // 자동로그인 처리
            byte[] decrypt = EncryptUtil.decodeBase64(memberDto.getAuto());
            byte[] resultByte = EncryptUtil.aes256DecryptEcb(autologinKey, decrypt);
            JSONObject auto = new JSONObject(new String(resultByte, StandardCharsets.UTF_8));

            // 계정 설정
            memberDto.setId(auto.getString("id"));
            memberDto.setPassword(auto.getString("password"));

        } else {

            // 아이디 체크
            if (id == null || id.equals("")) {
                throw new CustomException(CustomError.LOGIN_ID_ERROR);
            }

            // 비밀번호 체크
            if (password == null || password.equals("")) {
                throw new CustomException(CustomError.LOGIN_PW_ERROR);
            }

            // 비밀번호 암호화
            memberDto.setPassword(super.encrypt(password));
        }

        // 회원가입 후 바로 로그인 MAIN DB에서 조회
        MemberDto memberInfo = loginDao.getInfoForLogin(memberDto);

        if (memberInfo != null && memberInfo.getIdx() > 0) {
            if (memberInfo.getState() != 1) {
                throw new CustomException(CustomError.MEMBER_STATE_ERROR);
            }

            // adult 업데이트 - 생년월일 기준으로 성인 여부 확인
            if (memberInfo.getBirth() != null) {
                String brith = memberInfo.getBirth();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                int today = Integer.parseInt(formatter.format(new Date()));
                int date = Integer.parseInt(brith.replace("-", ""));
                //int sum = today - date;
                //pushAlarm(Integer.toString(sum),"LJH");
                if ((today - date) > 190000) {
                    memberInfo.setAdult(1);
                    memberDao.modifyAdult(memberInfo);
                }
            }

            // 세션 생성
            session.setAttribute(SessionConfig.LOGIN_ID, memberInfo.getId());

            // 관리자 정보 입력 Object -> json
            ObjectMapper objectMapper = new ObjectMapper();

            // 닉네임 null인 경우 빈값 설정
            if (memberInfo.getNick() == null) {
                memberInfo.setNick("");
            }

            // 세션에 회원 정보 세팅
            session.setAttribute(SessionConfig.MEMBER_INFO, objectMapper.writeValueAsString(memberInfo));

            // 회원 환경설정 정보 DB에서 조회
            MemberDto memberSetting = loginDao.getSettingInfo(memberInfo);

            // 세션에 회원 환경설정 정보 세팅
            session.setAttribute(SessionConfig.MEMBER_SETTING, objectMapper.writeValueAsString(memberSetting));

            // 세션 만료 시간 설정
            session.setMaxInactiveInterval(SessionConfig.EXPIRED_TIME); // 30분

            // 레디스 key 입력
            String sKey = "session_" + memberInfo.getId();
            session.setAttribute(sKey, memberInfo.toString());

            // 세션 정보 레디스에 적용
            super.setRedis(sKey, memberDto.toString(), SessionConfig.EXPIRED_TIME);

            // jwt 토큰 발행
            jwtInterceptor.setJwtToken(httpRequest, httpResponse);

            // 로그인 타입 추가
            if (memberDto.getLoginType() == null) {

                // 디바이스 정보 set
                String device = httpRequest.getHeader("User-Agent");
                if (isMobile(device)) {  // 모바일
                    memberInfo.setLoginType(MOBILE);
                } else { // pc
                    memberInfo.setLoginType(PC);
                }

            } else {
                memberInfo.setLoginType(memberDto.getLoginType());
            }

            /** 꿀툰 서비스 종료 -> 로그인 마일리지 지급 중지(주석 처리) **/
//            // 마일리지 지급 이벤트
//            if (memberInfo != null) {
//                HashMap<String, Object> map = loginMileageEvent(memberInfo);
//                data.put("loginMileage", map);
//            }

            // 로그인 시간 업데이트
            memberInfo.setLogindate(dateLibrary.getDatetime());
            loginDao.updateLoginDate(memberInfo);
            
            // 로그인 성공
            data.put("result", true);

        } else {

            // 로그인 실패
            data.put("result", false);
        }
        return data;
    }

    /**
     * 자동로그인 항목 설정
     *
     * @param memberDto
     * @return
     */
    public String auto(MemberDto memberDto) throws Exception {

        String loginKey = loginDaoSub.getLoginKey(memberDto);

        if (loginKey == null) {
            String password = memberDto.getId() + dateLibrary.getDatetime();
            JSONObject data = new JSONObject();
            data.put("id", memberDto.getId());
            data.put("password", password);

            //암호화 처리
            byte[] aesStrRaw = EncryptUtil.aes256EncryptEcb(autologinKey, data.toString());
            memberDto.setAuto(EncryptUtil.encodeBase64(aesStrRaw));
            memberDto.setRegdate(dateLibrary.getDatetime());
            loginDao.insertLoginKey(memberDto);
        } else {
            memberDto.setAuto(loginKey);
        }
        return memberDto.getAuto();

    }

    /**
     * 로그인 마일리지 지급 이벤트
     * 1. 최초 로그인 시 1000M 지급
     *      예외 : 같은 CI 정보로 이미 받은 경우(다계정, 탈퇴 후 재가입 등)
     *      예외 : OTT 가입 이벤트로 1000M를 이미 받은 경우
     * 2. 1일 1회 로그인 시 500M 지급
     *      예외 : 가입일과 로그인 일자가 같은 경우(1000M 이미 받은 경우)
     *
     * @param memberInfo 회원 정보
     * @return map : 지급 여부, 지급 마일리지
     */
    public HashMap<String, Object> loginMileageEvent(MemberDto memberInfo) {

        // 결과값 반환용 map 기본값 set
        HashMap<String, Object> map = new HashMap<>();
        map.put("new", false);      // 신규 회원 여부 -> 기본값 : 기존 회원
        map.put("result", false);   // 지급 여부 -> 기본값 : 미지급
        map.put("mileage", 0);      // 지급 마일리지 개수 -> 기본값 : 0개

        /** 1. 최초 로그인 시 1000M 지급 이벤트 **/
        // 이벤트가 현재 진행 중인지 체크
        if (Boolean.TRUE.equals(dateLibrary.checkEventState(START_FIRST_LOGIN, END_FIRST_LOGIN))) {

            // 회원가입 후 최초 로그인한 회원인지 체크
            if (memberInfo.getLogindate() == null) {

                // 회원 ci 정보 체크
                if (memberInfo.getCi() != null) {

                    // 회원 ci 개수 조회
                    int memberCiCnt = memberDaoSub.getMemberCiCnt(memberInfo.getCi());
                    
                    // 현재 계정이 해당 회원이 최초로 가입한 계정인지 체크
                    if (memberCiCnt == 1) {

                        // 신규 회원
                        map.put("new", true);

                        // 현재 계정이 OTT 가입 마일리지를 받았는지 체크
                        int memberOttCnt = memberDaoSub.getMemberOttCnt(memberInfo.getIdx());
                        
                        // OTT 가입 마일리지를 받지 않은 경우
                        if (memberOttCnt == 0) {

                            // 최초 로그인 마일리지 지급용 dto set
                            CoinDto loginMileage = CoinDto.builder()
                                    .memberIdx(memberInfo.getIdx())
                                    .paymentIdx(0)
                                    .coin(0)
                                    .coinFree(0)
                                    .mileage(1000) // 지급 마일리지
                                    .position("회원가입")
                                    .title("꿀툰 가입 이벤트") // 이용내역 노출 문구
                                    .state(1)
                                    .build();

                            // 1000M 지급
                            coinService.coinPayment(loginMileage);

                            // 결과값 반환
                            map.put("result", true);  // 지급 완료
                            map.put("mileage", 1000); // 1000M
                        }
                    }
                }
            }
        }

        /** 2. 1일 1회 로그인 시 500M 지급 이벤트 **/
        // 이벤트가 현재 진행 중인지 체크
        if (Boolean.TRUE.equals(dateLibrary.checkEventState(START_DAILY_LOGIN, END_DAILY_LOGIN))) {

            // 회원가입 후 최초 로그인한 경우 -> 지급X
            if (memberInfo.getLogindate() == null) {

                // 가입일 기준으로 지급일 계산
                if (Boolean.TRUE.equals(map.get("result")) && Boolean.TRUE.equals(map.get("new"))) { // 신규 회원 팝업 노출 시
                    String regdate = getLoginMileageRegDate(memberInfo.getRegdate());
                    map.put("regdate", regdate);
                }

                // 신규 회원이 가입일에 재로그인한 경우 -> 지급X
            } else if (Boolean.TRUE.equals(dateLibrary.checkIsSameDay(memberInfo.getRegdate()))) {

                // 가입일 기준으로 지급일 계산
                if (Boolean.TRUE.equals(map.get("result")) && Boolean.TRUE.equals(map.get("new"))) { // 신규 회원 팝업 노출 시
                    String regdate = getLoginMileageRegDate(memberInfo.getRegdate());
                    map.put("regdate", regdate);
                }

                // 기존 회원이 로그인한 경우 -> 지급O
            } else {

                // 로그인한 시점 기준 조회할 시작일 ~ 종료일 계산
                HashMap<String, String> data = getEventStartAndEndDate();

                // 중복 지급 여부 체크용 dto set
                MemberDto dto = MemberDto.builder()
                        .memberIdx(memberInfo.getIdx()) // 회원 idx
                        .ci(memberInfo.getCi()) // 회원 ci 정보
                        .startDate(data.get("start")) // 로그인한 날짜 기준 당일 오전 10시(시작일)
                        .endDate(data.get("end")) // 로그인한 날짜 기준 익일 오전 9시 59분(종료일)
                        .build();

                // 로그인한 시점 기준 당일 오전 10시 ~ 익일 오전 9시 59분 사이에 이미 지급받았는지 체크
                CoinDto checkMember = coinDaoSub.getMemberLoginMileageInfo(dto);

                // 지급 내역이 없는 경우에 한해 지급
                if (checkMember == null) {

                    // 지급 마일리지의 만료일 계산
                    String expireDate = getLoginMileageExpireDate();

                    // dto set
                    CoinDto loginMileage = CoinDto.builder()
                            .memberIdx(memberInfo.getIdx())
                            .paymentIdx(0)
                            .coin(0)
                            .coinFree(0)
                            .mileage(500) // 500 마일리지
                            .position("로그인")
                            .title("꿀툰 데일리 이벤트") // 이용 내역 노출 문구
                            .state(1)
                            .mileageExpireDate(expireDate) // 로그인한 시점 기준 익일 오전 9시 59분
                            .build();

                    // 로그인 마일리지 지급
                    coinService.coinPayment(loginMileage);

                    // event_login_log insert(로그 테이블)
                    int result = coinDao.insertMemberLoginMileage(loginMileage);

                    // 로그인 마일리지 지급 완료
                    if (result > 0) {
                        map.put("new", false);   // 기존 회원
                        map.put("result", true); // 지급 완료
                        map.put("mileage", loginMileage.getMileage()); // 500M
                    }
                }
            }
        }
        // 마일리지 지급 결과 반환
        return map;
    }
}
