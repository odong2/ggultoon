package com.architecture.admin.services.member;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.libraries.utils.BadgeUtils;
import com.architecture.admin.libraries.utils.CoinUtils;
import com.architecture.admin.models.dao.coin.CoinDao;
import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.models.daosub.coin.CoinDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.daosub.notification.NotificationDaoSub;
import com.architecture.admin.models.daosub.purchase.PurchaseDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.member.MemberAppDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberSimpleDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.purchase.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.regex.Pattern;

import static com.architecture.admin.config.SessionConfig.LOGIN_NICK;
import static com.architecture.admin.config.SessionConfig.MEMBER_INFO;
import static com.architecture.admin.libraries.utils.CoinUtils.BENEFIT_COIN;
import static com.architecture.admin.libraries.utils.CoinUtils.COIN_FREE;
import static com.architecture.admin.libraries.utils.CommonUtils.*;
import static com.architecture.admin.libraries.utils.ContentUtils.*;

@RequiredArgsConstructor
@Service
@Transactional
public class MemberService extends BaseService {

    private final MemberDao memberDao;
    private final MemberDaoSub memberDaoSub;
    private final CoinDaoSub coinDaoSub;
    private final PurchaseDaoSub purchaseDaoSub;
    private final PurchaseService purchaseService;
    private final CoinDao coinDao;
    private final CoinService coinService;
    private final NotificationDaoSub notificationDaoSub;

    // 비밀번호 형식 : 영문/숫자/특수문자 중 2가지 이상 포함 6 ~ 20자
    String pwdRegex = "^(?=.*[A-Za-z])(?=.*[0-9!@#$%^&*()_+\\-\\=\\[\\]{};':\"\\\\|,.<>\\/?])[A-Za-z0-9!@#$%^&*()_+\\-\\=\\[\\]{};':\"\\\\|,.<>\\/?]{6,20}$";

    /********************************************************************************
     * SELECT
     ********************************************************************************/

    /**
     * 회원 중복 닉네임 검사
     *
     * @param nick : 변경할 닉네임
     * @return
     */
    @Transactional(readOnly = true)
    public Boolean checkDupleNick(String nick) {
        // 닉네임 공백 제거 및 소문자 변환 후 DB 조회
        nick = nick.trim().toLowerCase();

        int iCount = memberDaoSub.getCountByNick(nick);

        return iCount > 0;
    }

    /**
     * CI, DI 정보로 회원 정보 가져오기
     *
     * @param memberDto
     */
    public List<MemberDto> getMemberInfoByCiDi(MemberDto memberDto) {
        return memberDaoSub.getMemberInfoByCiDi(memberDto);
    }

    /**
     * 회원 아이디 유무 조회
     *
     * @param inputId
     * @throws Exception
     */
    public int getCountById(String inputId) {
        return memberDaoSub.getCountById(inputId);
    }

