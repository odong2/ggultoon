package com.architecture.admin.controllers.v1.event;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.login.JoinDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.event.EventService;
import com.architecture.admin.services.login.JoinService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/event")
public class EventV1Controller extends BaseController {

    private final CoinService coinService;
    private final JoinService joinService;
    private final EventService eventService;
    private final MemberDaoSub memberDaoSub;
    private final JoinDaoSub joinDaoSub;

    @PostMapping("/coupon")
    public String coupon(@RequestBody MemberOttDto memberOttDto) throws Exception {

        // 세션에서 가져온 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // 회원 기본 정보 조회
        MemberDto memberDto = memberDaoSub.getMemberInfoByIdx(memberIdx);

        // 꿀툰 쿠폰 이벤트 종료
        if(memberDto.getSite().equals("ggultoon")){
            // 종료된 이벤트 입니다. 다음 이벤트를 기다려 주세요.
            throw new CustomException(CustomError.EVENT_END);
        }


        if(memberDto.getCi() == null){
            // 본인인증이 필요합니다.
            throw new CustomException(CustomError.NAME_CHECK_ERROR);
        }

        // 이벤트 타입 설정 - 쿠폰
        memberDto.setEventType("coupon");
        // 이벤트 참여내역 확인
        MemberOttDto eventCiCheck = joinService.eventCiCheck(memberDto);
        if(eventCiCheck != null){
            // pushAlarm(eventCiCheck.toString(),"LJH");
            // 이벤트 참여내역이 있습니다.
            throw new CustomException(CustomError.EVENT_DUPLE_ERROR);
        }

        // 쿠폰 입력값 확인
        if(!eventService.couponCheck(memberDto.getSite(),memberOttDto.getCoupon())){
            // 쿠폰코드를 확인해주세요.
            throw new CustomException(CustomError.EVENT_COUPON_CODE_ERROR);
        }

        // 회원 기본 정보 조회
        MemberOttDto ottInfo = joinDaoSub.getEventMember(memberIdx);

        // OTT 이벤트 회원정보
        if(ottInfo != null) {
            // ottInfo 가 null 일때 toString으로 인하여 error 발생
            // pushAlarm(ottInfo.toString(),"LJH");

            // API 전송값 설정
            JSONObject ott = new JSONObject();
            ott.put("userid", ottInfo.getOttId());
            ott.put("gtUserid", ottInfo.getId());
            ott.put("eventType", ottInfo.getEventType());
            ott.put("bannerCode", ottInfo.getBannerCode());
            ott.put("isGive", "Y");
            ott.put("rtMsg", "ok");
            ott.put("point", "1000");

            // OTT 이벤트 결과 전송
            String res = joinService.ottEventSend(ottInfo.getReturnUrl(),ott.toString());
            // 회원정보 업데이트
            // joinService.updateMemberOtt(ottInfo.getMemberIdx(), res);

            // 통계 업데이트
            eventService.insertEventOtt(ottInfo);

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
                data.put("rtMsg", "ci||" + ottInfo.getSite() + "||" + ottInfo.getOttId());
                data.put("point", "0");

                // OTT 이벤트 결과 전송
                String dataRes = joinService.ottEventSend(dto.getReturnUrl(),data.toString());
                // 회원정보 업데이트
                // joinService.updateMemberOtt(dto.getMemberIdx(),data.getString("rtMsg"), dataRes);

                // pushAlarm(data.toString(),"LJH");
            }
        }

        // 이벤트 log 입력
        eventService.ottEvent(memberIdx,1000, "coupon");
        // 회원 마일리지 지급
        coinService.addMileage(memberIdx, 1000,"쿠폰","꿀툰 쿠폰 이벤트");

        // return value
        String sErrorMessage = "lang.common.success.regist"; // 등록 완료하였습니다.
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message);
    }

    /**
     * 전작품 무료 감상 이벤트
     * 이벤트 참여 내역 및 통계 데이터 등록 목적(중복 제거 X)
     * (24.02.05 ~ 24.02.12)
     *
     * @param contentIdx  : contents.idx
     * @param episodeIdx  : episode.idx
     * @param purchaseDto : type(감상 유형 -> 1: 대여, 2: 소장),
     *                      route(감상 경로 -> 1: 웹, 2: 어플),
     *                      userType(회원 구분 -> 1: OTT 토큰, 2: OTT 가입 회원)
     * @return
     */
    @PostMapping("/free/contents/{contentIdx}/episodes/{episodeIdx}")
    public String freeViewEvent(@PathVariable Integer contentIdx,
                                @PathVariable Long episodeIdx,
                                @RequestBody PurchaseDto purchaseDto) {

        // 회원 IDX session 체크
        super.checkSession();
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // dto set
        purchaseDto.setContentsIdx(contentIdx); // 작품 idx
        purchaseDto.setEpisodeIdx(episodeIdx);  // 회차 idx
        purchaseDto.setMemberIdx(memberIdx);    // 회원 idx

        // 이벤트 참여 내역 및 통계 데이터 등록
        eventService.freeViewEvent(purchaseDto);

        // return value
        String sErrorMessage = "lang.common.success.regist"; // 등록 완료하였습니다.
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message);
    }
}
