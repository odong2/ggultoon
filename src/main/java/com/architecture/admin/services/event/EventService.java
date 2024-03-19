package com.architecture.admin.services.event;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.event.EventDao;
import com.architecture.admin.models.dao.login.JoinDao;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.architecture.admin.libraries.utils.EventUtils.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService extends BaseService {
    private final JoinDao joinDao;
    private final EventDao eventDao;
    private final ContentDaoSub contentDaoSub;
    private final EpisodeDaoSub episodeDaoSub;

    /**********************************************************************
     * Select
     *********************************************************************/

    /**
     * 이벤트 쿠폰 확인
     * @param site 가입 사이트
     * @param coupon 쿠폰코드
     */
    public Boolean couponCheck(String site, String coupon){
        String couponCode = switch (site) {
            case "me2disk" -> "만화는 역시 꿀툰";
            case "fileis" -> "꿀툰의 추가 혜택받기";
            case "filecity" -> "꿀툰의 띵작은?";
            case "filecast" -> "꿀툰은 내 동반자";
            default -> "꿀툰은 사랑입니다.";
        };
        return (coupon.equals(couponCode)||coupon.equals("꿀툰보다 싼데 없음"));
    }


    /**********************************************************************
     * insert
     *********************************************************************/

    /**
     * OTT 이벤트 LOG
     * @param memberIdx 회원 idx
     * @throws Exception
     */
    public void ottEvent(Long memberIdx, Integer point, String eventType) {
        MemberOttDto eventLog = MemberOttDto.builder()
            .memberIdx(memberIdx)
            .eventType(eventType)
            .point(point)
            .regdate(dateLibrary.getDatetime())
            .build();
        joinDao.insertEventLog(eventLog);
    }


    /**
     * OTT 통계 업데이트
     * @param ottInfo 이벤트 회원 정보
     * @throws Exception
     */
    public void insertEventOtt(MemberOttDto ottInfo) {
        // 통계 업데이트
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String today = formatter.format(new Date());
        ottInfo.setToday(today);
        ottInfo.setPoint(1000);
        ottInfo.setCouponCnt(1);
        joinDao.insertEventOtt(ottInfo);
    }

    /**
     * 전작품 무료 감상 이벤트
     * 이벤트 참여 내역 및 통계 데이터 등록 목적
     * (24.02.05 ~ 24.02.12)
     *
     * @param purchaseDto 이벤트 참여 정보
     */
    public void freeViewEvent(PurchaseDto purchaseDto) {

        // 이벤트 진행 상태 유효성 체크
        if (!EVENT_STATE || !dateLibrary.checkEventState(START_FREE_VIEW, END_FREE_VIEW)) {
            throw new CustomException(CustomError.EVENT_END); // 종료된 이벤트입니다. 다음 이벤트를 기다려 주세요.
        }
        
        // 작품 및 회차 IDX 유효성 체크
        contentIdxValidate(purchaseDto.getContentsIdx());
        episodeIdxValidate(purchaseDto.getEpisodeIdx());

        // type 유효성 체크
        if (purchaseDto.getType() == null || purchaseDto.getType() < 1 || purchaseDto.getType() > 2) {
            throw new CustomException(CustomError.PURCHASE_TYPE_ERROR); // 구매유형이 올바르지 않습니다.
        }

        // route 유효성 체크
        if (purchaseDto.getRoute() == null || purchaseDto.getRoute() < 1 || purchaseDto.getRoute() > 2) {
            throw new CustomException(CustomError.PURCHASE_ROUTE_ERROR); // 잘못된 구매 경로입니다.
        }

        // userType 유효성 체크
        if (purchaseDto.getUserType() == null || purchaseDto.getUserType() < 1 || purchaseDto.getUserType() > 2) {
            throw new CustomException(CustomError.PURCHASE_ROUTE_ERROR); // 잘못된 구매 경로입니다.
        }

        // 이벤트 참여일 및 종료일 set
        purchaseDto.setNowDate(dateLibrary.getDatetime()); // 참여일 set
        purchaseDto.setExpiredate(dateLibrary.localTimeToUtc(END_FREE_VIEW)); // 종료일 set

        // 이벤트 참여 내역 등록
        eventDao.insertFreeViewInfo(purchaseDto);

        // 이벤트 참여 통계 집계
        purchaseDto.setViewCnt(1);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String today = formatter.format(new Date());
        purchaseDto.setNowDate(today); // 집계일 set(yyyy-mm-dd)
        eventDao.insertFreeViewStat(purchaseDto);
    }

    /**********************************************************************
     * SUB
     *********************************************************************/

    /**
     * 작품 IDX 유효성 체크
     *
     * @param contentIdx (선택한 컨텐츠 idx)
     */
    private void contentIdxValidate(Integer contentIdx) {

        // 선택한 작품 IDX 값이 없는 경우
        if (contentIdx == null || contentIdx < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_EMPTY); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // dto set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentIdx); // 작품 IDX
        searchDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간

        // 유효한 작품 IDX 값인지 DB 조회
        int contentCnt = contentDaoSub.getContentCountByIdx(searchDto);

        // 유효한 작품이 아닐 경우
        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
        }
    }

    /**
     * 회차 IDX 유효성 검사
     */
    private void episodeIdxValidate(Long episodeIdx) {

        // 회차 IDX 기본 유효성 검사
        if (episodeIdx == null || episodeIdx < 1L) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

        // dto set
        EpisodeDto episodeDto = EpisodeDto.builder()
                .idx(episodeIdx) // 회차 IDX
                .nowDate(dateLibrary.getDatetime())// 현재 시간
                .build();

        // 유효한 회차 IDX 값인지 DB 조회
        int episodeCnt = episodeDaoSub.getEpisodeCnt(episodeDto);

        // 유효한 회차가 아닐 경우
        if (episodeCnt < 1) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }
    }
}