    /**
     * 마이페이지 (메인)
     *
     * @param searchDto : idx(memberIdx)
     * @return
     */
    public JSONObject getMyPageInfo(@ModelAttribute SearchDto searchDto) {

        // memberIdx set
        Long memberIdx = searchDto.getIdx();

        /** 회원 기본 정보 조회 **/
        MemberDto memberDto = memberDaoSub.getMemberInfoByIdx(memberIdx);

        // 회원이 읽지 않은 알림 개수 세팅
        Integer unreadNotiCnt = notificationDaoSub.getUnreadNotiCnt(memberIdx);
        memberDto.setUnreadNotiCnt(unreadNotiCnt);

        // 문자 변환
        stateText(memberDto);

        // dto set
        CoinDto coinDto = CoinDto.builder()
                .memberIdx(memberIdx)
                .nowDate(dateLibrary.getDatetime()).build();

        // 만료된 코인 & 마일리지 update 및 로그 등록
        coinService.updateExpireCoinAndMileage(memberIdx);

        /** 회원 코인 정보 조회 (만료된 코인은 제외) **/
        Integer coin = coinDaoSub.getMemberCoin(coinDto);
        Integer mileage = coinDaoSub.getMemberMileage(coinDto);
        coinDto.setCoin(coin);       // 앞단에 보여줄 회원 코인 set
        coinDto.setMileage(mileage); // 앞단에 보여줄 회원 마일리지 set

        /** 내가 보던 꿀작 리스트 **/
        List<PurchaseDto> lastViewList = purchaseDaoSub.getMemberLastViewList(searchDto);

        if (lastViewList != null && !lastViewList.isEmpty()) {
            // 회차 번호 텍스트 set
            setLibraryText(lastViewList);
            // 이미지 리스트 도메인 set
            purchaseService.setImgFullUrl(lastViewList);
            // 배지 코드 set
            purchaseService.setBadgeCode(lastViewList, BadgeUtils.LIBRARY);
        }

        // 컨트롤러로 리턴할 데이터 담기
        JSONObject jsonData = new JSONObject();

        jsonData.put("lastViewList", lastViewList);
        jsonData.put("coin", new JSONObject(coinDto));
        jsonData.put("member", new JSONObject(memberDto));

        return jsonData;
    }

    /**
     * 회원 기본정보 + 보유코인 정보 조회
     *
     * @param memberIdx
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getMemberTotalInfo(Long memberIdx) {
        // 회원 코인 정보
        CoinDto coinDto = CoinDto.builder()
                .memberIdx(memberIdx)
                .nowDate(dateLibrary.getDatetime()).build();

        /** 회원 코인 정보 조회 (만료된 코인은 제외) **/
        Integer coin = coinDaoSub.getMemberCoin(coinDto);
        Integer mileage = coinDaoSub.getMemberMileage(coinDto);
        coinDto.setCoin(coin);       // 앞단에 보여줄 회원 코인 set
        coinDto.setMileage(mileage); // 앞단에 보여줄 회원 마일리지 set

        // 회원 기본 정보
        MemberDto memberDto = memberDaoSub.getMemberInfoByIdx(memberIdx);
        //문자 변환
        stateText(memberDto);

        // 컨트롤러로 리턴할 데이터 담기
        JSONObject jsonData = new JSONObject();

        jsonData.put("coin", new JSONObject(coinDto));
        jsonData.put("member", new JSONObject(memberDto));

