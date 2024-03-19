package com.architecture.admin.models.daosub.purchase;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface PurchaseDaoSub {

    /**
     * 회차 소장 및 대여 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getCoinUsedTotalCnt(SearchDto searchDto);

    /**
     * 대여 및 소장중인 contents Idx 리스트
     *
     * @param searchDto
     * @return
     */
    int getMemberPurchaseTotalCnt(SearchDto searchDto);

    /**
     * 최근 본 작품 리스트 개수 카운트(내 서재)
     *
     * @param searchDto
     * @return
     */
    int getMemberLastViewTotalCnt(SearchDto searchDto);

    /**
     * 유효한 관심 컨텐츠 idx 인지 조회
     *
     * @param searchDto : idxList, memberIdx
     * @return
     */
    int getLastViewIdxListCnt(SearchDto searchDto);

    /**
     * 유효한 코인 사용내역(구매내역) & 구매 회차 인지 조회
     *
     * @param purchaseDto
     * @return
     */
    int getPurchaseCnt(PurchaseDto purchaseDto);

    /**
     * 유효한 구매 회차 idx 인지 조회
     *
     * @param searchDto
     * @return
     */
    int getPurchaseIdxListCnt(SearchDto searchDto);

    /**
     * 코인 사용 내역 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<PurchaseDto> getCoinUsedList(SearchDto searchDto);

    /**
     * 최근 본 작품 리스트 (내 서재)
     *
     * @param searchDto
     * @return
     */
    List<PurchaseDto> getMemberLastViewList(SearchDto searchDto);

    /**
     * 대여 및 소장 리스트 (내 서재)
     *
     * @param searchDto
     * @return
     */
    List<PurchaseDto> getMemberPurchaseList(SearchDto searchDto);

    /**
     * 보유중인 회차 코인 총합(무료 회차 제외)
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), epIdxList(회차 idx 리스트), sort(무료 회차 최대값)
     * @return
     */
    int getRentTotalCoin(PurchaseDto purchaseDto);

    /**
     * 소장중인 회차 코인 총합
     *
     * @param purchaseDto
     * @return
     */
    int getHaveTotalCoin(PurchaseDto purchaseDto);

    /**
     * 보유중인 회차 idx 리스트 조회
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx), type(1:대여, 2:소장, null: 모두 조회)
     * @return
     */
    List<Long> getEpisodeIdxListFromPurchase(PurchaseDto purchaseDto);

    /**
     * 해당 회차 idx 보유중인지 카운트
     *
     * @param purchaseDto : episodeIdx, memberIdx, type
     * @return
     */
    int getMemberPurchaseCnt(PurchaseDto purchaseDto);

    /**
     * 해당 회차가 무료 OR 이벤트 무료 회차인지 조회
     *
     * @param contentsIdx
     * @return
     */
    PurchaseDto getEpisodeFreeInfo(Integer contentsIdx);

    /**
     * 해당 회차 소장 여부 조회
     *
     * @param purchaseDto
     * @return
     */
    int getEpisodeHaveInfo(PurchaseDto purchaseDto);

    /**
     * 현재 회차 구매 여부 조회
     *
     * @param purchaseDto
     * @return
     */
    List<PurchaseDto> getEpisodePurchaseInfo(PurchaseDto purchaseDto);

    /**
     * 구매 회차 idx 리스트로 컨텐츠 idx 조회
     *
     * @param searchDto : idxList[구매 회차 idxList]
     * @return
     */
    List<Integer> getContentsIdxList(SearchDto searchDto);

    /**
     * 첫화 구매 여부
     * @param memberFirstEpisodeSearch
     * @return
     */
    PurchaseDto getMemberFirstEpisode(Map<String, Object> memberFirstEpisodeSearch);
}
