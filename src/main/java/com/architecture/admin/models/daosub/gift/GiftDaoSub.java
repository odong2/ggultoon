package com.architecture.admin.models.daosub.gift;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.gift.GiftDto;

import java.util.List;

public interface GiftDaoSub {

    /**
     * 회원이 받을 수 있는 선물 개수 카운트
     * @param searchDto : memberIdx(회원 idx),
     *                    device(기기 정보),
     *                    searchType(검색 유형 - 오늘/내일),
     *                    nowDate(현재 시간),
     *                    startDate(조회 기간 시작일),
     *                    endDate(조회 기간 종료일)
     */
    int getAvailableGiftTotalCnt(SearchDto searchDto);

    /**
     * 회원이 받을 수 있는 선물 리스트 조회
     * @param searchDto : memberIdx(회원 idx),
     *                    device(기기 정보),
     *                    searchType(검색 유형 - 오늘/내일),
     *                    nowDate(현재 시간),
     *                    startDate(조회 기간 시작일),
     *                    endDate(조회 기간 종료일)
     */
    List<GiftDto> getAvailableGiftList(SearchDto searchDto);

    /**
     * 회원이 실제 지급 받은 선물 IDX 리스트 조회
     * @param giftDto : memberCi(회원 CI 정보),
     *                  contentsIdx(작품 idx),
     *                  nowDate(현재 시간),
     *                  startDate(조회 기간 시작일),
     *                  endDate(조회 기간 종료일)
     */
    List<Long> getMemberGiftIdxList(GiftDto giftDto);

    /**
     * 회원이 특정 작품에 대해 지급 받은 선물 개수 조회
     * @param searchDto : memberIdx(회원 idx),
     *                    contentsIdx(작품 idx),
     *                    nowDate(현재 시간)
     */
    int getMemberGiftCnt(SearchDto searchDto);

    /**
     * 회원이 특정 작품에 대해 지급 받은 유효한 선물 리스트 조회
     * @param searchDto : memberIdx(회원 idx),
     *                    contentsIdx(작품 idx),
     *                    nowDate(현재 시간),
     *                    minExceptCnt(최신 회차 보호 개수)
     */
    List<GiftDto> getMemberGiftList(SearchDto searchDto);

    /**
     * 동일한 작품으로 지급 받은 작품 이용권 중 제외할 최신 회차 개수의 최소값
     * @param searchDto : memberIdx(회원 idx),
     *                    contentsIdx(작품 idx),
     *                    nowDate(현재 시간)
     */
    int getMemberMinExceptCnt(SearchDto searchDto);
}