        return jsonData;
    }

    /**
     * 회원 기본정보 조회
     *
     * @param memberIdx
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getMemberInfoByIdx(Long memberIdx) {
        // 회원 기본 정보
        MemberDto memberDto = memberDaoSub.getMemberInfoByIdx(memberIdx);
        //문자 변환
        stateText(memberDto);

        // 컨트롤러로 리턴할 데이터 담기
        //JSONObject jsonData = new JSONObject(memberDto);
        //jsonData.put("member", new JSONObject(memberDto));

        return  new JSONObject(memberDto);
    }

    /**
     * 닉네임 사용 가능 체크
     *
     * @param memberDto : nick(변경할 닉네임)
     * @return true : 변경하려는 닉네임과 기존 닉네임 같음
     */
    @Transactional(readOnly = true)
    public Boolean checkNick(MemberDto memberDto) {
        String loginMemberNick = super.getMemberInfo(LOGIN_NICK);

        // 변경하려는 닉네임과 기존 닉네임이 같으면 false
        Boolean isNotEqualsNick;
        // 기존 닉네임이 null 인 회원
        if (loginMemberNick == null || loginMemberNick.isEmpty()) {
            isNotEqualsNick = nullNickUserValidate(memberDto);
            // 기존 닉네임이 존재하는 회원
        } else {
            isNotEqualsNick = notNullNickUserValidate(memberDto);
        }

        return isNotEqualsNick;
    }

    /********************************************************************************
     * INSERT
     ********************************************************************************/

    /**
     * 회원정보 테이블 등록(member_info insert)
     *
     * @param memberDto
     */
    @Transactional
    public void insertMemberInfo(MemberDto memberDto) {
        // 회원정보 테이블 등록(member_info insert)
        memberDao.insertMemberInfo(memberDto);
    }

    /**
     * 회원탈퇴 취소 혜택 받았는지 체크
     *
     * @param memberIdx
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject checkDeleteBenefit(Long memberIdx) {

        /** 1. 회원탈퇴 관련 혜택 받은적 있는지 체크 **/
        int benefitCnt = memberDaoSub.getDeleteBenefitCntByIdx(memberIdx);

        JSONObject jsonData = new JSONObject();

        // 받은적 없음
        jsonData.put("isBenefit", true);

        // 받은적 있음
        if (benefitCnt > 0) {
            jsonData.put("isBenefit", false);
        }
        return jsonData;
    }

    /**
     * 회원 탈퇴 취소 시 혜택 지급
     *
     * @param memberIdx
     * @return
     */
    @Transactional
    public void giveDeleteCancelBenefit(Long memberIdx) {

        /** 1. 회원탈퇴 관련 혜택 받은적 있는지 체크 **/
        int benefitCnt = memberDaoSub.getDeleteBenefitCntByIdx(memberIdx);

        if (benefitCnt > 0) {
            throw new CustomException(CustomError.ALREADY_DELETE_BENEFIT); // 이미 특별 혜택을 받으셨습니다.
        }

        /**  2. 회원 탈퇴 취소 혜택 부여 (보너스 코인 10) **/
        // 회원 코인 정보 조회
        CoinDto memberCoinDto = coinDaoSub.getMemberCoinInfoByMemberIdx(memberIdx);
        int existBonusCoin = memberCoinDto.getCoinFree(); // 현재 보유한 보너스 코인
        // 기존 보너스 코인 + 혜택 코인(10) set
        memberCoinDto.setCoinFree(existBonusCoin + BENEFIT_COIN);
        memberCoinDto.setMemberIdx(memberIdx);

        //  회원 코인정보 업데이트
        int updateResult = coinDao.updateMemberCoin(memberCoinDto);

        if (updateResult < 1) {
            throw new CustomException(CustomError.DELETE_BENEFIT_FAIL); // 특별 혜택 받기에 실패하였습니다.
        }

        /** 4. coin_used insert **/
        // coin_used 등록할 dto setting
        CoinDto benefitCoinDto = CoinDto.builder()
                .memberIdx(memberIdx)
                .memberCoinSaveIdx(0L)
                .coin(BENEFIT_COIN)
                .restCoin(BENEFIT_COIN)
                .type(COIN_FREE)
                .state(1)
                .expiredate(CoinUtils.getCoinExpireDate(COIN_FREE))
                .regdate(dateLibrary.getDatetime())
                .build();

        // coin_used insert
        coinDao.insertCoinUsed(benefitCoinDto);

        /** 5. 특별헤택 받음으로 업데이트 **/
        int result = memberDao.updateDeleteBenefit(memberIdx);

        if (result < 1) {
            throw new CustomException(CustomError.DELETE_BENEFIT_FAIL); // 특별 혜택 받기에 실패하였습니다.
        }
    }

    /**
     * 회원 광고ID 저장
     *
     * @param
     * @return
     */
    @Transactional
    public void insertAdid(MemberAppDto memberAppDto) {
        // Adid가 없다면
        if (empty(memberAppDto.getAdid())) {
            throw new CustomException(CustomError.BAD_REQUEST_REQUIRED_VALUE); // 필수값을 입력해주세요.
        }
        memberAppDto.setRegdate(dateLibrary.getDatetime());
        memberDao.insertAdid(memberAppDto);
    }

    /**
     * 회원 알림 토큰 저장
     *
     * @param
     * @return
     */
    @Transactional
    public void insertAppToken(MemberAppDto memberAppDto) {
        // Token이 없다면
        if (empty(memberAppDto.getToken())) {
            throw new CustomException(CustomError.BAD_REQUEST_REQUIRED_VALUE); // 필수값을 입력해주세요.
        }
        memberAppDto.setRegdate(dateLibrary.getDatetime());
        memberDao.insertAppToken(memberAppDto);
    }

    /********************************************************************************
     * UPDATE
     ********************************************************************************/

    /**
     * 닉네임 변경
     *
     * @param memberDto
     */
    @Transactional
    public void modifyNick(MemberDto memberDto) {

        String loginMemberNick = super.getMemberInfo(LOGIN_NICK);

        // 변경하려는 닉네임과 기존 닉네임이 같으면 false
        Boolean isNotEqualsNick;

        // 기존 닉네임이 존재하지 않는 회원
        if (loginMemberNick == null || loginMemberNick.isEmpty()) {
            isNotEqualsNick = nullNickUserValidate(memberDto);
            // 기존 닉네임이 존재하는 회원
        } else {
            isNotEqualsNick = notNullNickUserValidate(memberDto);
        }

        // 기존 닉네임과 변경하려는 닉네임 다름
        if (Boolean.TRUE == isNotEqualsNick) {

            // 닉네임 변경
            memberDao.modifyNick(memberDto);

            // 세션 닉네임 세팅
            JSONObject json = new JSONObject(session.getAttribute(MEMBER_INFO).toString());
            json.put(LOGIN_NICK, memberDto.getNick());
            session.setAttribute(SessionConfig.MEMBER_INFO, json.toString());
        }
    }

    /**
     * 회원 비밀번호 변경(로그인 상태)
     *
     * @param memberDto
     */
    @SneakyThrows
    @Transactional
    public void modifyPasswordLogin(MemberDto memberDto) {
        // 1. 유효성 검사
        modifyPasswordValidate(memberDto);

        String newPassword = memberDto.getNewPassword();
        // 2. 변경할 비밀번호 암호화
        memberDto.setNewPassword(super.encrypt(newPassword));

        // 3. 비밀번호 변경일 set
        memberDto.setModifyDate(dateLibrary.getDatetime());

        // 4. 비밀번호 변경
        memberDao.modifyPasswordLogin(memberDto);
    }

    /**
     * 비밀번호 찾기 성공 > 비밀번호 재설정
     *
     * @param memberDto 비밀번호 찾기를 통해 리턴받은 회원의 id, ci, di 정보 + 입력받은 newPassword(변경할 비밀번호), newPasswordConfirm(비밀번호 확인)
     */
    @SneakyThrows
    @Transactional
    public void modifyPasswordNonLogin(MemberDto memberDto) {

        // 아이디 유효성 검사
        idValidate(memberDto.getId());

        // 재설정할 비밀번호 유효성 검사
        modifyPasswordValidate(memberDto);

        // 재설정할 비밀번호 암호화
        String newPassword = memberDto.getNewPassword();
        memberDto.setNewPassword(super.encrypt(newPassword));

        // 비밀번호 변경일 set
        memberDto.setModifyDate(dateLibrary.getDatetime());

        // 비밀번호 재설정
        int result = memberDao.modifyPasswordNonLogin(memberDto);

        // 비밀번호 재설정 실패한 경우
        if (result < 1) {
            throw new CustomException(CustomError.FIND_MEMBER_PASSWORD_RESET_FAIL); // 비밀번호를 재설정할 수 없습니다.
        }
        // 비밀번호 변경
        memberDao.modifyPasswordNonLogin(memberDto);
    }

    /**
     * 내 정보 수정
     *
     * @param memberDto : nick, newPassWord, newPasswordConfirm
     */
    @SneakyThrows
    @Transactional
    public Boolean modifyNickAndPassword(MemberDto memberDto) {

        boolean isModify = false;         // 내 정보 변경 여부
        boolean isNickModify = false;     // 닉네임 변경 여부
        boolean isPasswordModify = false; // 비밀번호 변경 여부

        // 닉네임 변경
        if (memberDto.getNick() != null && !memberDto.getNick().isEmpty()) {
            isNickModify = true;
            modifyNick(memberDto);
        }
        // 비밀번호 변경
        if (memberDto.getNewPassword() != null && !memberDto.getNewPassword().isEmpty()) {
            isPasswordModify = true;
            modifyPasswordLogin(memberDto);
        }

        // 닉네임 비밀번호 하나라도 수정 되면 true return
        if (isNickModify || isPasswordModify) {
            isModify = true;
        }

        return isModify;
    }

    /********************************************************************************
     * DELETE
     ********************************************************************************/

    /**
     * 회원 탈퇴
     *
     * @param memberIdx
     */
    @Transactional
    public void deleteMemberByIdx(Long memberIdx) {

        // 로그인한 회원 정보

        JSONObject memberInfo = new JSONObject(session.getAttribute(MEMBER_INFO).toString());
        String id = memberInfo.getString(SessionConfig.LOGIN_ID);
        String nick = memberInfo.getString(LOGIN_NICK);
        if (nick.isEmpty()) {
            nick = null;
        }

        // 회원정보 set(member_out 테이블 insert 시 사용)
        MemberDto memberDto = MemberDto.builder()
                .idx(memberIdx)
                .id(id)
                .nick(nick)
                .outdate(dateLibrary.getDatetime())
                .build();

        // 1. 회원 정보 수정(state=0)
        int result = memberDao.deleteMember(memberIdx);

        if (result < 1) { // 이미 탈퇴했거나 없는 회원
            throw new CustomException(CustomError.MEMBER_DELETE_FAIL); // 회원 탈퇴에 실패하였습니다
        }

        // 2. 회원 테이블 수정(state=0)
        result = memberDao.deleteMemberInfo(memberIdx);
        if (result < 1) {
            throw new CustomException(CustomError.MEMBER_DELETE_FAIL); // 회원 탈퇴에 실패하였습니다
        }
        // 간편 가입 회원 유무 조회
        int isSimple = memberDaoSub.getMemberIsSimpleByIdx(memberIdx);

        // 간편가입 회원
        if (isSimple > 0) {
            // 1. 간편가입 정보 조회
            MemberSimpleDto memberSimpleDto = memberDaoSub.getMemberSimpleInfoByIdx(memberIdx); // 간편가입 정보 조회
            // 2. 간편가입 탈퇴 insert
            memberDao.insertMemberSimpleOut(memberSimpleDto);
        }

        // 3. 회원탈퇴 테이블 등록(member_out insert)
        memberDao.insertOutMember(memberDto);
    }

    /********************************************************************************
     * SUB
     ********************************************************************************/


    /**
     * List text 변환(내 서재 - 최근 본 작품 리스트)
     *
     * @param purchaseDtoList
     */
    private void setLibraryText(List<PurchaseDto> purchaseDtoList) {
        for (PurchaseDto purchaseDto : purchaseDtoList) {
            setLibraryText(purchaseDto);
        }
    }

    /**
     * text 변환(내 서재 - 최근 본 작품 리스트)
     *
     * @param purchaseDto
     */
    private void setLibraryText(PurchaseDto purchaseDto) {
        if (purchaseDto != null) {

            purchaseDto.setType(3);  // 최근(더미) - json 데이터 형식 맞추기 위한 용도
            purchaseDto.setTypeText(super.langMessage("lang.contents.lastView")); // 최근

            // 1. 회차 번호 텍스트
            if (purchaseDto.getEpisodeNumber() != null && purchaseDto.getCategory() != null) {

                if (purchaseDto.getCategory().equals(COMIC_TEXT)) {
                    purchaseDto.setEpisodeNumTitle(purchaseDto.getEpisodeNumber() + "권");
                } else {
                    purchaseDto.setEpisodeNumTitle(purchaseDto.getEpisodeNumber() + "화");
                }
            }
        }
    }

    /**
     * 회원 정보 문자변환
     *
     * @param memberDto
     */
    private void stateText(MemberDto memberDto) {
        // 성별
        if (memberDto.getGender() != null) {
            if (memberDto.getGender().equals("M")) {
                memberDto.setGenderText(super.langMessage("lang.member.gender.male")); // 남자
            } else if (memberDto.getGender().equals("F")) {
                memberDto.setGenderText(super.langMessage("lang.member.gender.female")); // 여자
            }
        }
        // 본인 인증
        if (memberDto.getAuth() != null) {
            if (memberDto.getAuth() == 0) {
                memberDto.setAuthText(super.langMessage("lang.member.non.auth")); // 비인증
            } else if (memberDto.getAuth() == 1) {
                memberDto.setAuthText(super.langMessage("lang.member.auth")); // 인증
            }
        }
        if (memberDto.getAdult() != null) {
            if (memberDto.getAdult() == 0) {
                memberDto.setAdultText(super.langMessage("lang.member.non.adult"));// 비성인
            } else if (memberDto.getAdult() == 1) {
                memberDto.setAdultText(super.langMessage("lang.member.adult"));// 성인
            }
        }
        // 간편가입 아닐 경우
        if (memberDto.getSimpleType() == null) {
            memberDto.setSimpleType(super.langMessage("lang.member.longin.type.normal")); // 일반
        }
        // 탈퇴 취소 특별혜택
        if (memberDto.getIsBenefit() != null) {
            if (memberDto.getIsBenefit() == 0) {
                memberDto.setBenefitText(super.langMessage("lang.member.benefit.ok")); // 지급 (받은적 있음)
            } else if (memberDto.getIsBenefit() == 1) {
                memberDto.setBenefitText(super.langMessage("lang.member.benefit.no")); // 미지급 (받은적 없음)
            }
        }
    }

    /********************************************************************************
     * Validation
     ********************************************************************************/

    /**
     * 비밀번호 찾기 > 회원 아이디 유효성검사
     *
     * @param inputId (입력받은 아이디)
     * @throws Exception
     */
    private void idValidate(String inputId) {

        //회원이 입력한 아이디 정보가 없는 경우
        if (inputId == null || inputId.isEmpty()) {
            throw new CustomException(CustomError.ID_EMPTY); // 아이디를 입력해주세요.
        }

        //DB에 존재하지 않는 아이디를 입력한 경우
        int idCount = memberDaoSub.getCountById(inputId);
        if (idCount < 1) {
            throw new CustomException(CustomError.ID_CORRESPOND_ERROR); // 입력하신 아이디를 찾을 수 없습니다.
        }
    }

    /**
     * 회원 비밀번호 변경 유효성검사
     *
     * @param memberDto : 회원 id
     * @throws Exception
     */
    private void modifyPasswordValidate(MemberDto memberDto) throws Exception {

        // 변경할 비밀번호 / 변경할 비밀번호 확인
        String newPassword = memberDto.getNewPassword();
        String newPasswordConfirm = memberDto.getNewPasswordConfirm();

        //변경할 비밀번호를 입력하지 않은 경우
        if (newPassword == null || newPassword.isEmpty()) {
            throw new CustomException(CustomError.NEW_PASSWORD_EMPTY); // 변경할 비밀번호를 입력해주세요.
        }

        //변경할 비밀번호 확인란을 입력하지 않은 경우
        if (newPasswordConfirm == null || newPasswordConfirm.isEmpty()) {
            throw new CustomException(CustomError.NEW_PASSWORD_CONFIRM_EMPTY); // 비밀번호 확인란을 입력해주세요.
        }

        // 변경할 비밀번호가 형식에 맞지 않음 : 영문/숫자/특수문자 중 2가지 이상 포함 6 ~ 20자
        if (!Pattern.matches(pwdRegex, newPassword)) {
            throw new CustomException(CustomError.PASSWORD_PATTERN_NOT_MATCH); // 비밀번호 형식에 맞게 입력해주세요.
        }

        // 비밀번호 확인값이 형식에 맞지 않음 : 영문/숫자/특수문자 중 2가지 이상 포함 6 ~ 20자
        if (!Pattern.matches(pwdRegex, newPasswordConfirm)) {
            throw new CustomException(CustomError.PASSWORD_PATTERN_NOT_MATCH); // 비밀번호 형식에 맞게 입력해주세요.
        }

        // 변경할 비밀번호와 비밀번호 확인값이 일치하지 않음
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(CustomError.PASSWORD_CONFIRM); // 비밀번호를 동일하게 입력해주세요.
        }

        // 기존 비밀번호 가져오기(로그인 비밀번호 변경 : 회원 idx로 조회 / 비로그인 비밀번호 재설정 : 회원 id로 조회)
        String oldPassword = memberDaoSub.getOldPassword(memberDto);

        // 변경할 비밀번호 암호화
        newPassword = super.encrypt(newPassword);

        // 변경할 비밀번호가 이전 비밀번호와 동일한 경우
        if (newPassword.equals(oldPassword)) {
            throw new CustomException(CustomError.PASSWORD_CORRESPOND_ERROR); // 이전 비밀번호와 같아요.
        }

        // 변경할 비밀번호 확인란 암호화
        newPasswordConfirm = super.encrypt(newPasswordConfirm);

        // 변경할 비밀번호 확인란 값이 이전 비밀번호와 동일한 경우
        if (newPasswordConfirm.equals(oldPassword)) {
            throw new CustomException(CustomError.PASSWORD_CORRESPOND_ERROR); // 이전 비밀번호와 같아요.
        }
    }

    /**
     * 기존 닉네임이 null 인 회원 유효성 검사
     *
     * @param memberDto
     */
    private boolean nullNickUserValidate(MemberDto memberDto) {

        String nick = memberDto.getNick(); // 변경할 닉네임

        // 닉네임 설정하지 않음
        if (nick == null || nick.isEmpty()) {
            return false;
        }

        // 닉네임 변경 공통 유효성
        modifyNickCommonValidate(memberDto);

        // 닉네임 설정함
        return true;
    }

    /**
     * 기존 닉네임이 존재하는 회원 유효성 검사
     *
     * @param memberDto
     */
    private boolean notNullNickUserValidate(MemberDto memberDto) {

        String nick = memberDto.getNick(); // 변경할 닉네임
        String loginMemberNick = super.getMemberInfo(LOGIN_NICK); // 현재 회원 닉네임

        // 변경하려는 닉네임과 현재 닉네임이 같은 경우
        if (loginMemberNick.equalsIgnoreCase(nick)) {
            return false;
        }
        // 닉네임 변경 공통 유효성
        modifyNickCommonValidate(memberDto);

        // 기존 닉네임과 변경하려는 닉네임이 다른 경우
        return true;
    }


    /**
     * 닉네임 변경 공통 유효성 검사
     *
     * @param memberDto
     */
    private void modifyNickCommonValidate(MemberDto memberDto) {

        String nick = memberDto.getNick(); // 변경할 닉네임

        if (memberDto.getIdx() == null || memberDto.getIdx() < 1) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해주세요.
        }

        if (nick == null || nick.isEmpty()) {
            throw new CustomException(CustomError.NICK_EMPTY); // 닉네임 값
        }

        // 닉네임 길이 검사
        if (nick.length() < 1 || nick.length() > 12) {
            throw new CustomException(CustomError.NICK_LENGTH_ERROR); // 최소 2자 이상 최대 12자 이하만 입력할 수 있습니다.
        }

        // 숫자/한글/영어만 입력 가능
        if (!Pattern.matches("^[0-9a-zA-Zㄱ-ㅎ가-힣]*$", nick)) {
            throw new CustomException(CustomError.NICK_STRING_ERROR); // 사용할 수 없는 문자가 포함되어 있습니다.
        }
        // 닉네임 중복체크
        Boolean isDupleNick = checkDupleNick(nick);

        if (Boolean.TRUE.equals(isDupleNick)) {
            throw new CustomException(CustomError.NICK_DUPLE); // 이미 존재하는 닉네임입니다.
        }
    }


}
