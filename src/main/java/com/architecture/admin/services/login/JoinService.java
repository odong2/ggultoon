package com.architecture.admin.services.login;

import com.architecture.admin.libraries.ServerLibrary;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.libraries.utils.pg.EncryptUtil;
import com.architecture.admin.models.dao.coin.CoinDao;
import com.architecture.admin.models.dao.login.JoinDao;
import com.architecture.admin.models.dao.member.GradeDao;
import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.models.dao.setting.SettingDao;
import com.architecture.admin.models.daosub.login.JoinDaoSub;
import com.architecture.admin.models.daosub.setting.SettingDaoSub;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberGradeDto;
import com.architecture.admin.models.dto.setting.SettingDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.event.EventService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import java.nio.charset.StandardCharsets;

import static com.architecture.admin.libraries.utils.CommonUtils.*;

/*****************************************************
 * 회원 가입 모델러
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class JoinService extends BaseService {

    private final MemberDao memberDao;
    private final JoinDao joinDao;
    private final GradeDao gradeDao;
    private final CoinDao coinDao;
    private final SettingDao settingDao;
    private final SettingDaoSub settingDaoSub;
    private final JoinDaoSub joinDaoSub;

    private final CoinService coinService;
    private final EventService eventService;

    // ott 암호화키
    @Value("${auth.ott.api.key}")
    private String ottApiKey;

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 일반 회원 가입
     *
     * @param memberDto (id,pw,isSimple,pwConfirm)
     * @param httpResponse
     * @throws Exception
     */
    public Long regist(MemberDto memberDto, HttpServletResponse httpResponse) throws Exception {

        // 아이디/패스워드 검증
        String id = memberDto.getId();
        String password = memberDto.getPassword();
        String txseq = memberDto.getTxseq(); // 본인인증키
        String privacy = memberDto.getPrivacy(); // 개인정보 수집 동의
        String age = memberDto.getAge(); // 14세 이상 동의
        String marketing = memberDto.getMarketing(); // 마케팅 광고 동의


        // 간편가입 계정
        Integer isSimple = memberDto.getIsSimple();
        String simpleType = memberDto.getSimpleType();
        if (memberDto.getIsSimple() == null) {
            memberDto.setIsSimple(0);
            isSimple = 0;
        }

        // 본인인증 번호 확인
        if (empty(txseq)) {
            // 본인인증이 필요합니다.
            throw new CustomException(CustomError.NAME_CHECK_ERROR);
        }

        // 개인정보 수집 동의 확인
        if (empty(privacy)) {
            // 약관 필수값에 동의해주세요.
            throw new CustomException(CustomError.JOIN_POLICY_MUST);
        }

        // 14세 이상 확인
        if (empty(age)) {
            // 약관 필수값에 동의해주세요.
            throw new CustomException(CustomError.JOIN_POLICY_MUST);
        }

        // 소셜가입
        if (isSimple == 1 && empty(simpleType)) {
            // 간편 가입으로 진입 해 주세요.
            throw new CustomException(CustomError.SIMPLE_JOIN_ERROR);
        }

        if (empty(id)) {
            // 아이디를 입력해주세요.
            throw new CustomException(CustomError.JOIN_ID_ERROR);
        }

        // 패스워드 확인
        if (empty(password)) {
            // 비밀번호를 입력해주세요.
            throw new CustomException(CustomError.JOIN_PW_ERROR);
        }

        // 아이디 중복체크
        Boolean bDupleId = checkDupleId(memberDto);
        if (Boolean.TRUE.equals(bDupleId)) {
            // 이미 존재하는 아이디입니다.
            throw new CustomException(CustomError.ID_DUPLE);
        }

        // 본인인증 정보 확인
        String sInfo = checkInfo(memberDto);
        if (sInfo == null) {
            // 본인인증이 필요합니다.
            throw new CustomException(CustomError.NAME_CHECK_ERROR);
        }else{
            memberDto.setCi(sInfo);
        }

        // ott 회원 정보 입력
        if (memberDto.getEdata() != null) {
            // 복호화
            JSONObject ottData = decode(memberDto.getEdata());
            super.pushAlarm(String.valueOf("regist : " + ottData),"YJM");
            memberDto.setSite(ottData.getString("site"));
        }

        /*
        // 간편 로그인이 아닐때 
        if (isSimple != 1) {
            // 일반 회원가입 아이디는 이메일로
            if (!isEmail(id)) {
                throw new CustomException(CustomError.JOIN_ID_EMAIL_ERROR);
            }
            // 패스워드 확인
            String passwordConfirm = memberDto.getPasswordConfirm();
            if (!password.equals(passwordConfirm)) {
                throw new CustomException(CustomError.PASSWORD_CONFIRM);
            }
        }
       */

        // 패스워드 암호화
        memberDto.setPassword(super.encrypt(password));

        // 회원가입처리
        Long lInsertIdx = insert(memberDto);

        // 회원 idx set
        memberDto.setInsertedIdx(lInsertIdx);
        memberDto.setIdx(lInsertIdx);

        // 회원 정보 업데이트
        updateInfo(lInsertIdx, memberDto);

        // 등급 정보 등록
        insertMemberGrade(lInsertIdx);

        // 코인 정보 등록
        insertMemberCoin(lInsertIdx);

        // 환경설정 정보 등록
        insertMemberSetting(lInsertIdx, marketing);

        // 회원 약관 정보 등록
        if(notEmpty(memberDto.getAge()) || notEmpty(memberDto.getMarketing()) || notEmpty(memberDto.getPrivacy())){
            memberDao.insertMemberPolicy(memberDto);
        }

        // 간편 가입일 경우
        if (isSimple == 1) {
            insertSimple(lInsertIdx, memberDto);
        }

        // 최초 가입 이벤트 포인트 지급
        ottEvent(memberDto, httpResponse);
        return lInsertIdx;
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/

    /**
     * 회원 아이디 중복 검색
     *
     * @param memberDto
     * @return
     */
    public Boolean checkDupleId(MemberDto memberDto) {
        Integer iCount = joinDaoSub.getCountById(memberDto);

        return iCount > 0;
    }

    /**
     * 본인인증 정보 확인
     *
     * @param memberDto
     * @return
     */
    public String checkInfo(MemberDto memberDto) {
        return  joinDaoSub.getCountByTxseq(memberDto);
    }

    /**
     * OTT 이벤트 참여 CI 확인
     * @param memberDto 회원정보
     * @return 최초참여 여부
     */
    public MemberOttDto eventCiCheck(MemberDto memberDto) {
        return joinDaoSub.getEventCiCheck(memberDto);
    }

    /**
     * OTT 이벤트 참여 확인
     * @param memberDto 회원정보
     * @return 최초참여 여부
     */
    public Boolean eventCheck(MemberDto memberDto) throws Exception {
        boolean check = false;
        // 복호화
        JSONObject ottData = decode(memberDto.getEdata());
        super.pushAlarm(String.valueOf("eventCheck : " + ottData),"YJM");

        // 회원정보 설정
        MemberOttDto memberOttDto = MemberOttDto.builder()
                .memberIdx(memberDto.getInsertedIdx())
                .site(ottData.getString("site"))
                .ottId(ottData.getString("userid"))
                .build();

        Long iCount = joinDaoSub.getEventCheck(memberOttDto);

        if(iCount == null){
            check = true;
        }
        return check;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 회원 등록
     *
     * @param memberDto
     * @return
     */
    @Transactional
    public Long insert(MemberDto memberDto) {

        HttpServletRequest request = ServerLibrary.getCurrReq();
        memberDto.setRegdate(dateLibrary.getDatetime());
        memberDto.setModifyDate(dateLibrary.getDatetime());
        memberDto.setJoinIp(super.getClientIP(request));

        joinDao.insert(memberDto);

        return memberDto.getInsertedIdx();
    }

    /**
     * 회원 정보 등록
     *
     * @param insertIdx member.idx
     * @param memberDto
     */
    public void insertInfo(Long insertIdx, MemberDto memberDto) {
        memberDto.setMemberIdx(insertIdx);
        joinDao.insertInfo(memberDto);
    }

    /**
     * 소셜 로그인 등록
     *
     * @param insertIdx member.idx
     * @param memberDto
     */
    public void insertSimple(Long insertIdx, MemberDto memberDto) {
        memberDto.setMemberIdx(insertIdx);
        joinDao.insertSimple(memberDto);
    }

    /**
     * 등급 정보 등록
     *
     * @param insertIdx member.idx
     */
    public void insertMemberGrade(Long insertIdx) {
        MemberGradeDto memberGradeDto = new MemberGradeDto();
        memberGradeDto.setIdx(insertIdx);
        memberGradeDto.setGrade(0);
        memberGradeDto.setAmount(0);
        memberGradeDto.setPayback(0);
        memberGradeDto.setAddMileage(0);
        gradeDao.insertMemberGrade(memberGradeDto);
    }

    /**
     * 코인 정보 등록
     *
     * @param insertIdx member.idx
     */
    public void insertMemberCoin(Long insertIdx) {
        CoinDto coinDto = new CoinDto();
        coinDto.setMemberIdx(insertIdx);
        coinDto.setRegdate(dateLibrary.getDatetime());
        coinDao.insertMemberCoin(coinDto);
    }

    /**
     * 환경설정 정보 등록
     *
     * @param insertIdx member.idx
     */
    public void insertMemberSetting(Long insertIdx, String marketing) {
        List<SettingDto> settingList = settingDaoSub.getSettingList();
        for (SettingDto dto : settingList) {

            // 회원 idx set
            dto.setMemberIdx(insertIdx);

            // 환경설정 기본 상태값 set
            if (dto.getSettingIdx() == 1) { // 코인 차감 안내 옵션일 경우
                dto.setState(0); // 기본값 OFF
            } else {
                dto.setState(1); // 기본값 ON
            }

            // 등록일 set
            dto.setRegdate(dateLibrary.getDatetime());
            
            // 마케팅 수신 거부 시 광고 및 혜택 알림 & 야간 알림 수신 설정 0FF
            if (marketing == null || marketing.isEmpty() || marketing.equals("N")) {
                if (dto.getSettingIdx() == 3 || dto.getSettingIdx() == 4) {
                    dto.setState(0);
                }
            }
        }
        // 환경설정 정보 등록
        settingDao.insertMemberSetting(settingList);
    }

    /**
     * OTT 회원 정보 입력
     * @param memberDto 회원가입 데이터
     * @param ott ott api 전달 데이터
     * @throws Exception
     */
    public String insertMemberOtt(MemberDto memberDto, JSONObject ott) throws Exception {

        // 복호화
        JSONObject ottData = decode(memberDto.getEdata());
        super.pushAlarm(String.valueOf("insertMemberOtt : " + ottData),"YJM");

        // dto set
        MemberOttDto memberOttDto = MemberOttDto.builder()
                .memberIdx(memberDto.getInsertedIdx())
                .site(ottData.getString("site"))
                .bannerCode(ottData.getString("bannerCode"))
                .eventType(ottData.getString("eventType"))
                .ottId(ottData.getString("userid"))
                .point(Integer.parseInt(ott.getString("point")))
                .sendMsg(ott.getString("rtMsg"))
                .returnUrl(ottData.getString("returnUrl"))
                .returnMsg("")
                .regdate(dateLibrary.getDatetime())
                .build();
        joinDao.insertMemberOtt(memberOttDto);

        return decode(memberDto.getEdata()).toString();

    }

    /**
     * OTT 이벤트 통계 입력
     * @param memberDto 회원가입 데이터
     * @param point 지급 마일리지
     * @param httpResponse
     * @throws Exception
     */
    public void insertEventOtt(MemberDto memberDto, String type, Integer point, HttpServletResponse httpResponse) throws Exception {
        
        int join = 0; // 가입
        int visit = 0; // 접속
        if (Objects.equals(type, "join")){
            join = 1;
        } else{
            visit = 1;
        }
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String today = formatter.format(new Date());

        // OTT로부터 전달 받은 데이터 복호화
        JSONObject ottData = decode(memberDto.getEdata());
        super.pushAlarm(String.valueOf("insertEventOtt : " + ottData),"YJM");

        // 복호화한 데이터로 쿠키 굽기(UTF-8 인코딩)
        Cookie ottVisitToken = new Cookie("ottVisitToken", URLEncoder.encode(ottData.toString(), StandardCharsets.UTF_8));
        ottVisitToken.setMaxAge(60 * 60 * 24); // 24시간 후 만료
        ottVisitToken.setPath("/");
        ottVisitToken.setSecure(true);
        ottVisitToken.setHttpOnly(true);
        httpResponse.addCookie(ottVisitToken);

        // dto set
        MemberOttDto memberOttDto = MemberOttDto.builder()
                .site(ottData.getString("site"))
                .bannerCode(ottData.getString("bannerCode"))
                .point(point)
                .join(join)
                .visit(visit)
                .today(today)
                .couponCnt(0)
                .build();
        
        // 이벤트 통계 입력
        joinDao.insertEventOtt(memberOttDto);
    }

    /**
     * OTT 가입 이벤트 처리
     * @param memberDto 회원 DTO
     * @param httpResponse
     * @throws Exception
     */
    public void ottEvent(MemberDto memberDto, HttpServletResponse httpResponse) throws Exception {

        // 회원정보가 없거나 암호화 데이터가 없는경우 중단
        if (memberDto == null  || memberDto.getEdata() == null) {
            //pushAlarm("test","LJH");
            return;
        }

        // 복호화
        JSONObject ottData = decode(memberDto.getEdata());
        super.pushAlarm(String.valueOf("ottEvent : " + ottData),"YJM");

        // API 전송값 설정
        JSONObject ott = new JSONObject();
        ott.put("userid", ottData.getString("userid"));
        ott.put("gtUserid", memberDto.getId());
        ott.put("eventType", ottData.getString("eventType"));
        ott.put("bannerCode", ottData.getString("bannerCode"));
        ott.put("isGive", "N");
        ott.put("rtMsg", "ok");
        ott.put("point", "0");

            // 이벤트 타입 설정 - 회원가입
            memberDto.setEventType("join");
            // 이벤트 중복 참여 확인
            MemberOttDto eventCiCheck = eventCiCheck(memberDto);

            // 회원가입 성공 시 처리
            if (eventCiCheck == null) {
                    // 이벤트 log 입력
                    eventService.ottEvent(memberDto.getInsertedIdx(), 1000, "join");
                    // 이벤트 포인트 지급
                    coinService.addMileage(memberDto.getInsertedIdx(), 1000);

                    // 추가 전송
                    // OTT 회원 조회
                    List<MemberOttDto> list = joinDaoSub.getEventCiCheckList(memberDto);

                    for (MemberOttDto dto : list) {
                        // API 전송값 설정
                        JSONObject data = new JSONObject();
                        data.put("userid", dto.getOttId());
                        data.put("gtUserid", dto.getId());
                        data.put("eventType", dto.getEventType());
                        data.put("bannerCode", dto.getBannerCode());
                        data.put("isGive", "N");
                        // 다른 사이트 이벤트 참여회원
                        data.put("rtMsg", "ci||" + ottData.getString("site") + "||" + ottData.getString("userid"));
                        data.put("point", "0");

                        // OTT 이벤트 결과 전송
                        String dataRes = ottEventSend(dto.getReturnUrl(),data.toString());
                        // 회원정보 업데이트
                        updateMemberOtt(dto.getMemberIdx(),data.getString("rtMsg"), dataRes);

                        pushAlarm(data.toString(),"LJH");
                    }
                    ott.put("point", "1000");

                ott.put("isGive", "Y");

            } else {
               /* if (!eventCheck) {
                    // 동일 사이트 이벤트 참여회원
                    ott.put("rtMsg", "id||" + ottData.getString("site") + "||" + ottData.getString("userid"));
                }*/
                if (eventCiCheck.getOttId() == null) {
                    // 기존 꿀툰회원
                    ott.put("rtMsg", "old||" + eventCiCheck.getId());
                } else {
                    // 다른 사이트 이벤트 참여회원
                    ott.put("rtMsg", "ci||" + eventCiCheck.getSite() + "||" + eventCiCheck.getOttId());
                }
            }
        // ott 회원 정보 입력
        insertMemberOtt(memberDto, ott);
        insertEventOtt(memberDto, "join", ott.getInt("point"), httpResponse);

        // OTT 이벤트 결과 전송
        String res = ottEventSend(ottData.getString("returnUrl"), ott.toString());

        // 회원정보 업데이트
        updateMemberOtt(memberDto.getInsertedIdx(), res);

        //pushAlarm(res,"LJH");
        pushAlarm(ott.toString(), "LJH");
        //encode(new JSONObject(map).toString());
    }

    /**
     * OTT 이벤트 결과 전송
     * @param str OTT 전송 정보
     * @return OTT 이벤트 전송 응답 결과
     */
    public String ottEventSend(String returnUrl, String str) throws Exception {
        // 전달 데이터 암호화
        MultiValueMap<String, String> edata = new LinkedMultiValueMap<>();
        edata.add("e", encode(str.toString()));
        return super.postCurl(returnUrl, edata);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /**
     * 회원 정보 등록
     *
     * @param insertIdx member.idx
     * @param memberDto
     */
    public void updateInfo(Long insertIdx, MemberDto memberDto) {
        memberDto.setMemberIdx(insertIdx);
        memberDao.updateMemberInfo(memberDto);
    }

    /**
     * OTT 회원 정보 입력
     * @param memberIdx 회원idx
     * @param returnMsg API리턴메세지
     * @throws Exception
     */
    public void updateMemberOtt(Long memberIdx, String returnMsg) {
        MemberOttDto memberOttDto = MemberOttDto.builder()
                .memberIdx(memberIdx)
                .returnMsg(returnMsg)
                .build();
        joinDao.updateMemberOtt(memberOttDto);

    }

    /**
     * OTT 회원 정보 입력
     * @param memberIdx 회원idx
     * @param returnMsg API리턴메세지
     * @throws Exception
     */
    public void updateMemberOtt(Long memberIdx, String sendMsg, String returnMsg) {
        MemberOttDto memberOttDto = MemberOttDto.builder()
                .memberIdx(memberIdx)
                .sendMsg(sendMsg)
                .returnMsg(returnMsg)
                .build();
        joinDao.updateMemberOtt(memberOttDto);

    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/



    /*****************************************************
     *  SubFunction
     ****************************************************/
    /**
     * 복호화
     * @param s 복호화 할 데이터
     * @return JSONObject
     * @throws Exception
     */
    public JSONObject decode(String s) throws Exception {
        // 복호화 처리
        // String s = "8vHejeW10HqTdi%2Fz0tTrO4PVZimWX%2FQLaOMNx4wFcSmKsRui5SRu038Abv91fNCGh1B4jBIv08Z5JS62tJ3XplLt66b7o8Gjjiao61Yqv7LKUielQf05QKUh2CtxulYGmXslehpt%2BMYflhWep5%2Bk2o1C7Xi6TGZr7GoHdmVgz0AihhfM7Zl1ORzhj9o087PJ";
        //URLEncoder.encode("오류가 발생했습니다.", StandardCharsets.UTF_8);
        // URLDecoder 주석처리
        // s = URLDecoder.decode(s,StandardCharsets.UTF_8);
        //pushAlarm(s,"LJH");
        byte[] decrypt = EncryptUtil.decodeBase64(s);
        //byte[] decrypt = s.getBytes("UTF-8");
        byte[] resultByte = EncryptUtil.aes256DecryptEcb(ottApiKey, decrypt);
        return new JSONObject(new String(resultByte, StandardCharsets.UTF_8));
    }

    /**
     * 암호화
     * @param s 암호화 할 데이터
     * @return 암호문
     * @throws Exception
     */
    public String encode(String s) throws Exception {

        // 암호화 처리
        byte[] aesStrRaw = EncryptUtil.aes256EncryptEcb(ottApiKey,s);
        return EncryptUtil.encodeBase64(aesStrRaw);

    }
}
