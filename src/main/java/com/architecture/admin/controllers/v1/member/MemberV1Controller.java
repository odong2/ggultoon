package com.architecture.admin.controllers.v1.member;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberAppDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.models.dto.setting.SettingDto;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.content.ContentService;
import com.architecture.admin.services.member.MemberService;
import com.architecture.admin.services.notification.NotificationService;
import com.architecture.admin.services.payment.PaymentService;
import com.architecture.admin.services.purchase.PurchaseService;
import com.architecture.admin.services.setting.SettingService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/member")
public class MemberV1Controller extends BaseController {

    private final MemberService memberService;
    private final PaymentService paymentService;
    private final ContentService contentService;
    private final PurchaseService purchaseService;
    private final CoinService coinService;
    private final SettingService settingService;
    private final NotificationService notificationService;
    private String searchSuccessMsg = "lang.common.success.search"; // 조회 완료하였습니다.
    private String deleteSuccessMsg = "lang.common.success.delete"; // 삭제 완료하였습니다.

    /**
     * 회원탈퇴 취소 혜택 여부 조회
     *
     * @return
     */
    @GetMapping("/benefit")
    public String checkDeleteTry() {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 탈퇴 시도한적 있는지 체크
        JSONObject data = memberService.checkDeleteBenefit(memberIdx);

        // return value
        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 회원탈퇴 취소 혜택 지급
     *
     * @return
     */
    @PostMapping("/benefit")
    public String giveDeleteCancelBenefit() {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 탈퇴 시도한적 있는지 체크
        memberService.giveDeleteCancelBenefit(memberIdx);

        // return value
        String message = super.langMessage("lang.member.success.delete.benefit"); // 특별 혜택으로 10코인을 지급받았습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 회원 탈퇴
     *
     * @return
     */
    @DeleteMapping()
    public String deleteMember() {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 탈퇴
        memberService.deleteMemberByIdx(memberIdx);

        // return value
        String message = super.langMessage("lang.member.success.delete"); // 회원 탈퇴하였습니다

        return displayJson(true, "1000", message);
    }

    /**
     * 회원 정보 조회
     *
     * @return
     */
    @GetMapping()
    public String memberInfo() {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        JSONObject data = memberService.getMemberTotalInfo(memberIdx);

        // return value
        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }


    /**
     * 닉네임 사용가능 체크 (정책상 로그인한 회원만 api 호출 가능)
     *
     * @param memberDto : nick(변경할 닉네임)
     * @return : message(변경 가능 여부 메시지)
     */

    @GetMapping("/nick/check")
    public String checkNick(@ModelAttribute MemberDto memberDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        memberDto.setIdx(memberIdx);

        // 닉네임 중복 체크 (변경하려는 닉네임과 기존 닉네임이 같으면 false)
        Boolean isNotEqualsNick = memberService.checkNick(memberDto);

        // return value
        String message = super.langMessage("lang.member.success.nick");      // 사용할 수 있는 닉네임입니다.
        String code = "1000";

        if (Boolean.FALSE == isNotEqualsNick) {
            message = super.langMessage("lang.member.success.nick.equals");  // 기존 닉네임과 동일합니다.
            code = "1001";
        }

        return displayJson(true, code, message);
    }

    /**
     * 내 정보 수정 (닉네임 & 비밀번호 수정)
     *
     * @param memberDto
     * @return message : 정보 변경 여부 메시지
     */
    @PutMapping()
    public String modifyMemberNickAndPassword(@RequestBody MemberDto memberDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 IDX set
        memberDto.setIdx(memberIdx);

        // 내정보 변경
        Boolean isModify = memberService.modifyNickAndPassword(memberDto);
        // return value
        String message = super.langMessage("lang.member.success.change.member.info"); // 회원정보를 변경하였습니다.
        String code = "1000";

        // 회원정보 변경된 사항 없음
        if (Boolean.FALSE == isModify) {
            message = super.langMessage("lang.member.exception.change.empty");     // 변경할 회원정보가 없습니다.
            code = "2000";
        }

        return displayJson(true, code, message);
    }

    /**
     * 닉네임 변경
     *
     * @return
     */
    @PutMapping("/nick")
    public String changeNickName(@RequestBody MemberDto memberDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 IDX set
        memberDto.setIdx(memberIdx);

        // 닉네임 변경
        memberService.modifyNick(memberDto);

        // return value
        String message = super.langMessage("lang.member.success.change.nick"); // 닉네임을 변경하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 비밀번호 변경
     *
     * @param memberDto : newPassword(변경할 비밀번호), newPasswordConfirm(비밀번호 확인)
     * @return
     */
    @PutMapping("/password")
    public String modifyPassword(@RequestBody MemberDto memberDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        memberDto.setIdx(memberIdx);

        // 비밀번호 변경
        memberService.modifyPasswordLogin(memberDto);

        // return value
        String message = super.langMessage("lang.member.success.reset.password"); // 비밀번호를 재설정하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 회원 성인 여부 체크
     *
     * @return
     */
    @GetMapping("/check/adult")
    public String checkAdult() {

        // 회원 IDX session 체크
        super.checkSession();

        // 세션에서 성인 여부 상태값 가져오기
        JSONObject memberInfo = new JSONObject(session.getAttribute(SessionConfig.MEMBER_INFO).toString());
        int isAdult = memberInfo.getInt(SessionConfig.ADULT);

        JSONObject data = new JSONObject();
        data.put("adult", isAdult);

        // return value
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마이페이지 메인
     *
     * @param request   : 요청 정보
     * @param searchDto : page, recordSize
     * @param result
     * @return
     */
    @GetMapping("/mypage")
    public String myPage(HttpServletRequest request,
                         @ModelAttribute @Valid SearchDto searchDto,
                         BindingResult result) {

        // 회원 IDX session 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setIdx(memberIdx);

        String device = request.getHeader("User-Agent");

        // 모바일
        if (isMobile(device)) {
            searchDto.setDevice(ORIGIN);
            // 태블릿
        } else if (isTablet(device)) {
            searchDto.setDevice(ORIGIN);
            // pc
        } else {
            searchDto.setDevice(ORIGIN);
        }

        // 리스트 조회
        JSONObject data = memberService.getMyPageInfo(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마이페이지 - 회원 코인 결제내역 (결제내역 + 마일리지)
     *
     * @param searchDto
     * @param result
     * @return
     */
    @GetMapping("/payment")
    public String memberPaymentList(@ModelAttribute @Valid SearchDto searchDto,
                                    BindingResult result) {

        // 회원 IDX session 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setIdx(memberIdx);

        // 리스트 조회
        JSONObject data = paymentService.getPaymentList(searchDto); // 페이징 & 리스트 조회

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마이페이지 - 회원 코인 사용내역
     *
     * @param searchDto : page, recordSize, searchType(rent, have)
     *                  : categoryIdx(0: 전체, 1: 웹툰, 2: 만화, 3: 소설)
     * @param result
     * @return
     */
    @GetMapping("/coin/use")
    public String memberUsedCoinList(@ModelAttribute @Valid SearchDto searchDto,
                                     BindingResult result) {

        // 회원 IDX session 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        searchDto.setIdx(memberIdx);

        // 리스트 조회
        JSONObject data = purchaseService.getCoinUsedList(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 코인 & 마일리지 소멸 리스트 조회
     *
     * @param searchDto : searchType(검색 유형 : coin(코인), mileage(마일리지))
     * @param result
     * @return
     */
    @GetMapping("/coin/expire")
    public String memberExpireCoinList(@ModelAttribute @Valid SearchDto searchDto,
                                       BindingResult result) {

        // 회원 IDX session 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        searchDto.setIdx(memberIdx);

        // 코인 소멸 리스트 조회
        JSONObject data = coinService.getExpireCoinList(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마일리지 지급 내역 조회(만료되지 않은 마일리지만)
     * 1. 페이백 지급
     * 2. 관리자 지급
     * 3. 이벤트 지급
     *
     * @param searchDto : type(유형) > 현재 정책 상으로는 필요없음 / 빈값으로 전달 받을 예정
     * @param result
     * @return
     */
    @GetMapping("/coin/mileage")
    public String memberMileageList(@ModelAttribute @Valid SearchDto searchDto,
                                    BindingResult result) {

        // 세션 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        // 회원 idx set
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setMemberIdx(memberIdx);

        // 마일리지 지급 내역 조회
        JSONObject data = coinService.getGivenMileageList(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 오늘 받은 로그인 마일리지 지급 내역 조회
     *
     * @return
     */
    @GetMapping("/coin/mileage/login")
    public String getTodayLoginMileageInfo() {

        // 세션 체크
        super.checkSession();

        // 회원 idx set
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        SearchDto searchDto = new SearchDto();
        searchDto.setMemberIdx(memberIdx);

        // 오늘 받은 로그인 마일리지 지급 내역 조회
        JSONObject data = coinService.getTodayLoginMileageInfo(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 회원 코인 사용내역 삭제
     *
     * @param purchaseDto : idxList
     * @return
     */
    @DeleteMapping("/coin/use")
    public String deleteMemberUsedCoin(@RequestBody PurchaseDto purchaseDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        purchaseDto.setMemberIdx(memberIdx);
        // 코인 사용내역 삭제
        purchaseService.deleteMemberUsedCoinList(purchaseDto);

        String message = super.langMessage("lang.common.success.delete"); // 삭제 완료하였습니다.

        return displayJson(true, "1000", message);
    }

    /**************************************************************************************
     * 내 서재
     **************************************************************************************/

    /**
     * 내 서재 리스트 조회
     *
     * @param searchDto : type(구매 유형)
     *                  -> view : 내가 본 작품 조회
     *                  -> rent : 대여 작품 조회
     *                  -> have : 소장 작품 조회
     *                  -> favorite : 관심 작품
     * @return
     */
    @GetMapping("/library")
    public String memberLibrary(HttpServletRequest request,
                                @ModelAttribute @Valid SearchDto searchDto,
                                BindingResult result) {

        // 회원 IDX session 체크
        super.checkSession();

        if (result.hasErrors()) {
            return displayError(result);
        }

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 검색 조건
        String searchType = searchDto.getSearchType();

        if (searchType == null || searchType.isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        searchDto.setIdx(memberIdx); // 회원 idx set
        JSONObject data = null;

        String device = request.getHeader("User-Agent");

        // 모바일
        if (isMobile(device)) {
            searchDto.setDevice(ORIGIN);
            // 태블릿
        } else if (isTablet(device)) {
            searchDto.setDevice(ORIGIN);
            // pc
        } else {
            searchDto.setDevice(ORIGIN);
        }

        // 내가 본 작품 리스트
        if (searchType.equals("view")) {
            data = purchaseService.getMemberLastViewList(searchDto);
            // 대여 또는 소장 작품 리스트
        } else if (searchType.equals("rent") || searchType.equals("have")) {
            data = purchaseService.getMemberPurchaseList(searchDto);
            // 관심 작품 리스트
        } else if (searchType.equals("favorite")) {
            data = contentService.getFavoriteContentsList(searchDto);
        }

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.
        return displayJson(true, "1000", message, data);
    }

    /**
     * 내 서재 컨텐츠 상세 정보 (내 서재)
     *
     * @param episodeIdx :현재 회차 번호
     * @param request
     * @return
     */
    @GetMapping("/library/episodes/{idx}")
    public String myLibraryContentDetail(@PathVariable(name = "idx") Long episodeIdx,
                                         HttpServletRequest request) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        SearchDto searchDto = new SearchDto();
        searchDto.setEpisodeIdx(episodeIdx);
        searchDto.setMemberIdx(memberIdx); // 회원 idx set

        JSONObject data = null;

        String device = request.getHeader("User-Agent");

        // 모바일
        if (isMobile(device)) {
            searchDto.setDevice(ORIGIN);
            // 태블릿
        } else if (isTablet(device)) {
            searchDto.setDevice(ORIGIN);
            // pc
        } else {
            searchDto.setDevice(ORIGIN);
        }
        // 컨텐츠 상세 정보 조회
        data = contentService.getContentsInfoFromLibrary(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 최근 본 작품 리스트 삭제 (내 서재)
     *
     * @param searchDto : idxList
     * @return
     */
    @DeleteMapping("/library/view")
    public String deleteLastViewList(@RequestBody SearchDto searchDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setMemberIdx(memberIdx); // 회원 idx set
        // 내가 본 작품 삭제
        purchaseService.deleteMemberLastViewList(searchDto);

        String sMessage = super.langMessage("lang.common.success.delete"); // 삭제 완료하였습니다.

        return displayJson(true, "1000", sMessage);
    }

    /**
     * 대여 리스트 삭제(내 서재)
     *
     * @param searchDto : idxList
     * @return
     */
    @DeleteMapping("/library/purchase")
    public String deletePurchase(@RequestBody SearchDto searchDto) {
        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setMemberIdx(memberIdx); // 회원 idx set

        // 소장 및 대여 삭제
        purchaseService.deleteMemberPurchaseList(searchDto);

        String sMessage = super.langMessage("lang.common.success.delete"); // 삭제 완료하였습니다.

        return displayJson(true, "1000", sMessage);
    }

    /**
     * 관심 작품 리스트 삭제(내 서재)
     *
     * @param searchDto : idxList
     * @return
     */
    @DeleteMapping("/library/favorite")
    public String deleteFavoriteList(@RequestBody SearchDto searchDto) {
        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setMemberIdx(memberIdx); // 회원 idx set

        // 관심 작품 삭제
        contentService.deleteMemberFavoriteList(searchDto);

        String sMessage = super.langMessage("lang.common.success.delete"); // 삭제 완료하였습니다.

        return displayJson(true, "1000", sMessage);
    }

    /**************************************************************************************
     * 환경설정
     **************************************************************************************/

    /**
     * 마이페이지 - 환경설정 목록
     *
     * @param
     * @return
     */
    @GetMapping("/settings")
    public String memberSettingList() {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원별 환경설정 리스트 조회
        JSONObject data = settingService.getMemberSettingList(memberIdx);

        // 결과 메세지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마이페이지 - 환경설정 상태 변경
     *
     * @param settingIdx (변경할 환경설정 idx)
     * @return
     */
    @PutMapping("/settings/{idx}")
    public String modifyMemberSetting(@PathVariable(name = "idx") Integer settingIdx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // dto set
        SettingDto settingDto = SettingDto.builder()
                .memberIdx(memberIdx)
                .settingIdx(settingIdx)
                .build();

        // 환경설정 상태값 변경
        settingService.modifyMemberSetting(settingDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.setting.success.modify"); // 설정을 변경하였습니다.

        return displayJson(true, "1000", message);
    }

    /**************************************************************************************
     * 알림
     **************************************************************************************/

    /**
     * 마이페이지 - 알림 목록
     *
     * @param searchDto
     * @return
     */
    @GetMapping("/notifications")
    public String memberNotificationList(@ModelAttribute @Valid SearchDto searchDto,
                                         BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return displayError(result);
        }

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // memberIdx 세팅
        searchDto.setMemberIdx(memberIdx);

        // 회원별 알림 리스트 조회
        JSONObject data = notificationService.getMemberNotificationList(searchDto);

        // 결과 메세지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 마이페이지 - 선택한 알림 읽음 표시
     *
     * @param idx (알림 idx)
     * @return
     */
    @PutMapping("/notifications/{idx}")
    public String checkMemberNotification(@PathVariable(name = "idx") Long idx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 서비스단으로 전달할 데이터 세팅
        NotificationDto dto = new NotificationDto();
        dto.setMemberIdx(memberIdx); // 회원 idx
        dto.setIdx(idx); // 알림 idx

        // 회원 알림 읽음 표시
        notificationService.updateCheckDate(dto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.notification.success.check"); // 알림을 확인했습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 마이페이지 - 선택한 알림 목록 삭제
     *
     * @param notificationDto (삭제할 알림 idx 목록)
     * @return
     */
    @DeleteMapping("/notifications")
    public String deleteMemberNotification(@RequestBody NotificationDto notificationDto) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 삭제할 알림 목록이 없다면
        if (notificationDto.getIdxList().isEmpty()) {
            throw new CustomException(CustomError.NOTIFICATION_DELETE_IDX_EMPTY); // 삭제할 알림을 선택해주세요.
        }

        // 삭제할 알림 목록을 담을 리스트 생성
        List<NotificationDto> list = new ArrayList<>();

        // 회원이 선택한 알림 목록 순회
        for (Long index : notificationDto.getIdxList()) {

            NotificationDto dto = NotificationDto.builder()
                    .memberIdx(memberIdx) // 회원 idx 세팅
                    .idx(index) // 알림 idx 세팅
                    .delDate(dateLibrary.getDatetime()) // 삭제일(현재시간) 세팅
                    .build();

            // 리스트에 세팅한 dto 추가
            list.add(dto);
        }

        // 선택한 알림 목록 삭제
        notificationService.deleteNotification(list);

        // 결과 메세지 처리
        String message = super.langMessage("lang.notification.success.delete"); // 알림을 삭제했습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * adid 광고아이디 저장
     * @return
     */
    @PostMapping("/app/adid")
    public String insertAdId(@RequestBody MemberAppDto memberAppDto) {

        // 회원 IDX session 체크
        super.checkSession();
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        memberAppDto.setMemberIdx(memberIdx);
        memberService.insertAdid(memberAppDto);

        // return value
        String message = super.langMessage("lang.common.success.regist"); // 저장되었습니다.
        return displayJson(true, "1000", message);
    }

    /**
     * app 토큰 저장
     * @return
     */
    @PostMapping("/app/token")
    public String insertAppToken(@RequestBody MemberAppDto memberAppDto) {

        // 회원 IDX session 체크
        super.checkSession();
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        memberAppDto.setMemberIdx(memberIdx);
        memberService.insertAppToken(memberAppDto);

        // return value
        String message = super.langMessage("lang.common.success.regist"); // 저장되었습니다.
        return displayJson(true, "1000", message);
    }

    /**
     * 로그인 및 접속 경로 체크
     */
    @GetMapping("/check")
    public String checkIsMember(HttpServletRequest request) throws Exception {

        // return value
        JSONObject data = new JSONObject();
        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);
        
        // 로그인
        if (memberInfo != null) {
            data.put("memberInfo", true);

            // 비로그인
        } else {
            data.put("memberInfo", false);

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                data.put("ottVisitToken", true);

            // OTT 접속한 비성인 회원 OR 일반 회원이 비로그인 상태일 경우
            } else {
                data.put("ottVisitToken", false);
            }
        }
        return displayJson(true, "1000", message, data);
    }

    /**
     * OTT 접속 토큰 정보로 구운 쿠키 제거
     */
    @DeleteMapping("/cookie")
    public String removeTokenCookie(HttpServletRequest request, HttpServletResponse response) {

        // OTT 접속 토큰 정보로 구운 쿠키 제거
        super.removeOttVisitTokenCookie(request, response);

        // return value
        String message = super.langMessage(deleteSuccessMsg); // 삭제 완료하였습니다.
        return displayJson(true, "1000", message);
    }
}