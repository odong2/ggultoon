package com.architecture.admin.models.daosub.coin;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CoinDaoSub {

    /**
     * 회원 코인 정보(코인, 보너스 코인, 마일리지, 티켓 카운트 조회)
     *
     * @param idx
     * @return
     */
    CoinDto getMemberCoinInfoByMemberIdx(Long idx);

    /**
     * 코인 소멸 개수 조회
     *
     * @param searchDto
     * @return
     */
    int getExpireCoinTotalCnt(SearchDto searchDto);

    /**
     * 소멸 코인 리스트 조회
     *
     * @param searchDto
     * @return
     */

    List<CoinDto> getExpireCoinList(SearchDto searchDto);

    /**
     * 마일리지 소멸 개수 조회
     *
     * @param searchDto
     * @return
     */
    int getExpireMileageTotalCnt(SearchDto searchDto);

    /**
     * 마일리지 소멸 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<CoinDto> getExpireMileageList(SearchDto searchDto);

    /**
     * 마일리지 지급 내역 개수 조회
     * @param searchDto : memberIdx(회원 idx), nowDate(현재 시간)
     * @return
     */
    int getGivenMileageTotalCnt(SearchDto searchDto);

    /**
     * 마일리지 지급 내역 조회
     *
     * @param searchDto : memberIdx(회원 idx), nowDate(현재 시간)
     * @return
     */
    List<CoinDto> getGivenMileageList(SearchDto searchDto);

    /**
     * 회차 개별구매 가격 및 정보
     *
     * @param purchaseDto
     * @return
     */
    CoinDto getEpisodeInfo(PurchaseDto purchaseDto);

    /**
     * 전체 대여할 회차 idx, coin_rent 리스트 조회
     *
     * @param episodeDto : contentIdx(컨텐츠 idx), type(1: 대여, 2:소장), nowDate
     * @return
     */
    List<EpisodeDto> getAllEpisodeIdxAndCoin(EpisodeDto episodeDto);

    /**
     * 회원 코인 조회
     *
     * @param searchCoinDto
     * @return Integer
     */
    Integer getMemberCoin(CoinDto searchCoinDto);

    /**
     * 회원 코인 & 보너스 코인 조회
     *
     * @param restCoinDto
     * @return
     */
    CoinDto getMemberCoinAndCoinFree(CoinDto restCoinDto);

    /**
     * 회원 마일리지 조회
     *
     * @param restCoinDto
     * @return
     */
    Integer getMemberMileage(CoinDto restCoinDto);

    /**
     * 만료된 코인 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getExpireCoinIdxList(Long memberIdx);

    /**
     * 만료된 마일리지 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getExpireMileageIdxList(Long memberIdx);

    /**
     * 코인 OR 마일리지 지급일 조회
     * @param dto
     * @return
     */
    String getCoinOrMileageRegdate(NotificationDto dto);

    /**
     * 1일 1회 로그인 마일리지 지급 여부 조회
     * @param searchMember : 회원 idx, 회원 ci
     * @return
     */
    CoinDto getMemberLoginMileageInfo(MemberDto searchMember);

    /**
     * 오늘 받은 로그인 마일리지 지급 내역 조회
     * @param searchDto : 회원 idx
     * @return
     */
    List<CoinDto> getTodayLoginMileageInfo(SearchDto searchDto);
}
