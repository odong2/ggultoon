package com.architecture.admin.services.purchase;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.coin.CoinDao;
import com.architecture.admin.models.dao.content.ContentDao;
import com.architecture.admin.models.dao.episode.EpisodeDao;
import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.models.dao.purchase.PurchaseDao;
import com.architecture.admin.models.daosub.coin.CoinDaoSub;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeDaoSub;
import com.architecture.admin.models.daosub.gift.GiftDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.daosub.purchase.PurchaseDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.content.*;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.purchase.PopupInfoDto;
import com.architecture.admin.models.dto.purchase.PurchaseBuyAllDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.member.GradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.architecture.admin.libraries.utils.BadgeUtils.*;
import static com.architecture.admin.libraries.utils.CoinUtils.*;
import static com.architecture.admin.libraries.utils.ContentUtils.*;
import static com.architecture.admin.libraries.utils.EventUtils.COIN_PURCHASE_DISABLED;

@Service
@RequiredArgsConstructor
public class PurchaseService extends BaseService {

    private final ContentDao contentDao;
    private final ContentDaoSub contentDaoSub;
    private final EpisodeDao episodeDao;
    private final EpisodeDaoSub episodeDaoSub;
    private final GradeService gradeService;
    private final CoinService coinService;
    private final CoinDao coinDao;
    private final CoinDaoSub coinDaoSub;
    private final PurchaseDao purchaseDao;
    private final PurchaseDaoSub purchaseDaoSub;
    private final MemberDaoSub memberDaoSub;
    private final MemberDao memberDao;
    private final S3Library s3Library;
    private final GiftDaoSub giftDaoSub;


    /*********************************************************************
     * Select
     *********************************************************************/

    /**
     * 코인 사용내역 리스트 조회 (이용 내역)
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getCoinUsedList(SearchDto searchDto) {

        // 코인 사용내역 조회
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        // 소장 대여 set
        if (searchDto.getSearchType().equals("rent")) {
            searchDto.setType(EPISODE_RENT);
        } else if (searchDto.getSearchType().equals("have")) {
            searchDto.setType(EPISODE_HAVE);
        } else {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR);  // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        //코인 사용내역 개수 조회
        int totalCnt = purchaseDaoSub.getCoinUsedTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<PurchaseDto> purchaseList = null;

        if (totalCnt > 0) {
            // 코인 사용내역 리스트(이미지 불포함)
            purchaseList = purchaseDaoSub.getCoinUsedList(searchDto);
            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set
            // 구매유형 문자 변환
            stateText(purchaseList);
        }
        jsonData.put("coinUsedList", purchaseList);

        return jsonData;
    }

    /**
     * 전체 구매할 금액 계산
     * 이벤트 중일 경우 originEpisodeList 원본 dto 에 buyCoinPrice 이벤트 가격 세팅
     *
     * @param buyType           : 구매 방식(대여 or 소장)
     * @param originEpisodeList : 원가격 회차 리스트(idx, buyCoinPrice)
     * @param episodeEventList  : 이벤트 회차 리스트(idx, eventCoin, eventCoinRent)
     * @return buyCoinPrice      : 결제할 금액
     */
    private int getBuyCoinPrice(int buyType, List<EpisodeDto> originEpisodeList, List<EpisodeDto> episodeEventList) {

        int buyCoinPrice = 0; // 결제할 금액

        for (EpisodeDto episodeDto : originEpisodeList) {
            // 회차 이벤트 있는 경우
            Boolean isEvent = false;

            if (episodeEventList != null) {
                long epIdx = episodeDto.getIdx(); // 회차 idx

                for (int j = 0; j < episodeEventList.size(); j++) {
                    long eventIdx = episodeEventList.get(j).getIdx();           // 회차 이벤트 idx
                    int eventCoin = 0; // 회차 이벤트 가격

                    // 1. 회차 이벤트 가격 set
                    if (buyType == EPISODE_HAVE) {
                        eventCoin = episodeEventList.get(j).getEventCoin();     // 회차 이벤트(소장) 가격

                    } else {
                        eventCoin = episodeEventList.get(j).getEventCoinRent(); // 회차 이벤트(대여) 가격
                    }
                    // 2. 이벤트 중인 회차(이벤트 가격 적용)
                    if (epIdx == eventIdx) {
                        buyCoinPrice += eventCoin;  // 대여할 코인 총합
                        /** 원가격 대신 이벤트 가격 set **/
                        episodeDto.setBuyCoinPrice(eventCoin);
                        episodeEventList.remove(j); // 현재 이벤트 인덱스 삭제
                        isEvent = true;
                    }
                    // 회차 이벤트 List 사이즈가 0이면 break
                    if (episodeEventList.isEmpty()) {
                        break;
                    }
                }
            }
            // 3. 이벤트 중이 아닌 회차 원가격 적용
            if (Boolean.FALSE == isEvent) {
                buyCoinPrice += episodeDto.getBuyCoinPrice(); // 원 가격 적용
            }
        } // end of for

        return buyCoinPrice; // 최종 결제할 금액 리턴
    }

    /*********************************************************************
     * 내 서재
     *********************************************************************/

    /**
     * 최근 본 작품 리스트(내 서재)
     * -> 보유한 작품중 본 작품만
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getMemberLastViewList(SearchDto searchDto) {

        // 최근 본 작품 카운트
        int totalCnt = purchaseDaoSub.getMemberLastViewTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<PurchaseDto> lastViewContentList = null;

        if (totalCnt > 0) {
            // 최근 본 작품 리스트 조회
            lastViewContentList = purchaseDaoSub.getMemberLastViewList(searchDto);
            // 문자 변환
            typeText(lastViewContentList);
            // 이미지 fulUrl 세팅
            setImgFullUrl(lastViewContentList);
            // 작가 & 태그 set
            setAuthorAndTag(lastViewContentList);
            // 배지 코드 set
            setBadgeCode(lastViewContentList, LIBRARY);

            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set
        }
        jsonData.put("list", lastViewContentList); // 리스트 set , 없으면 length 0

        return jsonData;
    }

    /**
     * 소장 or 대여 리스트 조회 (내 서재)
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getMemberPurchaseList(SearchDto searchDto) {

        /** 기본 유효성 검사 **/
        purchaseEpisodeListValidate(searchDto);

        // 회차 소장 or 대여 리스트 조회
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        // 대여, 소장 type(integer) 지정
        if (searchDto.getSearchType().equals("rent")) {
            searchDto.setType(EPISODE_RENT);
        } else if (searchDto.getSearchType().equals("have")) {
            searchDto.setType(EPISODE_HAVE);
        }

        // 구매내역 개수 조회
        searchDto.setNowDate(dateLibrary.getDatetime()); // UTC 기준 현재 시간 set
        // 보유중인 컨텐츠 idxList 조회
        int totalCnt = purchaseDaoSub.getMemberPurchaseTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<PurchaseDto> purchaseList = null;

        if (totalCnt > 0) {
            // 보유중인 에피소드 리스트(이미지 포함) - 내서재
            purchaseList = purchaseDaoSub.getMemberPurchaseList(searchDto);
            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set

            // 구매유형 문자 변환
            stateText(purchaseList);
            setRegDateEmpty(purchaseList); // 소장 및 대여는 regDate 빈값으로 설정
            // 이미지 fulUrl 세팅
            setImgFullUrl(purchaseList);
            // 작가 & 태그 set
            setAuthorAndTag(purchaseList);
            // 배지 코드 set
            setBadgeCode(purchaseList, LIBRARY);
        }

        jsonData.put("list", purchaseList);

        return jsonData;
    }

    /*********************************************************************
     * 전체 대여 & 소장 팝업 정보
     *********************************************************************/

    /**
     * 전체 소장 팝업 정보
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     * @return
     */
    public JSONObject getAllHavePupUpInfo(PurchaseDto purchaseDto) {
        /** 컨텐츠 idx 유효성 검사 **/
        contentIdxValidate(purchaseDto.getContentsIdx());

        Integer contentIdx = purchaseDto.getContentsIdx();
        Long memberIdx = purchaseDto.getMemberIdx();

        PopupInfoDto freeHaveDto = getFreePopupInfo(contentIdx, memberIdx);
        PopupInfoDto nonFreeHaveDto = getNonFreePopupInfo(contentIdx, memberIdx);

        JSONObject jsonData = new JSONObject();

        jsonData.put("freeInfo", new JSONObject(freeHaveDto));        // 무료 회차 포함
        jsonData.put("nonFreeInfo", new JSONObject(nonFreeHaveDto));  // 무료 회차 미포함

        return jsonData;
    }

    /**
     * 전체 대여 팝업
     * 정책 -> 무료회차는 대여를 할 수 없음
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getAllRentPupUpInfo(PurchaseDto purchaseDto) {
        /** 컨텐츠 idx 유효성 검사 **/
        contentIdxValidate(purchaseDto.getContentsIdx());

        Integer contentIdx = purchaseDto.getContentsIdx();

        // 조회 위해 현재 시간 set
        purchaseDto.setNowDate(dateLibrary.getDatetime());
        // 조회용 dto
        EpisodeDto episodeDto = EpisodeDto.builder()
                .nowDate(dateLibrary.getDatetime())
                .contentIdx(contentIdx)
                .build();

        /** 1. 무료 회차 조회 **/
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

        /** 2. 이벤트 무료 회차 조회 **/
        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        // 무료 회차 최대값
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 회차 이벤트 리스트
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        List<Long> episodeEventIdxList = null; // 회차 이벤트 무료 리스트
        int eventEpisodeFree = 0;              // 회차 이벤트 무료 개수
        /** 회차 이벤트 무료(0코인) 개수 카운트 및 리스트 추가 **/
        if (episodeEventList != null) {
            for (EpisodeDto episodeEventDto : episodeEventList) {
                Integer eventCoinRent = episodeEventDto.getEventCoinRent();

                if (eventCoinRent != null && eventCoinRent == 0) {
                    eventEpisodeFree += 1; // 무료 회차 개수 증가
                    if (episodeEventIdxList == null) {
                        episodeEventIdxList = new ArrayList<>();
                    }
                    episodeEventIdxList.add(episodeEventDto.getIdx()); // 회차 이벤트 무료 추가
                }
            }
        }

        /** 3. 전체 회차 idx, buyCoinPrice(결제 금액) 조회 **/
        episodeDto.setType(EPISODE_RENT); // 조회 전 set
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto);

        // 회차 전체 개수
        int epTotalCnt = originEpisodeList.size();

        // 무료 회차만 있는 경우
        if (epTotalCnt <= freeCnt + eventEpisodeFree) {
            throw new CustomException(CustomError.PURCHASE_RENT_NOT_EXIST); // 대여할 회차가 없습니다.
        }

        /** 무료 회차 idx 리스트 조회 **/
        episodeDto.setSort(epFreeMaxSort); // 무료 회차 순서 set
        List<Long> freeIdxList = episodeDaoSub.getFreeIdxListBySort(episodeDto);

        /** 전체 회차 리스트에서 무료 회차 리스트 제외 **/
        int originSize = originEpisodeList.size();

        for (long freeIdx : freeIdxList) {
            for (int j = 0; j < originSize; j++) {
                long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                if (epIdx == freeIdx) {
                    originEpisodeList.remove(j);
                    originSize--;
                    j--;
                }
            } // end of for
        } // end of for

        /** 4. 소장한 회차 episodeIdx 리스트 조회 (구매 or 대여) **/
        List<Long> purchaseIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto); // 무료 회차를 제외한 소장중인 idx 리스트

        // 보유중인 회차 수
        int memHaveCnt = purchaseIdxList != null ? purchaseIdxList.size() : 0; // 현재 대여 or 소장 중인 회차 개수

        // 유료 회차 개수
        int epPayCnt = epTotalCnt - epFreeMaxSort - eventEpisodeFree;

        // 이미 전체 회차를 소장하거나 대여중
        if (epPayCnt <= memHaveCnt) {
            throw new CustomException(CustomError.PURCHASE_ALREADY_ALL_BUY); // 대여할 회차가 없습니다.
        }

        /** 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) {
            int size = originEpisodeList.size();

            for (long haveIdx : purchaseIdxList) { // 보유중인 회차 idx

                for (int j = 0; j < size ; j++) {
                    long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                    // 소장중인 idx 와 같은 경우 해당 인덱스 제거
                    if (epIdx == haveIdx) {
                        originEpisodeList.remove(j);
                        size--;
                        j--;
                    }
                } // end of for
            } // end of for
        } // end of if

        /** 대여할 코인 총합 계산 **/
        int buyCoinPrice = getBuyCoinPrice(EPISODE_RENT, originEpisodeList, episodeEventList);

        // 렌트할 회차 개수
        int rentCnt = epPayCnt - memHaveCnt;   // 유료 회차 수 - 현재 보유중인 회차 수

        /** 전체 대여 할인율 조회  **/
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto); // 할인 정보 조회(minPurchaseCnt,discount)
        int minPurchaseRentCnt = contentFreeInfo.getMinPurchaseRentCnt(); // 할인 최소 개수

        /** 할인율 - returnValue **/
        int discountPercent = 0;  // 무료 포함한 할인율
        int discountPrice = 0;    // 할인 금액
        boolean isDiscount = false;

        // 할인 최소 개수 이상 구매 시 만 할인율 적용
        if (rentCnt >= minPurchaseRentCnt) {
            // 할인율 존재
            if (contentFreeInfo.getDiscountRent() != null && contentFreeInfo.getDiscountRent() > 0) {
                    isDiscount = true; // 할인 적용
                    discountPercent = contentFreeInfo.getDiscountRent(); // 할인율
                    int disCount = (int) (buyCoinPrice * discountPercent / 100.0); // 나머지 버림 처리
                    discountPrice = buyCoinPrice - disCount;    // 전체 금액(무료 표함) - 할인 금액
            }
        }

        String epTotalCntText = "";
        String rentCntText = "";
        Integer category = contentDaoSub.getContentCategory(contentIdx);

        // 텍스트 변환
        if (category == CATEGORY_COMIC) { // 만화
            epTotalCntText = epTotalCnt + "권";
            rentCntText = rentCnt + "권";

        } else { // 웹툰 OR 소설
            epTotalCntText = epTotalCnt + "화";
            rentCntText = rentCnt + "화";
        }

        JSONObject jsonData = new JSONObject();

        jsonData.put("totalNumber", epTotalCntText);      // 전체 회차 수(유료회차)
        jsonData.put("rentNumber", rentCntText);          // 대여할 회차 수
        jsonData.put("rentCoin", buyCoinPrice);           // 대여할 코인 총합
        jsonData.put("isDiscount", isDiscount);           // 할인 여부
        jsonData.put("discountPercent", discountPercent); // 할인율
        jsonData.put("discountPrice", discountPrice);     // 할인된 금액

        return jsonData;
    }

    /**
     * 전체 소장(무료 회차 포함) 정보
     *
     * @param contentIdx
     * @param memberIdx
     * @return
     */
    @Transactional(readOnly = true)
    public PopupInfoDto getFreePopupInfo(Integer contentIdx, Long memberIdx) {

        // 조회 위해 현재 시간 set
        PurchaseDto purchaseDto = PurchaseDto.builder()
                .contentsIdx(contentIdx)
                .memberIdx(memberIdx)
                .nowDate(dateLibrary.getDatetime()).build();

        // 조회용 dto
        EpisodeDto episodeDto = EpisodeDto.builder()
                .nowDate(dateLibrary.getDatetime())
                .contentIdx(contentIdx)
                .build();

        /** 1. 회차 이벤트 무료 조회 **/
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        /** 2. 전체 소장할 회차 idx, buyCoinPrice(결제 금액) 조회 **/
        episodeDto.setType(EPISODE_HAVE); // 조회 전 set
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto);

        // 전체 회차 개수(고정)
        int epTotalCnt = originEpisodeList.size();

        /** 3. 소장한 회차 idx 리스트 조회 (대여 항목 제외하고 조회) **/
        purchaseDto.setType(EPISODE_HAVE); // 소장 set
        List<Long> haveIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto);

        // 소장중인 회차 수
        int memHaveCnt = haveIdxList != null ? haveIdxList.size() : 0; // 현재 대여 중인 회차 개수

        /** 4. 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) {
            int size = originEpisodeList.size();
            for (long haveIdx : haveIdxList) { //소장중인 회차 idx

                for (int j = 0; j < size ; j++) {
                    long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                    // 소장중인 idx 와 같은 경우 해당 인덱스 제거
                    if (epIdx == haveIdx) {
                        originEpisodeList.remove(j);
                        size--;
                        j--;
                    }
                } // end of for
            } // end of for
        } // end of if

        /** 6. 결제할 총 금액 계산 **/
        int totalHaveCoin = getBuyCoinPrice(EPISODE_HAVE, originEpisodeList, episodeEventList);

        /** 7. 소장할 회차 수(무료 포함) - 소장한 회차 수 - 이벤트 무료 회차(0코인) **/
        int haveCnt = epTotalCnt - memHaveCnt;

        // 할인 정보 조회(minPurchaseCnt,discount)
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto);

        int minPurchaseCnt = contentFreeInfo.getMinPurchaseCnt(); // 할인 최소 개수

        /** 8. 할인율 - returnValue **/
        int discountPercent = 0; // 무료 포함한 할인율
        int disCountPrice = 0;   // 할인 금액
        boolean isDiscount = false;

        // 할인율이 존재
        if (contentFreeInfo.getDiscount() != null && contentFreeInfo.getDiscount() > 0) {

            // 할인 최소 개수 이상 구매 시만 할인율 적용
            // 무료 회차를 포함하는 회차가 할인율 적용 기준 이상
            if (epTotalCnt >= minPurchaseCnt) {
                isDiscount = true; // 할인 적용
                discountPercent = contentFreeInfo.getDiscount(); // 할인율
                int disCount = (int) (totalHaveCoin * discountPercent / 100.0); // 나머지 버림 처리
                disCountPrice = totalHaveCoin - disCount;    // 전체 금액(무료 표함) - 할인 금액
            }
        }

        String epTotalCntText = "";
        String haveCntText = "";
        Integer category = contentDaoSub.getContentCategory(contentIdx);

        // 텍스트 변환 
        if (category == CATEGORY_COMIC) { // 만화
            epTotalCntText = epTotalCnt + "권";
            haveCntText = haveCnt + "권";
        } else { // 웹툰 OR 소설
            epTotalCntText = epTotalCnt + "화";
            haveCntText = haveCnt + "화";

        }

        // 무료 회차 포함한 정보
        return PopupInfoDto.builder()
                .totalEpisodeCnt(epTotalCntText)     // 무료 회차를 포함한 전체 회차 수
                .haveCnt(haveCntText)                // 무료 회차를 포함한 대여할 회차 수
                .totalHaveCoin(totalHaveCoin)        // 무료 회차를 포함한 전체 소장할 코인
                .hasDiscount(isDiscount)             // 무료 회차를 포함한 경우 할인 여부
                .disCountPercent(discountPercent)    // 무료 회차를 포함한 할인율
                .disCountHaveCoin(disCountPrice)     // 무료 회차를 포함한 할인된 금액
                .build();
    }

    /**
     * 전체 소장(유료 회차만) 정보
     *
     * @param contentIdx
     * @param memberIdx
     * @return
     */
    @Transactional(readOnly = true)
    public PopupInfoDto getNonFreePopupInfo(Integer contentIdx, Long memberIdx) {

        // 조회 위해 현재 시간 set
        PurchaseDto purchaseDto = PurchaseDto.builder()
                .contentsIdx(contentIdx)
                .memberIdx(memberIdx)
                .nowDate(dateLibrary.getDatetime()).build();

        // 조회 위해 현재 시간 set
        purchaseDto.setNowDate(dateLibrary.getDatetime());

        // 조회용 dto
        EpisodeDto episodeDto = EpisodeDto.builder()
                .nowDate(dateLibrary.getDatetime())
                .contentIdx(contentIdx)
                .build();

        /** 1. 무료 회차 조회 **/
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

        /** 2. 이벤트 무료 회차 조회 **/
        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        // 무료 회차 최대값
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 회차 이벤트 리스트 조회
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        List<Long> episodeEventIdxList = null; // 회차 이벤트 무료 리스트
        int eventEpisodeFree = 0;

        if (episodeEventList != null) {
            for (EpisodeDto episodeEventDto : episodeEventList) {
                Integer eventCoinRent = episodeEventDto.getEventCoinRent();

                if (eventCoinRent != null && eventCoinRent == 0) {
                    eventEpisodeFree += 1; // 무료 회차 개수 증가
                    if (episodeEventIdxList == null) {
                        episodeEventIdxList = new ArrayList<>();
                    }
                    episodeEventIdxList.add(episodeEventDto.getIdx()); // 회차 이벤트 무료 추가
                }
            }
        }

        /** 3. 소장할 회차 idx, buyCoinPrice 조회 **/
        episodeDto.setType(EPISODE_HAVE);
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto);

        /** 4. 전체 회차 개수(전체 회차 - 무료 회차 - 이벤트 무료 회차(0코인 회차)) **/
        int epTotalCnt = originEpisodeList.size();

        /** 5. 무료 회차 idx 리스트 조회 **/
        episodeDto.setSort(epFreeMaxSort);
        List<Long> freeIdxList = episodeDaoSub.getFreeIdxListBySort(episodeDto);
        episodeDto.setSort(null);

        /** 6. 전체 회차 리스트에서 무료 회차 리스트 제외 **/
        int originSize = originEpisodeList.size();

        for (long freeIdx : freeIdxList) {
            for (int j = 0; j < originSize; j++) {
                long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                if (epIdx == freeIdx) {
                    originEpisodeList.remove(j);
                    originSize--;
                    j--;
                }
            } // end of for
        } // end of for


        /** 4. 소장한 회차 idx 리스트 조회 (대여 항목 제외) **/
        purchaseDto.setType(EPISODE_HAVE); // 소장 set
        List<Long> haveIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto); // 무료 회차를 제외한 소장중인 idx 리스트

        // 소장중인 회차 수
        int memHaveCnt = haveIdxList != null ? haveIdxList.size() : 0; // 현재 대여 중인 회차 개수

        /** 5. 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) { // 보유한 회차가 존재
            
            if (episodeEventIdxList != null) {
                int size = haveIdxList.size();

                for (long freeIdx : episodeEventIdxList) {
                    for (int j = 0; j < size; j++) {
                        long haveIdx = haveIdxList.get(j); // 전체 회차 중 idx

                        if (haveIdx == freeIdx) {
                            memHaveCnt = memHaveCnt - 1;
                            haveIdxList.remove(j);
                            size--;
                            j--;
                        }
                    } // end of for
                } // end of for
            } // end of if

            /** 소장중인 회차 전체 회차 리스트에서 제외 **/
            int size = originEpisodeList.size();
            
            for (long haveIdx : haveIdxList) { //소장중인 회차 idx
                for (int j = 0; j < size ; j++) {
                    long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                    // 소장중인 idx 와 같은 경우 해당 인덱스 제거
                    if (epIdx == haveIdx) {
                        originEpisodeList.remove(j);
                        size--;
                        j--;
                    }
                } // end of for
            } // end of for
        } // end of if

        /** 결제할 총 금액 계산 **/
        int totalHaveCoin = getBuyCoinPrice(EPISODE_HAVE, originEpisodeList, episodeEventList);

        // 유료 회차 개수(return value)
        int epPayCnt = epTotalCnt - epFreeMaxSort - eventEpisodeFree;

        /** 소장할 회차 수(무료 포함) - returnValue **/
        int haveCnt = epPayCnt - memHaveCnt; // 유료 회차 수 - 현재 소장중인 회차 수
        haveCnt = (haveCnt < 0 ? 0 : haveCnt);

        // 할인 정보 조회(minPurchaseCnt,discount)
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto);
        int minPurchaseCnt = contentFreeInfo.getMinPurchaseCnt(); // 할인 최소 개수

        /** 할인율 - returnValue **/
        int discountPercent = 0;  // 무료 포함한 할인율
        int disCountPrice = 0;    // 할인 금액
        boolean isDiscount = false;

        // 할인율 존재
        if (contentFreeInfo.getDiscount() != null && contentFreeInfo.getDiscount() > 0) {
            // 할인 최소 개수 이상 구매 시만 할인율 적용
            // 무료 회차를 포함하는 회차가 할인율 적용 기준 이상
            if (epTotalCnt >= minPurchaseCnt) {
                isDiscount = true; // 할인 적용
                discountPercent = contentFreeInfo.getDiscount(); // 할인율
                int disCount = (int) (totalHaveCoin * discountPercent / 100.0); // 나머지 버림 처리
                disCountPrice = totalHaveCoin - disCount;    // 전체 금액(무료 표함) - 할인 금액
            }
        }

        String epTotalCntText = "";
        String haveCntText = "";

        Integer category = contentDaoSub.getContentCategory(contentIdx);

        // 텍스트 변환
        if (category == CATEGORY_COMIC) { // 만화
            epTotalCntText = epPayCnt + "권";
            haveCntText = haveCnt + "권";
        } else { // 웹툰 OR 소설
            epTotalCntText = epPayCnt + "화";
            haveCntText = haveCnt + "화";
        }

        // 무료 회차 포함한 정보
        return PopupInfoDto.builder()
                .totalEpisodeCnt(epTotalCntText)     // 무료 회차를 미포함한 전체 회차 수
                .haveCnt(haveCntText)                // 무료 회차를 미포함한 대여할 회차 수
                .totalHaveCoin(totalHaveCoin)        // 무료 회차를 미포함한 전체 소장할 코인
                .hasDiscount(isDiscount)             // 무료 회차를 미포함한 경우 할인 여부
                .disCountPercent(discountPercent)    // 무료 회차를 미포함한 할인율
                .disCountHaveCoin(disCountPrice)     // 무료 회차를 미포함한 할인된 금액
                .build();

    }

    /**************************************************************************************
     * 회차 구매
     **************************************************************************************/

    /**
     * 회차 개별 구매
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), episodeIdx(회차 idx), memberIdx(회원 idx), type(구매 유형), nowDate(현재 시간), device(디바이스 정보)
     */
    @SneakyThrows
    @Transactional
    public void purchaseEpisode(PurchaseDto purchaseDto) {

        /** 회차 구매 유효성 검사 **/
        purchaseEpisodeValidate(purchaseDto);
        Long memberIdx = purchaseDto.getMemberIdx();
        Integer contentsIdx = purchaseDto.getContentsIdx();

        /** 1. 코인 차감 알림 유무 **/
        Boolean isNotSendAlarm = purchaseDto.getIsNotSendAlarm();

        // 코인 차감 알림 끔 -> [다음 구매 시 물어보지 않기] 체크 시
        if (isNotSendAlarm != null && isNotSendAlarm == Boolean.TRUE) {

            // 회원 알림 정보 조회
            MemberDto memberDto = memberDaoSub.getSettingInfo(memberIdx);
            Integer coinAlarm = memberDto.getCoinAlarm();

            // 현재 코인 차감 알림 받고 있는 상태일 경우
            if (coinAlarm != null && coinAlarm == 0) {

                memberDto.setCoinAlarm(1);  // 알림 받지 않음 상태 set
                memberDto.setIdx(memberIdx);// memberIdx set

                // 코인 알림 받지 않음으로 환경설정 값 변경
                memberDao.modifyCoinAlarm(memberDto);

                // 회원 환경설정 정보 DB에서 조회
                MemberDto memberSetting = memberDaoSub.getSettingInfo(memberDto.getIdx());

                // 회원 환경설정 정보 Session 반영
                ObjectMapper objectMapper = new ObjectMapper();
                session.setAttribute(SessionConfig.MEMBER_SETTING, objectMapper.writeValueAsString(memberSetting));
            }
        }

        int buyCoinPrice = 0; // 결제할 금액
        Integer eventCoin;    // 회차 이벤트 금액

        /** 2. 구매할 회차 원가격 및 제목 조회 (coin, coin_rent, title) **/
        CoinDto coinDto = coinDaoSub.getEpisodeInfo(purchaseDto);
        purchaseDto.setTitle(coinDto.getTitle()); // 회차 제목 set

        /** 3. 회차 이벤트 가격 조회(null 인 경우 이벤트 중 아님) **/
        EpisodeDto episodeDto = EpisodeDto.builder()
                .idx(purchaseDto.getEpisodeIdx())
                .nowDate(dateLibrary.getDatetime())
                .build(); // 이베튼 가격 조회용 dto

        // 소장(회차 이벤트 가격)
        if (purchaseDto.getType() == EPISODE_HAVE) {
            eventCoin = episodeDaoSub.getEpisodeEventCoin(episodeDto);     // 소장 이벤트 가격

            // 대여(회차 이벤트 가격)
        } else {
            eventCoin = episodeDaoSub.getEpisodeEventRentCoin(episodeDto); // 대여 이벤트 가격
        }

        /** 회차 가격 이벤트 중 아님(원가격 setting) **/
        if (eventCoin == null) {
            if (purchaseDto.getType() == EPISODE_HAVE) { // 소장
                // 소장 가격 set
                buyCoinPrice = coinDto.getCoin();

            } else if (purchaseDto.getType() == EPISODE_RENT) { // 대여
                // 대여 가격 set
                buyCoinPrice = coinDto.getCoinRent();
            }
            purchaseDto.setBuyCoinPrice(buyCoinPrice); // 결제 금액 set

            /** 회차 가격 이벤트 중(할인 가격 setting) **/
        } else if (eventCoin > 0) {
            buyCoinPrice = eventCoin; // 결제할 금액 set
            purchaseDto.setBuyCoinPrice(buyCoinPrice); // 결제 금액 set

        } else if (eventCoin == 0) {
            throw new CustomException(CustomError.PURCHASE_CANT_BUY_EVENT);  // 본 회차는 이벤트로 인해 무료이므로 구매가 불가합니다.
        } // end of 결제할 가격 세팅

        /** 4. 회원 보유 선물(이용권) 조회 (restTicket) **/
        Integer restTicket = 0;
        // 소장이 아닐때만 이용권 사용
        if (purchaseDto.getType() != EPISODE_HAVE) {
            SearchDto searchDto = new SearchDto();
            searchDto.setMemberIdx(memberIdx);              // 회원 idx
            searchDto.setContentsIdx(contentsIdx);          // 작품 idx
            searchDto.setNowDate(dateLibrary.getDatetime());// 현재 시간
            restTicket = giftDaoSub.getMemberGiftCnt(searchDto);
        }

        /** 5. 회원 보유 코인 조회 (restCoin, restCoinFree, restMileage) **/
        CoinDto restCoinDto = coinService.getRestCoinAndRestMileage(memberIdx); // 잔여 코인 & 보너스 코인 & 마일리지 조회
        restCoinDto.setRestTicket(restTicket); // 잔여 이용권 추가 세팅

        /** 만료된 코인 & 마일리지 update (member_coin_used & member_mileage_used 상태값 0으로) **/
        coinService.updateExpireCoinAndMileage(memberIdx);

        /** 6. 회차 구매 가능한지 검사 (결제할 코인이 부족하면 exception) **/
        possibleBuyValidate(restCoinDto, buyCoinPrice);

        /** 7. 코인 차감 로직 (사용한 코인 리턴) **/
        CoinDto usedCoinDto = subtractCoin(purchaseDto, restCoinDto); // 회원 남은 코인

        /** 8. 페이백 마일리지 지급 (구매 시 소진한 (코인 + 마일리지) x 5% + 등급별 추가 % 지급) **/
        int payBackMileage = 0;

        // 작품 구매 시 소진한 코인 또는 마일리지가 있을 때
        if (usedCoinDto.getUsedCoin() > 0 || usedCoinDto.getUsedMileage() > 0) {

            // 회원 등급 조회
            Integer memberGrade = gradeService.getMemberGradeLevel(memberIdx);

            // 페이백 마일리지 계산
            payBackMileage = getPayBackMileage(usedCoinDto.getUsedCoin(), usedCoinDto.getUsedMileage(), memberGrade);

            // 작품 제목 조회
            String title = contentDaoSub.getContentsTitleByIdx(contentsIdx);

            // dto set
            CoinDto mileageDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .paymentIdx(0)
                    .achievementIdx(0L)
                    .mileage(payBackMileage)
                    .restMileage(payBackMileage)
                    .position("페이백")
                    .title("[" + title + "]" + " 감상 페이백")
                    .state(1)
                    .regdate(dateLibrary.getDatetime())
                    .mileageExpireDate(getCoinExpireDate(3))
                    .build();

            // member_mileage_save 테이블 insert
            coinDao.insertMileageSave(mileageDto);

            // member_mileage_save_log 테이블 insert
            coinDao.insertMileageSaveLog(mileageDto);

            // member_mileage_used 테이블 insert
            coinDao.insertMileageUsed(mileageDto);

            // member_mileage_used_log 테이블 insert
            coinDao.insertMileageUsedLog(mileageDto);
        }

        /** 9. 보유 금액 - 사용 금액 + 페이백 금액 **/
        int restCoin = restCoinDto.getRestCoin() - usedCoinDto.getUsedCoin();
        int restCoinFree = restCoinDto.getRestCoinFree() - usedCoinDto.getUsedCoinFree();
        int restMileage = restCoinDto.getRestMileage() - usedCoinDto.getUsedMileage() + payBackMileage;

        // 결제 후 남은 보유 금액 정보
        CoinDto memberRestCoinDto = CoinDto.builder()
                .coin(restCoin)
                .coinFree(restCoinFree)
                .mileage(restMileage)
                .memberIdx(memberIdx)
                .build();

        /** 10. 회원 보유 금액 update **/
        int updateResult = coinDao.updateMemberCoin(memberRestCoinDto);
        if (updateResult < 1) {
            throw new CustomException(CustomError.PURCHASE_MEMBER_COIN_UPDATE_FAIL); // 회차구매에 실패하였습니다.
        }

        // 등록일(구매일) set
        purchaseDto.setRegdate(dateLibrary.getDatetime());

        /** 구매 유형 판단 -> 만료일 세팅 **/
        if (purchaseDto.getType() == EPISODE_RENT) {

            // 사용한 이용권이 있는 경우
            if (usedCoinDto.getPeriod() != null && usedCoinDto.getPeriod() > 0) {
                if (usedCoinDto.getUsedTicket() != null && usedCoinDto.getUsedTicket() > 0) {
                    // 이용권 대여 만료일 set
                    purchaseDto.setExpiredate(dateLibrary.getTicketExpireDate(usedCoinDto.getPeriod()));
                }
            } else {
                // 대여 만료일 set (개별 회차 구매는 구매일로부터 +1일)
                purchaseDto.setExpiredate(dateLibrary.getExpireDate(purchaseDto.getRegdate()));
            }
        } else {
            purchaseDto.setExpiredate(getExpireDate(EPISODE_HAVE));
        }

        /** 11. member_purchase(구매 회차) 등록 **/
        purchaseDto.setCoin(usedCoinDto.getUsedCoin());           // 사용한 코인 set
        purchaseDto.setCoinFree(usedCoinDto.getUsedCoinFree());   // 사용한 보너스 코인 set
        purchaseDto.setMileage(usedCoinDto.getUsedMileage());     // 사용한 마일리지 set
        purchaseDto.setUsedTicket(usedCoinDto.getUsedTicket());   // 사용한 이용권 set
        purchaseDao.insertMemberPurchase(purchaseDto);

        /** 12. contents_info 업데이트 (view, purchase) **/
        ContentDto contentInfo = contentDaoSub.getContentInfo(contentsIdx);
        contentInfo.setPurchase(contentInfo.getPurchase() + 1);
        contentInfo.setView(contentInfo.getView() + 1);
        contentDao.updateViewCntAndPurchase(contentInfo);
    }

    /**
     * 전체 대여
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void rentAllEpisode(PurchaseDto purchaseDto) {

        /** 컨텐츠 idx 유효성 검사 **/
        contentIdxValidate(purchaseDto.getContentsIdx());
        routeValidate(purchaseDto.getRoute());

        purchaseDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set
        Long memberIdx = purchaseDto.getMemberIdx();       // 회원 idx
        Integer contentIdx = purchaseDto.getContentsIdx(); // 컨텐츠 idx

        EpisodeDto episodeDto = EpisodeDto.builder()
                .contentIdx(contentIdx)                 // 컨텐츠 idx set
                .nowDate(dateLibrary.getDatetime())     // 현재 시간 set
                .build();

        /** 1. 무료 회차 조회 **/
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

        /** 2. 이벤트 무료 회차 조회 **/
        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        // 무료 회차 최대값
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        /** 3. 이벤트 회차 조회 (idx, eventCoin, eventCoinRent) **/
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        List<Long> episodeEventIdxList = null; // 회차 이벤트 무료 리스트
        int eventEpisodeFree = 0;              // 회차 이벤트 무료 개수

        /** 4. 이벤트 무료 회차 (0코인) 개수 카운트 및 리스트 추가 **/
        if (episodeEventList != null) {

            for (EpisodeDto episodeEventDto : episodeEventList) {
                Integer eventCoinRent = episodeEventDto.getEventCoinRent();

                if (eventCoinRent != null && eventCoinRent == 0) {
                    eventEpisodeFree += 1; // 회차 이벤트 무료 개수 증가

                    if (episodeEventIdxList == null) {
                        episodeEventIdxList = new ArrayList<>();
                    }
                    episodeEventIdxList.add(episodeEventDto.getIdx()); // 회차 이벤트 무료 추가
                }
            } // end of for
        } // end of if

        /** 5. 대여할 회차 코인 정보(idx, buyCoinPrice(회차 대여 금액)) **/
        episodeDto.setType(EPISODE_RENT); // 대여 금액 조회
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto); // 대여할 회차 idx, 코인 정보

        // 전체 회차 개수
        int epTotalCnt = originEpisodeList.size();
        epTotalCnt = epTotalCnt - epFreeMaxSort - eventEpisodeFree;

        /** 6. 무료 회차 idx 리스트 조회 **/
        episodeDto.setSort(epFreeMaxSort);
        List<Long> freeIdxList = episodeDaoSub.getFreeIdxListBySort(episodeDto);

        /** 7. 전체 회차 리스트에서 무료 회차 리스트 제외 **/
        int originSize = originEpisodeList.size();

        for (long freeIdx : freeIdxList) {
            for (int j = 0; j < originSize; j++) {
                long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                if (epIdx == freeIdx) {
                    originEpisodeList.remove(j);
                    originSize--;
                    j--;
                }
            } // end of for
        } // end of for

        /** 8. 보유중인 회차 idx 리스트 조회 **/
        List<Long> haveIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto); // 무료 회차를 제외한 소장중인 idx 리스트

        // 보유중인 회차 수
        int memHaveCnt = haveIdxList != null ? haveIdxList.size() : 0; // 현재 대여 중인 회차 개수

        /** 9. 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) { // 보유한 회차가 존재

            /** 회차 이벤트 중인 것 중 0코인 회차 idx 소장 리스트에서 제외(중복 방지) **/
            if (episodeEventIdxList != null) {
                int size = haveIdxList.size();

                for (long freeIdx : episodeEventIdxList) {
                    for (int j = 0; j < size; j++) {
                        long haveIdx = haveIdxList.get(j); // 소장 회차 중 idx

                        if (haveIdx == freeIdx) {
                            memHaveCnt = memHaveCnt - 1;
                            haveIdxList.remove(j);
                            size--;
                            j--;
                        } // end of if
                    } // end of for
                } // end of for
            }

            /** 기본 무료 회차 소장 리스트에서 제외 **/
            if (freeIdxList != null) {
                int size = haveIdxList.size();

                for (long freeIdx : freeIdxList) {
                    for (int j = 0; j < size; j++) {
                        long haveIdx = haveIdxList.get(j); // 소장 회차 중 idx

                        if (haveIdx == freeIdx) {
                            memHaveCnt = memHaveCnt - 1;
                            haveIdxList.remove(j);
                            size--;
                            j--;
                        } // end of if
                    } // end of for
                } // end of for
            }
        } // end of if

        // 대여할 회차 수
        int totalBuyCnt = epTotalCnt - memHaveCnt;

        // 이미 전체 회차를 소장하거나 대여중
        if (totalBuyCnt <= memHaveCnt) {
            throw new CustomException(CustomError.PURCHASE_ALREADY_ALL_BUY); // 보유할 회차가 없습니다.
        }

        /** 10. 전체 대여할 코인 총합 조회(이벤트 중일경우 originEpisodeList의 buyCoinPrice 값 바껴서 리턴) **/
        int buyCoinPrice = getBuyCoinPrice(EPISODE_RENT, originEpisodeList, episodeEventList); // 결제할 총 금액

        /** 11. 전체 대여 할인율 조회  **/
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto); // 할인 정보 조회(minPurchaseCnt,discount)
        int minPurchaseRentCnt = contentFreeInfo.getMinPurchaseRentCnt(); // 할인 최소 개수

        // 할인 최소 개수 이상 구매 시 만 할인율 적용
        if (totalBuyCnt >= minPurchaseRentCnt) {
            int discountPercent = contentFreeInfo.getDiscountRent();            // 할인율
            int disCountPrice = (int) (buyCoinPrice * discountPercent / 100.0); // 할인 금액
            buyCoinPrice = buyCoinPrice - disCountPrice;                        // 결제할 금액 - 할인 금액 (할인된 금액)
        }

        /** 전체 대여할 결제 금액 set **/
        purchaseDto.setBuyCoinPrice(buyCoinPrice);

        // 컨텐츠 제목 조회
        String title = contentDaoSub.getContentsTitleByIdx(contentIdx);
        title += " " + super.langMessage("lang.purchase.all.rent");
        purchaseDto.setTitle(title); // 컨텐츠 제목 set

        /** 12. 회원 보유 코인 조회 (restCoin, restCoinFree, restMileage) **/
        CoinDto restCoinDto = coinService.getRestCoinAndRestMileage(memberIdx); // 잔여 코인 & 보너스 코인 & 마일리지 조회

        /** 만료된 코인 & 마일리지 update (member_coin_used & member_mileage_used 상태값 0으로) **/
        coinService.updateExpireCoinAndMileage(memberIdx);

        /** 13. 회차 구매 가능한지 검사 (결제할 코인이 부족하면 exception) **/
        possibleBuyValidate(restCoinDto, buyCoinPrice);

        /** 14. 코인 차감 로직 (사용한 코인 리턴) **/
        CoinDto usedCoinDto = subtractCoin(purchaseDto, restCoinDto);

        /** 15. 페이백 마일리지 지급 (구매 시 소진한 (코인 + 마일리지) x 5% + 등급별 추가 % 지급) **/
        int payBackMileage = 0;

        // 작품 구매 시 소진한 코인 또는 마일리지가 있을 때
        if (usedCoinDto.getUsedCoin() > 0 || usedCoinDto.getUsedMileage() > 0) {

            // 회원 등급 조회
            Integer memberGrade = gradeService.getMemberGradeLevel(memberIdx);

            // 페이백 마일리지 계산
            payBackMileage = getPayBackMileage(usedCoinDto.getUsedCoin(), usedCoinDto.getUsedMileage(), memberGrade);

            // dto set
            CoinDto mileageDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .paymentIdx(0)
                    .achievementIdx(0L)
                    .mileage(payBackMileage)
                    .restMileage(payBackMileage)
                    .position("페이백")
                    .title("[" + title + "]" + " 감상 페이백")
                    .state(1)
                    .regdate(dateLibrary.getDatetime())
                    .mileageExpireDate(getCoinExpireDate(3))
                    .build();

            // member_mileage_save 테이블 insert
            coinDao.insertMileageSave(mileageDto);

            // member_mileage_save_log 테이블 insert
            coinDao.insertMileageSaveLog(mileageDto);

            // member_mileage_used 테이블 insert
            coinDao.insertMileageUsed(mileageDto);

            // member_mileage_used_log 테이블 insert
            coinDao.insertMileageUsedLog(mileageDto);
        }

        /** 16. 보유 금액 - 사용 금액 **/
        int restCoin = restCoinDto.getRestCoin() - usedCoinDto.getUsedCoin();
        int restCoinFree = restCoinDto.getRestCoinFree() - usedCoinDto.getUsedCoinFree();
        int restMileage = restCoinDto.getRestMileage() - usedCoinDto.getUsedMileage() + payBackMileage;

        // 결제 후 남은 보유코인 정보
        CoinDto memberRestCoinDto = CoinDto.builder()
                .coin(restCoin)
                .coinFree(restCoinFree)
                .mileage(restMileage)
                .memberIdx(memberIdx)
                .build();

        /** 17. 회원 보유 금액 update **/
        int updateResult = coinDao.updateMemberCoin(memberRestCoinDto);
        if (updateResult < 1) {
            throw new CustomException(CustomError.PURCHASE_MEMBER_COIN_UPDATE_FAIL); // 회차구매에 실패하였습니다.
        }

        // 회원 전체 구매 등록에 사용할 dto
        PurchaseBuyAllDto purchaseBuyAllDto = PurchaseBuyAllDto.builder()
                .memberIdx(memberIdx)
                .contentsIdx(contentIdx)
                .totalCoin(buyCoinPrice)
                .episodeCount(totalBuyCnt)
                .type(EPISODE_RENT)
                .regdate(dateLibrary.getDatetime())
                .expiredate(getExpireDate(EPISODE_RENT))
                .build();

        // 회원 전체 구매 등록
        purchaseDao.insertMemberBuyAll(purchaseBuyAllDto);

        if (purchaseBuyAllDto.getInsertedIdx() == null) {
            throw new CustomException(CustomError.PURCHASE_EPISODE_BUY_FAIL); // 회차 구매에 실패하였습니다.
        }

        /** 18. member_purchase 등록 **/
        List<PurchaseDto> memberPurchaseList = new ArrayList<>();
        purchaseDto.setType(EPISODE_RENT); // 대여 set
        purchaseDto.setExpiredate(getExpireDate(EPISODE_RENT));
        purchaseDto.setRegdate(dateLibrary.getDatetime());
        purchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());

        for (EpisodeDto episodeRentDto : originEpisodeList) {
            PurchaseDto memberPurchaseDto = null;
            // 결제 금액이 남아 있음
            if (buyCoinPrice > 0) {
                memberPurchaseDto = coinService.purchaseCoinCalculate(episodeRentDto, usedCoinDto, purchaseDto);
                // 결제 금액
            } else {
                memberPurchaseDto = PurchaseDto.builder()
                        .memberIdx(memberIdx)
                        .contentsIdx(purchaseDto.getContentsIdx())
                        .episodeIdx(episodeRentDto.getIdx())
                        .coin(0)
                        .coinFree(0)
                        .mileage(0)
                        .usedTicket(0)
                        .type(purchaseDto.getType())
                        .title(purchaseDto.getTitle())
                        .route(purchaseDto.getRoute())
                        .buyAllIdx(purchaseDto.getBuyAllIdx())
                        .expiredate(purchaseDto.getExpiredate())
                        .regdate(purchaseDto.getRegdate())
                        .build();
            }
            // buyAllIdx set
            memberPurchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());
            memberPurchaseList.add(memberPurchaseDto);
        } // end of for

        purchaseDao.insertMemberPurchaseList(memberPurchaseList);

        /** 19. contents_info 업데이트 (view, purchase)**/
        ContentDto contentInfo = contentDaoSub.getContentInfo(contentIdx);
        contentInfo.setPurchase(contentInfo.getPurchase() + totalBuyCnt);
        contentInfo.setView(contentInfo.getView() + totalBuyCnt);
        contentDao.updateViewCntAndPurchase(contentInfo);
    }

    /**
     * 전체 소장 (무료 회차 포함)
     *
     * @param purchaseDto : isIncludeFree(무료 회차 포함 여부), contentsIdx(컨텐츠 회차), memberIdx(회원 idx), route(구매 경로)
     */
    @Transactional
    public void haveAllIncludeFreeEpisode(PurchaseDto purchaseDto) {

        /** 유효성 검사**/
        haveAllIncludeFreeEpisodeValidate(purchaseDto);

        purchaseDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set
        Long memberIdx = purchaseDto.getMemberIdx();       // 회원 idx
        Integer contentIdx = purchaseDto.getContentsIdx(); // 컨텐츠 idx


        EpisodeDto episodeDto = EpisodeDto.builder()
                .contentIdx(contentIdx)                 // 컨텐츠 idx set
                .nowDate(dateLibrary.getDatetime())     // 현재 시간 set
                .build();

        /** 1. 회차 이벤트 무료 조회 **/
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        /** 2. 전체 소장할 회차 idx, buyCoinPrice(결제 금액) 조회 **/
        episodeDto.setType(EPISODE_HAVE); // 조회 전 set
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto);

        // 전체 회차 개수(고정)
        int epTotalCnt = originEpisodeList.size();

        /** 3. 소장한 회차 idx 리스트 조회 (대여 항목 제외하고 조회) **/
        purchaseDto.setType(EPISODE_HAVE); // 소장 set
        List<Long> haveIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto);
        
        // 소장중인 회차 수
        int memHaveCnt = haveIdxList != null ? haveIdxList.size() : 0; // 현재 대여 중인 회차 개수

        /** 소장할 회차 수(무료 포함) - 소장한 회차 수 = 전체 소장할 회차 개수(최종) **/
        int totalBuyCnt = epTotalCnt - memHaveCnt;

        // 이미 전체 회차를 소장하거나 대여중
        if (totalBuyCnt <= memHaveCnt) {
            throw new CustomException(CustomError.PURCHASE_ALREADY_ALL_BUY); // 보유할 회차가 없습니다.
        }

        /** 4. 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) {
            int size = originEpisodeList.size();
            for (long haveIdx : haveIdxList) { //소장중인 회차 idx

                for (int j = 0; j < size ; j++) {
                    long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                    // 소장중인 idx 와 같은 경우 해당 인덱스 제거
                    if (epIdx == haveIdx) {
                        originEpisodeList.remove(j);
                        size--;
                        j--;
                    }
                } // end of for
            } // end of for
        } // end of if

        /** 5. 결제할 총 금액 계산 **/
        int buyCoinPrice = getBuyCoinPrice(EPISODE_HAVE, originEpisodeList, episodeEventList);

        /** 6. 전체 소장 할인율 조회  **/
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto); // 할인 정보 조회(minPurchaseCnt,discount)
        int minPurchaseCnt = contentFreeInfo.getMinPurchaseCnt(); // 할인 최소 개수

        // 할인 최소 개수 이상 구매 시만 할인율 적용
        // 무료 회차를 포함하는 회차가 할인율 적용 기준 이상
        if (totalBuyCnt >= minPurchaseCnt) {
            int discountPercent = contentFreeInfo.getDiscount();                // 할인율
            int disCountPrice = (int) (buyCoinPrice * discountPercent / 100.0); // 할인 금액
            buyCoinPrice = buyCoinPrice - disCountPrice;                        // 결제할 금액 - 할인 금액 (할인된 금액)
        }

        /** 최종 소장할 결제 금액 set **/
        purchaseDto.setBuyCoinPrice(buyCoinPrice);

        // 컨텐츠 제목 조회
        String title = contentDaoSub.getContentsTitleByIdx(contentIdx);
        title += " " + super.langMessage("lang.purchase.all.have");
        purchaseDto.setTitle(title); // 컨텐츠 제목 set

        /** 7. 회원 보유 코인 조회 (restCoin, restCoinFree, restMileage) **/
        CoinDto restCoinDto = coinService.getRestCoinAndRestMileage(memberIdx); // 잔여 코인 & 보너스 코인 & 마일리지 조회

        /** 만료된 코인 & 마일리지 update (member_coin_used & member_mileage_used 상태값 0으로) **/
        coinService.updateExpireCoinAndMileage(memberIdx);

        /** 8. 회차 구매 가능한지 검사 (결제할 코인이 부족하면 exception) **/
        possibleBuyValidate(restCoinDto, buyCoinPrice);

        /** 9. 코인 차감 로직 (사용한 코인 리턴) **/
        CoinDto usedCoinDto = subtractCoin(purchaseDto, restCoinDto); // 사용 코인

        /** 10. 페이백 마일리지 지급 (구매 시 소진한 (코인 + 마일리지) x 5% + 등급별 추가 % 지급) **/
        int payBackMileage = 0;

        // 작품 구매 시 소진한 코인 또는 마일리지가 있을 때
        if (usedCoinDto.getUsedCoin() > 0 || usedCoinDto.getUsedMileage() > 0) {

            // 회원 등급 조회
            Integer memberGrade = gradeService.getMemberGradeLevel(memberIdx);

            // 페이백 마일리지 계산
            payBackMileage = getPayBackMileage(usedCoinDto.getUsedCoin(), usedCoinDto.getUsedMileage(), memberGrade);

            // dto set
            CoinDto mileageDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .paymentIdx(0)
                    .achievementIdx(0L)
                    .mileage(payBackMileage)
                    .restMileage(payBackMileage)
                    .position("페이백")
                    .title("[" + title + "]" + " 감상 페이백")
                    .state(1)
                    .regdate(dateLibrary.getDatetime())
                    .mileageExpireDate(getCoinExpireDate(3))
                    .build();

            // member_mileage_save 테이블 insert
            coinDao.insertMileageSave(mileageDto);

            // member_mileage_save_log 테이블 insert
            coinDao.insertMileageSaveLog(mileageDto);

            // member_mileage_used 테이블 insert
            coinDao.insertMileageUsed(mileageDto);

            // member_mileage_used_log 테이블 insert
            coinDao.insertMileageUsedLog(mileageDto);
        }

        /** 11. 보유 코인 - 사용 코인 **/
        int restCoin = restCoinDto.getRestCoin() - usedCoinDto.getUsedCoin();
        int restCoinFree = restCoinDto.getRestCoinFree() - usedCoinDto.getUsedCoinFree();
        int restMileage = restCoinDto.getRestMileage() - usedCoinDto.getUsedMileage() + payBackMileage;

        // 결제 후 남은 보유 금액 정보
        CoinDto memberRestCoinDto = CoinDto.builder()
                .coin(restCoin)
                .coinFree(restCoinFree)
                .mileage(restMileage)
                .memberIdx(memberIdx)
                .build();

        /** 12. 회원 보유 코인 update **/
        int updateResult = coinDao.updateMemberCoin(memberRestCoinDto);
        if (updateResult < 1) {
            throw new CustomException(CustomError.PURCHASE_MEMBER_COIN_UPDATE_FAIL); // 회차구매에 실패하였습니다.
        }

        // 회원 전체 구매 등록에 사용할 dto
        PurchaseBuyAllDto purchaseBuyAllDto = PurchaseBuyAllDto.builder()
                .memberIdx(memberIdx)
                .contentsIdx(contentIdx)
                .totalCoin(buyCoinPrice)
                .episodeCount(totalBuyCnt)
                .type(EPISODE_HAVE)
                .regdate(dateLibrary.getDatetime())
                .expiredate(getExpireDate(EPISODE_HAVE))
                .build();

        // 회원 전체 구매 등록
        purchaseDao.insertMemberBuyAll(purchaseBuyAllDto);

        if (purchaseBuyAllDto.getInsertedIdx() == null) {
            throw new CustomException(CustomError.PURCHASE_EPISODE_BUY_FAIL); // 회차 구매에 실패하였습니다.
        }

        /** 13. member_purchase 등록 **/
        List<PurchaseDto> memberPurchaseList = new ArrayList<>();
        purchaseDto.setType(EPISODE_HAVE); // 대여 set
        purchaseDto.setExpiredate(getExpireDate(EPISODE_HAVE));
        purchaseDto.setRegdate(dateLibrary.getDatetime());
        purchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());

        for (EpisodeDto episodeHaveDto : originEpisodeList) {
            // member_purchase 에 등록한 dto 객체
            PurchaseDto memberPurchaseDto = null;
            // purchaseCoinCalculate 메서드 돌면서 결제 금액 계속 감소
            buyCoinPrice = purchaseDto.getBuyCoinPrice();

            // 결제 금액이 남아 있음
            if (buyCoinPrice > 0) {
                memberPurchaseDto = coinService.purchaseCoinCalculate(episodeHaveDto, usedCoinDto, purchaseDto);

                // 결제 금액 소멸 이후 사용 금액 0
            } else {
                memberPurchaseDto = PurchaseDto.builder()
                        .memberIdx(memberIdx)
                        .contentsIdx(purchaseDto.getContentsIdx())
                        .episodeIdx(episodeHaveDto.getIdx())
                        .coin(0)
                        .coinFree(0)
                        .mileage(0)
                        .usedTicket(0)
                        .type(purchaseDto.getType())
                        .title(purchaseDto.getTitle())
                        .buyAllIdx(purchaseDto.getBuyAllIdx())
                        .expiredate(purchaseDto.getExpiredate())
                        .regdate(purchaseDto.getRegdate())
                        .build();
            }
            // buyAllIdx set
            memberPurchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());
            memberPurchaseList.add(memberPurchaseDto);
        } // end of for

        // member_purchase 등록
        purchaseDao.insertMemberPurchaseList(memberPurchaseList);

        /** 14. contents_info 업데이트 (view, purchase)**/
        ContentDto contentInfo = contentDaoSub.getContentInfo(contentIdx);
        contentInfo.setPurchase(contentInfo.getPurchase() + totalBuyCnt);
        contentInfo.setView(contentInfo.getView() + totalBuyCnt);
        contentDao.updateViewCntAndPurchase(contentInfo);
    }

    /**
     * 전체 소장 (유료 회차만)
     *
     * @param purchaseDto : isIncludeFree(무료 회차 포함 여부), contentsIdx(컨텐츠 회차), memberIdx(회원 idx), route(구매 경로)
     */
    @Transactional
    public void haveAllOnlyPaidEpisode(PurchaseDto purchaseDto) {

        /** 유효성 검사**/
        haveAllOnlyPaidEpisodeValidate(purchaseDto);

        purchaseDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set
        Long memberIdx = purchaseDto.getMemberIdx();       // 회원 idx
        Integer contentIdx = purchaseDto.getContentsIdx(); // 컨텐츠 idx


        EpisodeDto episodeDto = EpisodeDto.builder()
                .contentIdx(contentIdx)                 // 컨텐츠 idx set
                .nowDate(dateLibrary.getDatetime())     // 현재 시간 set
                .build();

        /** 1. 무료 회차 조회 **/
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

        /** 2. 이벤트 무료 회차 조회 **/
        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        // 무료 회차 최대값
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 회차 이벤트 리스트 조회
        List<EpisodeDto> episodeEventList = episodeDaoSub.getEpisodeEvent(episodeDto);

        List<Long> episodeEventIdxList = null; // 회차 이벤트 무료 리스트
        int eventEpisodeFree = 0;              // 회차 이벤트 무료(0코인) 개수

        /** 이벤트 무료 회차 (0코인) 개수 카운트 및 리스트 추가 **/
        if (episodeEventList != null) {
            for (EpisodeDto episodeEventDto : episodeEventList) {
                Integer eventCoin = episodeEventDto.getEventCoin();

                if (eventCoin != null && eventCoin == 0) {
                    freeCnt += 1; // 무료 회차 개수 증가

                    if (episodeEventIdxList == null) {
                        episodeEventIdxList = new ArrayList<>();
                    }
                    episodeEventIdxList.add(episodeEventDto.getIdx()); // 회차 이벤트 무료 추가
                }
            }
        }

        /** 3. 소장할 회차 idx, buyCoinPrice 조회 **/
        episodeDto.setType(EPISODE_HAVE);
        List<EpisodeDto> originEpisodeList = coinDaoSub.getAllEpisodeIdxAndCoin(episodeDto);

        /** 4. 전체 회차 개수(전체 회차 - 무료 회차 - 이벤트 무료 회차(0코인 회차)) **/
        int epTotalCnt = originEpisodeList.size();
        epTotalCnt = epTotalCnt - epFreeMaxSort - eventEpisodeFree;

        /** 5. 무료 회차 idx 리스트 조회 **/
        episodeDto.setSort(epFreeMaxSort);
        List<Long> freeIdxList = episodeDaoSub.getFreeIdxListBySort(episodeDto);
        episodeDto.setSort(null);

        /** 6. 전체 회차 리스트에서 무료 회차 리스트 제외 **/
        int originSize = originEpisodeList.size();

        for (long freeIdx : freeIdxList) {
            for (int j = 0; j < originSize; j++) {
                long epIdx = originEpisodeList.get(j).getIdx(); // 전체 회차 중 idx

                if (epIdx == freeIdx) {
                    originEpisodeList.remove(j);
                    originSize--;
                    j--;
                }
            } // end of for
        } // end of for


        /** 7. 소장한 회차 idx 리스트 조회 (대여 항목 제외) **/
        purchaseDto.setType(EPISODE_HAVE); // 소장 set
        List<Long> haveIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto); // 무료 회차를 제외한 소장중인 idx 리스트

        // 소장중인 회차 수
        int memHaveCnt = haveIdxList != null ? haveIdxList.size() : 0; // 현재 소장 중인 회차 개수

        /** 5. 전체 회차 리스트에서 소장중인 회차 제외 **/
        if (memHaveCnt > 0) { // 보유한 회차가 존재

            /** 회차 이벤트 중인 것 중 0코인 회차 idx 소장 리스트에서 제외(중복 방지) **/
            if (episodeEventIdxList != null) {
                int size = haveIdxList.size();

                for (long freeIdx : episodeEventIdxList) {
                    for (int j = 0; j < size; j++) {
                        long haveIdx = haveIdxList.get(j); // 소장 회차 중 idx

                        if (haveIdx == freeIdx) {
                            memHaveCnt = memHaveCnt - 1;
                            haveIdxList.remove(j);
                            size--;
                            j--;
                        } // end of if
                    } // end of for
                } // end of for
            }

            /** 기본 무료 회차 소장 리스트에서 제외 **/
            if (freeIdxList != null) {
                int size = haveIdxList.size();

                for (long freeIdx : freeIdxList) {
                    for (int j = 0; j < size; j++) {
                        long haveIdx = haveIdxList.get(j); // 소장 회차 중 idx

                        if (haveIdx == freeIdx) {
                            memHaveCnt = memHaveCnt - 1;
                            haveIdxList.remove(j);
                            size--;
                            j--;
                        } // end of if
                    } // end of for
                } // end of for
            }
        } // end of if

        /** 전체 소장할 회차 개수(최종) **/
        int totalBuyCnt = epTotalCnt - memHaveCnt;

        // 이미 전체 회차를 소장중
        if (totalBuyCnt <= memHaveCnt) {
            throw new CustomException(CustomError.PURCHASE_ALREADY_ALL_BUY); // 보유할 회차가 없습니다.
        }

        /** 7. 전체 소장할 코인 총합 조회(무료 회차<sort> 제외) **/
        int buyCoinPrice = getBuyCoinPrice(EPISODE_HAVE, originEpisodeList, episodeEventList);  // 결제할 총 금액

        /** 8. 전체 소장 할인율 조회  **/
        ContentDto contentFreeInfo = contentDaoSub.getContentFreeInfo(episodeDto); // 할인 정보 조회(minPurchaseCnt,discount)
        int minPurchaseCnt = contentFreeInfo.getMinPurchaseCnt(); // 할인 최소 개수

        // 할인 최소 개수 이상 구매 시만 할인율 적용
        // 무료 회차를 포함하는 회차가 할인율 적용 기준 이상
        if (totalBuyCnt >= minPurchaseCnt) {
            int discountPercent = contentFreeInfo.getDiscount();                // 할인율
            int disCountPrice = (int) (buyCoinPrice * discountPercent / 100.0); // 할인 금액
            buyCoinPrice = buyCoinPrice - disCountPrice;                        // 결제할 금액 - 할인 금액 (할인된 금액)
        }

        /** 전체 소장할 결제 금액 set **/
        purchaseDto.setBuyCoinPrice(buyCoinPrice);

        // 컨텐츠 제목 조회
        String title = contentDaoSub.getContentsTitleByIdx(contentIdx);
        title += " " + super.langMessage("lang.purchase.all.have");
        purchaseDto.setTitle(title); // 컨텐츠 제목 set

        /** 9. 회원 보유 코인 조회 (restCoin, restCoinFree, restMileage) **/
        CoinDto restCoinDto = coinService.getRestCoinAndRestMileage(memberIdx); // 잔여 코인 & 보너스 코인 & 마일리지 조회

        /** 만료된 코인 & 마일리지 update (member_coin_used & member_mileage_used 상태값 0으로) **/
        coinService.updateExpireCoinAndMileage(memberIdx);

        /** 10. 회차 구매 가능한지 검사 (결제할 코인이 부족하면 exception) **/
        possibleBuyValidate(restCoinDto, buyCoinPrice);

        /** 11. 코인 차감 로직 (사용한 코인 리턴) **/
        CoinDto usedCoinDto = subtractCoin(purchaseDto, restCoinDto); // 회원 남은 코인

        /** 12. 페이백 마일리지 지급 (구매 시 소진한 (코인 + 마일리지) x 5% + 등급별 추가 % 지급) **/
        int payBackMileage = 0;

        // 작품 구매 시 소진한 코인 또는 마일리지가 있을 때
        if (usedCoinDto.getUsedCoin() > 0 || usedCoinDto.getUsedMileage() > 0) {

            // 회원 등급 조회
            Integer memberGrade = gradeService.getMemberGradeLevel(memberIdx);

            // 페이백 마일리지 계산 -> 사용한 코인 & 사용한 마일리지 & 회원 등급 가져가서 계산
            payBackMileage = getPayBackMileage(usedCoinDto.getUsedCoin(), usedCoinDto.getUsedMileage(), memberGrade);

            // dto set
            CoinDto mileageDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .paymentIdx(0)
                    .achievementIdx(0L)
                    .mileage(payBackMileage)
                    .restMileage(payBackMileage)
                    .position("페이백")
                    .title("[" + title + "]" + " 감상 페이백")
                    .state(1)
                    .regdate(dateLibrary.getDatetime())
                    .mileageExpireDate(getCoinExpireDate(3))
                    .build();

            // member_mileage_save 테이블 insert
            coinDao.insertMileageSave(mileageDto);

            // member_mileage_save_log 테이블 insert
            coinDao.insertMileageSaveLog(mileageDto);

            // member_mileage_used 테이블 insert
            coinDao.insertMileageUsed(mileageDto);

            // member_mileage_used_log 테이블 insert
            coinDao.insertMileageUsedLog(mileageDto);
        }

        /** 13. 보유 코인 - 사용 코인 + 페이백 금액 **/
        int restCoin = restCoinDto.getRestCoin() - usedCoinDto.getUsedCoin();
        int restCoinFree = restCoinDto.getRestCoinFree() - usedCoinDto.getUsedCoinFree();
        int restMileage = restCoinDto.getRestMileage() - usedCoinDto.getUsedMileage() + payBackMileage;

        // 결제 후 남은 보유코인 정보
        CoinDto memberRestCoinDto = CoinDto.builder()
                .coin(restCoin)
                .coinFree(restCoinFree)
                .mileage(restMileage)
                .memberIdx(memberIdx)
                .build();

        /** 14. 회원 보유 코인 update **/
        int updateResult = coinDao.updateMemberCoin(memberRestCoinDto);

        if (updateResult < 1) {
            throw new CustomException(CustomError.PURCHASE_MEMBER_COIN_UPDATE_FAIL); // 회차구매에 실패하였습니다.
        }

        // 회원 전체 구매 등록에 사용할 dto
        PurchaseBuyAllDto purchaseBuyAllDto = PurchaseBuyAllDto.builder()
                .memberIdx(memberIdx)
                .contentsIdx(contentIdx)
                .totalCoin(buyCoinPrice)
                .episodeCount(totalBuyCnt)
                .type(EPISODE_HAVE)
                .regdate(dateLibrary.getDatetime())
                .expiredate(getExpireDate(EPISODE_HAVE))
                .build();

        // 회원 전체 구매 등록
        purchaseDao.insertMemberBuyAll(purchaseBuyAllDto);

        if (purchaseBuyAllDto.getInsertedIdx() == null) {
            throw new CustomException(CustomError.PURCHASE_EPISODE_BUY_FAIL); // 회차 구매에 실패하였습니다.
        }

        /** 15. member_purchase 등록 **/
        List<PurchaseDto> memberPurchaseList = new ArrayList<>();
        purchaseDto.setType(EPISODE_HAVE); // 소장 set
        purchaseDto.setExpiredate(getExpireDate(EPISODE_HAVE));
        purchaseDto.setRegdate(dateLibrary.getDatetime());
        purchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());

        // episodeToHaveDto(idx, buyCoinPrice)
        for (EpisodeDto episodeHaveDto : originEpisodeList) {
            // member_purchase 에 등록한 dto 객체
            PurchaseDto memberPurchaseDto = null;
            // purchaseCoinCalculate 메서드 돌면서 결제 금액 계속 감소
            buyCoinPrice = purchaseDto.getBuyCoinPrice();

            // 결제 금액이 남아 있음
            if (buyCoinPrice > 0) {
                /** member_purchase에 등록할 코인 계산 및 분배 작업 **/
                memberPurchaseDto = coinService.purchaseCoinCalculate(episodeHaveDto, usedCoinDto, purchaseDto);

                // 결제 금액 할인으로 결제할 금액이 0인 경우
            } else {
                memberPurchaseDto = PurchaseDto.builder()
                        .memberIdx(memberIdx)
                        .contentsIdx(purchaseDto.getContentsIdx())
                        .episodeIdx(episodeHaveDto.getIdx())
                        .coin(0)
                        .coinFree(0)
                        .mileage(0)
                        .usedTicket(0)
                        .type(purchaseDto.getType())
                        .title(purchaseDto.getTitle())
                        .route(purchaseDto.getRoute())
                        .buyAllIdx(purchaseDto.getBuyAllIdx())
                        .expiredate(purchaseDto.getExpiredate())
                        .regdate(purchaseDto.getRegdate())
                        .build();
            }
            // buyAllIdx set
            memberPurchaseDto.setBuyAllIdx(purchaseBuyAllDto.getInsertedIdx());
            memberPurchaseList.add(memberPurchaseDto);
        } // end of for

        // member_purchase 등록
        purchaseDao.insertMemberPurchaseList(memberPurchaseList);

        /** 16. contents_info 업데이트 (view, purchase)**/
        ContentDto contentInfo = contentDaoSub.getContentInfo(contentIdx);
        contentInfo.setPurchase(contentInfo.getPurchase() + totalBuyCnt);
        contentInfo.setView(contentInfo.getView() + totalBuyCnt);
        contentDao.updateViewCntAndPurchase(contentInfo);
    }


    /*********************************************************************
     * Insert
     *********************************************************************/

    /**
     * 개별 회차 구매
     * 코인 차감 로직(소장 or 대여)
     *
     * @param purchaseInfoDto : contentsIdx(컨텐츠 idx), episodeIdx(회차 idx), memberIdx(회원 idx),
     *                        : type(구매 유형), buyCoinPrice(결제할 금액), title(회차 제목)
     * @param restCoinDto     : restCoin(보유 코인), restCoinFree(보유 보너스 코인), restMielage(보유 마일리지), restTicket(보유 이용권)
     */
    private CoinDto subtractCoin(PurchaseDto purchaseInfoDto, CoinDto restCoinDto) {

        Integer contentIdx = purchaseInfoDto.getContentsIdx();  // 컨텐츠 idx
        Long episodeIdx = purchaseInfoDto.getEpisodeIdx();      // 회차 idx
        Long memberIdx = purchaseInfoDto.getMemberIdx();        // 회원 idx
        Integer buyType = purchaseInfoDto.getType();            // 구매 유형
        String title = purchaseInfoDto.getTitle();              // 회차 제목

        int restCoin = restCoinDto.getRestCoin();               // 회원 보유 코인
        int restCoinFree = restCoinDto.getRestCoinFree();       // 회원 보유 보너스 코인
        int restMileage = restCoinDto.getRestMileage();         // 회원 보유 마일리지

        int buyCoinPrice = purchaseInfoDto.getBuyCoinPrice();   // 결제할 금액

        // 회원 보유 이용권
        int restTicket;
        if(restCoinDto.getRestTicket() == null){
            restTicket = 0;
        }else{
            // 남은 이용권
            restTicket = restCoinDto.getRestTicket();
        }

        // 회차 구매에 사용할 코인 & 마일리지 미리 초기화 (사용한 금액으로 저장되어 return 됨)
        PurchaseDto purchaseDto = PurchaseDto.builder()
                .contentsIdx(contentIdx)
                .episodeIdx(episodeIdx)
                .coin(0)                      // 회차 구매에 사용한 코인
                .coinFree(0)                  // 회차 구매에 사용한 보너스 코인
                .mileage(0)                   // 회차 구매에 사용한 마일리지
                .usedTicket(0)                // 회차 구매에 사용한 이용권 개수
                .period(0)                    // 이용권 유효 시간(대여 시간)
                .buyAllIdx(0L)
                .memberIdx(memberIdx)
                .type(buyType)
                .title(title)                 // 회차 제목
                .buyCoinPrice(buyCoinPrice)
                .restMileage(restMileage)     // 현재 보유 마일리지
                .restCoinFree(restCoinFree)   // 현재 보유 보너스 코인
                .restCoin(restCoin)           // 현재 보유 코인
                .restTicket(restTicket)       // 현재 보유 이용권
                .build();


        /********************** 차감 순서(중요) : 이용권 -> 마일리지 -> 보너스 코인 -> 일반 코인 ***********************/

        /** 0. 사용 가능한 이용권이 있을 경우 **/
        if (restTicket > 0) {
            // 이용권 차감
            buyCoinPrice = coinService.subtractTicket(purchaseDto, buyCoinPrice);
        }

        /** 1. 결제할 금액이 남아 있고, 사용 가능한 마일리지가 있을 경우 **/
        if (buyCoinPrice > 0 && restMileage >= MIN_USE_MILEAGE) { // 마일리지는 100 단위로 사용 가능
            // 마일리지 차감
            buyCoinPrice = coinService.subtractMileage(purchaseDto, buyCoinPrice);
        }

        /** 2. 결제할 금액이 남아 있고, 사용 가능한 보너스 코인이 있을 경우 **/
        if (buyCoinPrice > 0 && restCoinFree > 0) {
            buyCoinPrice = coinService.subtractCoinOrCoinFree(purchaseDto, COIN_FREE, buyCoinPrice); // 무료 코인 차감

        }

        /** 3. 결제할 금액이 남아 있고, 사용 가능한 일반 코인이 있을 경우 **/
        if (buyCoinPrice > 0 && restCoin > 0) {
            buyCoinPrice = coinService.subtractCoinOrCoinFree(purchaseDto, COIN, buyCoinPrice);      // 코인 차감

        }

        // 결제할 금액이 남아 있다면 Exception
        if (buyCoinPrice != 0) {
            throw new CustomException(CustomError.HAVE_COIN_LACK);  // 보유코인이 부족합니다.
        }

        /** 4. 회차 구매에 사용한 코인 계산하여 보유코인 업데이트 **/

        // 사용한 코인
        int usedCoin = purchaseDto.getCoin();
        int usedCoinFree = purchaseDto.getCoinFree();
        int usedMileage = purchaseDto.getMileage();
        int usedTicket = purchaseDto.getUsedTicket();
        int usedPeriod = purchaseDto.getPeriod();

        // 사용한 코인, 보너스 코인, 마일리지, 이용권 리턴
        return CoinDto.builder()
                .usedCoin(usedCoin)
                .usedCoinFree(usedCoinFree)
                .usedMileage(usedMileage)
                .usedTicket(usedTicket)
                .period(usedPeriod)
                .build();
    }

    /*********************************************************************
     * Delete
     *********************************************************************/

    /**
     * 회원 코인 사용내역 삭제
     *
     * @param purchaseDto
     */
    @Transactional
    public void deleteMemberUsedCoinList(PurchaseDto purchaseDto) {

        /** 유효성 검사 **/
        deleteUsedCoinValidate(purchaseDto);
        // 코인 사용내역 삭제
        int updateResult = purchaseDao.deleteMemberUsedCoinList(purchaseDto);

        if (updateResult < 1) {
            throw new CustomException(CustomError.PURCHASE_DELETE_FAIL);  // 구매내역 삭제에 실패하였습니다.
        }
    }

    /**
     * 내가 본 작품 리스트 삭제 (내 서재)
     *
     * @param searchDto : idxList, memberIdx, searchType
     */
    public void deleteMemberLastViewList(SearchDto searchDto) {

        /** 유효성 검사 **/
        deleteMemberLastViewValidate(searchDto);

        // 코인 사용내역 삭제
        int deleteResult = episodeDao.deleteMemberLastViewList(searchDto);

        if (deleteResult < 1) {
            throw new CustomException(CustomError.CONTENTS_VIEW_DELETE_FAIL);  // 구매내역 삭제에 실패하였습니다.
        }
    }

    /**
     * 대여 및 소장 리스트 삭제(내 서재)
     *
     * @param searchDto : idxList, memberIdx, searchType
     */
    public void deleteMemberPurchaseList(SearchDto searchDto) {
        /** 유효성 검사 **/
        deleteMemberPurchaseValidate(searchDto);
        
        // 구매 번호로 컨텐츠 idx 조회
        List<Integer> contentsIdxList = purchaseDaoSub.getContentsIdxList(searchDto);

        searchDto.setContentsIdxList(contentsIdxList);

        // 구매 회차 삭제
        int deleteResult = purchaseDao.deleteMemberPurchaseList(searchDto);

        if (deleteResult < 1) {
            throw new CustomException(CustomError.PURCHASE_DELETE_FAIL);  // 구매내역 삭제에 실패하였습니다.
        }
    }


    /*********************************************************************
     * 회차 만료일 계산
     *********************************************************************/

    /**
     * 회차 만료일 계산
     * 대여 시 만료일 return, 소장 시 null return
     *
     * @param buyType : 회차 구매 방법(RENT : 대여)
     * @return
     */
    private String getExpireDate(int buyType) {

        Calendar cal = Calendar.getInstance();
        Date expireDate;
        String stringExpireDate = null;
        // 대여
        if (buyType == EPISODE_RENT) {
            // 일주일 후
            cal.add(Calendar.DATE, +Calendar.DAY_OF_WEEK + 1);
            expireDate = cal.getTime();
            SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
            // 타임존 UTC 기준
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            formatDatetime.setTimeZone(utcZone);

            // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
            stringExpireDate = formatDatetime.format(expireDate);
        }

        return stringExpireDate;
    }

    /*********************************************************************
     * 이미지 리사이징
     *********************************************************************/

    /**
     * 컨텐츠 이미지 url setting
     *
     * @param purchaseList
     * @return
     */
    public void setImgFullUrl(List<PurchaseDto> purchaseList) {

        for (PurchaseDto purchaseDto : purchaseList) {

            // 컨텐츠 세로 이미지 리스트 url setting
            setContentImgFulUrl(purchaseDto.getContentHeightImgList());
        }
    }

    /**
     * 컨텐츠 이미지 fulUrl 세팅
     * 리사이징 된 이미지(s3Library.getThumborFullUrl())
     *
     * @param contentImgDtoList
     */
    private void setContentImgFulUrl(List<ContentImgDto> contentImgDtoList) {

        if (contentImgDtoList != null && !contentImgDtoList.isEmpty()) {

            for (ContentImgDto contentImgDto : contentImgDtoList) {

                if (contentImgDto.getUrl() != null) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("fileUrl", contentImgDto.getUrl());     // 이미지 url
                    map.put("width", contentImgDto.getWidth());     // 이미지 가로 사이즈
                    map.put("height", contentImgDto.getHeight());   // 이미지 세로 사이즈

                    String fullUrl = s3Library.getThumborFullUrl(map);
                    contentImgDto.setUrl(fullUrl);
                }
            }
        }
    }

    /*********************************************************************
     * SUB
     *********************************************************************/

    /**
     * 회차 구매 가능한지 검사
     *
     * @param restCoinDto  : restCoin(남은 코인), restCoinFree(남은 보너스 코인), restMileage(남은 마일리지), restTicket(남은 이용권)
     * @param buyCoinPrice : 결제 할 금액
     * @return
     */
    public void possibleBuyValidate(CoinDto restCoinDto, int buyCoinPrice) {

        // 남은 이용권
        int restTicket;
        if(restCoinDto.getRestTicket() == null){
            restTicket = 0;

        } else {
            // 남은 이용권
            restTicket = restCoinDto.getRestTicket();
        }

        // 남은 이용권이 없는 경우
        if (restTicket < 1) {

            // 남은 코인은 member_coin 테이블 기준으로 조회
            int restCoin = restCoinDto.getRestCoin();
            int restCoinFree = restCoinDto.getRestCoinFree();

            // 조회한 마일리지 100:1로 단위 맞추기(100마일리지 1코인)
            int restMileage = restCoinDto.getRestMileage();

            // 남은 마일리지 코인 단위로 환산(마일리지로만 구매 가능 여부 체크 목적)
            int totalRestMileage = restMileage / MILEAGE_PERCENTAGE;

            // 남은 코인 + 보너스 코인 + 마일리지
            int totalRestCoinAndMileage = (((restCoin + restCoinFree) * MILEAGE_PERCENTAGE) + restMileage) / MILEAGE_PERCENTAGE;

            /** 1. 꿀툰 서비스 종료 -> 코인 사용 불가 시작 시간(24년 02월 29일 오전 10시) 체크 **/
            if (dateLibrary.checkIsPassed(COIN_PURCHASE_DISABLED)) { // 24년 02월 29일 오전 10시부터
                // 마일리지 + 코인으로는 구매 가능하나, 마일리지만으로는 구매할 수 없는 경우
                if (buyCoinPrice > totalRestMileage && buyCoinPrice <= totalRestCoinAndMileage) {
                    throw new CustomException(CustomError.COIN_PURCHASE_LOCK);  // 서비스 종료로 코인 사용이 불가능합니다.
                }
            }

            /** 2. 구매 코인이 부족할 경우 **/
            if (buyCoinPrice > totalRestCoinAndMileage) {
                throw new CustomException(CustomError.HAVE_COIN_LACK);  // 보유코인이 부족합니다.
            }
        }
    }

    /**
     * 작품 작가 & 태그 정보 세팅
     *
     * @param purchaseList
     * @return
     */
    private void setAuthorAndTag(List<PurchaseDto> purchaseList) {

        for (PurchaseDto purchaseDto : purchaseList) {
            if (purchaseDto != null) {

                // 작품 작가 & 태그 정보 세팅
                setAuthorAndTag(purchaseDto);
            }
        }
    }

    /**
     * 작품 작가 & 태그 정보 세팅
     *
     * @param purchaseDto
     * @return
     */
    private void setAuthorAndTag(PurchaseDto purchaseDto) {

        // 세팅용 리스트 생성
        List<AuthorDto> writerList = new ArrayList<>();  // 글작가 리스트
        List<AuthorDto> painterList = new ArrayList<>(); // 그림작가 리스트
        List<TagDto> tagList = new ArrayList<>();        // 태그 리스트

        // 글작가 set
        if (purchaseDto.getWriterList() != null && !purchaseDto.getWriterList().isEmpty() && purchaseDto.getWriterList().size() > 0) {
            String[] writerStringList = purchaseDto.getWriterList().get(0).getName().split(",");

            for (String name : writerStringList) {
                AuthorDto writerDto = new AuthorDto();
                writerDto.setName(name);
                writerList.add(writerDto);
            }
            purchaseDto.setWriterList(writerList);
        }

        // 그림작가 set
        if (purchaseDto.getPainterList() != null && !purchaseDto.getPainterList().isEmpty() && purchaseDto.getPainterList().size() > 0) {
            String[] painterStringList = purchaseDto.getPainterList().get(0).getName().split(",");
            for (String name : painterStringList) {
                AuthorDto painterDto = new AuthorDto();
                painterDto.setName(name);
                painterList.add(painterDto);
            }
            purchaseDto.setPainterList(painterList);
        }

        // 태그 set
        if (purchaseDto.getTagList() != null && !purchaseDto.getTagList().isEmpty() && purchaseDto.getTagList().size() > 0) {
            String[] tagStringList = purchaseDto.getTagList().get(0).getName().split(",");
            for (String name : tagStringList) {
                TagDto tagDto = new TagDto();
                tagDto.setName(name);
                tagList.add(tagDto);
            }
            purchaseDto.setTagList(tagList);
        }
    }

    /**
     * 배지 코드 세팅
     * 완결 배지 세팅된 경우 -> 신작 배지 세팅 X
     *
     * @param purchaseDtoList
     */
    public void setBadgeCode(List<PurchaseDto> purchaseDtoList, String type) {

        // nowDate set
        String nowDate = dateLibrary.getDatetimeToSeoul();

        // 이벤트 무료 회차 idx 리스트 전체 조회
        List<Integer> freeIdxList = contentDaoSub.getEventFreeEpisodeInfo(nowDate);

        // 컨텐츠 idx 리스트 set
        List<Integer> idxList = new ArrayList<>();
        for (PurchaseDto purchaseDto : purchaseDtoList) {
            if (purchaseDto != null) {
                idxList.add(purchaseDto.getContentsIdx());
            }
        }

        // 이벤트 할인 회차 개수 리스트 조회
        List<ContentDto> discountEpisodeCntList = contentDaoSub.getEventEpisodeCount(nowDate);
        List<Integer> discountIdxList = new ArrayList<>();
        if (!discountEpisodeCntList.isEmpty()) {
            for (int index = 0; index < discountEpisodeCntList.size(); index++) {
                discountIdxList.add(discountEpisodeCntList.get(index).getIdx());
            }
        }

        // 랭킹 리스트에 포함된 작품 idx 리스트
        List<Integer> rankIdxList = contentDaoSub.getRankContentsIdxList();

        /** 배지 코드 세팅 시작 **/
        for (PurchaseDto purchaseDto : purchaseDtoList) {
            if (purchaseDto != null) {

                // 배지 리스트 생성
                purchaseDto.setBadgeList(new ArrayList<>());
                List<BadgeDto> badgeList = purchaseDto.getBadgeList();

                /** 작품에 이벤트 무료 회차가 존재할 경우 free 배지 세팅 **/
                if (!freeIdxList.isEmpty()) {
                    if (freeIdxList.contains(purchaseDto.getContentsIdx())) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_FREE); // free 코드 set
                        badgeList.add(badgeDto);
                    }
                }

                /** 작품에 이벤트 할인 회차가 존재할 경우 discount 배지 세팅 **/
                if (!discountIdxList.isEmpty()) {
                    if (discountIdxList.contains(purchaseDto.getContentsIdx())) {

                        // 이벤트 할인 회차 idx 리스트의 인덱스 구하기
                        int discountIdx = discountIdxList.indexOf(purchaseDto.getContentsIdx());

                        // 이벤트 할인 회차가 1개라도 있는 경우
                        if (discountEpisodeCntList.get(discountIdx).getDiscountEpisodeCnt() > 0) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_DISCOUNT); // discount 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 완결 작품일 경우 complete 배지 세팅 **/
                Integer progress = purchaseDto.getProgress(); // 작품 완결 여부
                purchaseDto.setProgress(null); // 앞단에서 안쓰므로 null 처리

                if (progress != null && progress == 3) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_COMPLETE); // complete 코드 set
                    badgeList.add(badgeDto);

                } else {

                    /** 완결 배지 세팅 X 경우에만 -> 작품 발행 후 30일이 지나지 않았다면 new 배지 세팅 **/
                    String contentsPubDate = purchaseDto.getContentsPubdate(); // 작품 발행일
                    purchaseDto.setContentsPubdate(null); // 앞단에서 안쓰므로 null 처리

                    if (contentsPubDate != null && !contentsPubDate.isEmpty()) {
                        if (dateLibrary.isNotAfterDate(contentsPubDate, dateLibrary.ONE_MONTH)) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_NEW); // new 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 마지막 회차 업데이트 후 24시간이 지나지 않았다면 up 배지 세팅 **/
                String episodePubDate = purchaseDto.getEpisodePubdate(); // 회차 발행일
                purchaseDto.setEpisodePubdate(null); // 앞단에서 안쓰므로 null 처리

                if (episodePubDate != null && !episodePubDate.isEmpty()) {
                    if (dateLibrary.isNotAfterDate(episodePubDate, dateLibrary.ONE_DAY)) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_UP); // up 코드 set
                        badgeList.add(badgeDto);
                    }
                }

                /** 독점 작품일 경우 only 배지 세팅 **/
                Integer exclusive = purchaseDto.getExclusive(); // 작품 독점 여부
                purchaseDto.setExclusive(null); // 앞단에서 안쓰므로 null 처리

                if (exclusive != null && exclusive == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_ONLY); // only 코드 set
                    badgeList.add(badgeDto);
                }

                /** 성인 작품일 경우 19 배지 세팅 **/
                Integer adult = purchaseDto.getAdult(); // 성인 작품 여부
                purchaseDto.setAdult(null); // 앞단에서 안쓰므로 null 처리

                if (adult != null && adult == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_ADULT_19); // adult_19 코드 sets
                    badgeList.add(badgeDto);
                }

                /** 작품이 단행본일 경우 book 배지 세팅 **/
                Integer publication = purchaseDto.getPublication(); // 작품 단행본 여부
                purchaseDto.setPublication(null); // 앞단에서 안쓰므로 null 처리

                if (publication != null && publication == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_BOOK); // book 코드 set
                    badgeList.add(badgeDto);
                }

                /** 작품이 개정판일 경우 revised 배지 세팅 **/
                Integer revision = purchaseDto.getRevision(); // 작품 개정판 여부
                purchaseDto.setRevision(null); // 앞단에서 안쓰므로 null 처리

                if (revision != null && revision == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_REVISED); // revised 코드 set
                    badgeList.add(badgeDto);
                }

                /** 소설 원작 작품일 경우 original 배지 세팅 **/
                // 태그 이름 리스트
                if (!purchaseDto.getTagList().isEmpty()) {
                    for (int index = 0; index < purchaseDto.getTagList().size(); index++) {
                        if (purchaseDto.getTagList().get(index).getName().equals(CODE_ORIGINAL_TEXT)) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_ORIGINAL); // original 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 랭킹 1위 ~ 100위 사이의 작품일 경우 top 배지 세팅 **/
                if (!rankIdxList.isEmpty()) {
                    // 랭킹 리스트에 포함된 작품일 경우
                    if (rankIdxList.contains(purchaseDto.getContentsIdx())) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_TOP); // top 코드 set
                        badgeList.add(badgeDto);
                    }
                }

                /** 내 서재 페이지 한정 - webtoon, comic, novel, adult_pavilion 배지 세팅 **/
                if (type.equals(LIBRARY)) {

                    String code = "";
                    String category = purchaseDto.getCategory(); // 작품의 카테고리 이름
                    Integer adultPavilion = purchaseDto.getAdultPavilion(); // 작품 성인관 여부

                    if (category != null && !category.isEmpty()) {

                        if (category.equals(WEBTOON_TEXT)) { // 카테고리가 웹툰일 때
                            code = CODE_WEBTOON; // webtoon 코드 set

                        } else if (category.equals(COMIC_TEXT)) { // 카테고리가 만화일 때
                            code = CODE_COMIC; // comic 코드 set

                        } else if (category.equals(NOVEL_TEXT)) { // 카테고리가 소설일 때
                            code = CODE_NOVEL; // novel 코드 set

                        } else if (adultPavilion == 1) { // 성인관 작품일 때
                            code = CODE_ADULT_PAVILION; // adult_pavilion 코드 set
                        }
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(code); // 카테고리 배지 set
                        badgeList.add(badgeDto);
                    }
                }
            }
        }
    }

    /*********************************************************************
     * Validate
     *********************************************************************/

    /**
     * 컨텐츠 idx 유효성 검사(공통)
     *
     * @param idx
     */
    private void contentIdxValidate(Integer idx) {
        // idx 기본 유효성 검사
        if (idx == null || idx < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 조회할 episodeDto set
        ContentDto contentDto = ContentDto.builder()
                .nowDate(dateLibrary.getDatetime())// 현재 시간
                .contentsIdx(idx) // 컨텐츠 idx
                .build();

        // idx db 조회 후 검사
        int contentCnt = contentDaoSub.getContentCnt(contentDto);

        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST); // 유효하지 않은 컨텐츠입니다.
        }
    }

    /**
     * 회차 구매 유효성검사
     *
     * @param purchaseDto : contentsIdx(컨텐츠 idx), episodeIdx(회차 idx), memberIdx(회원 idx), type(구매 유형)
     */
    private void purchaseEpisodeValidate(PurchaseDto purchaseDto) {

        // memberIdx 기본 유효성
        if (purchaseDto.getMemberIdx() == null || purchaseDto.getMemberIdx() < 1L) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);   // 로그인 후 이용 가능합니다.
        }

        // 컨텐츠 idx 기본 유효성 검사
        if (purchaseDto.getContentsIdx() == null || purchaseDto.getContentsIdx() < 1L) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 회차 idx 기본 유효성 검사
        if (purchaseDto.getEpisodeIdx() == null || purchaseDto.getEpisodeIdx() < 1L) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

        // 조회용 dto
        EpisodeDto episodeDto = EpisodeDto.builder()
                .idx(purchaseDto.getEpisodeIdx())         // 에피소드 idx
                .contentIdx(purchaseDto.getContentsIdx()) // 컨텐츠 idx
                .type(purchaseDto.getType())              // 구매 유형
                .nowDate(dateLibrary.getDatetime())       // 현재 시간
                .build();


        /** 유효한 회차인지 (contentIdx, episodeIdx) **/
        int episodeCnt = episodeDaoSub.getEpisodeCnt(episodeDto);

        if (episodeCnt < 1) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

        /** 구매 경로 체크 **/
        routeValidate(purchaseDto.getRoute());

        /** 구매 유형 체크 **/
        if (purchaseDto.getType() == null || !(purchaseDto.getType() == EPISODE_RENT || purchaseDto.getType() == EPISODE_HAVE)) {
            throw new CustomException(CustomError.PURCHASE_TYPE_ERROR); // 구매 유형이 올바르지 않습니다.
        }

        // 유효성 검사용 dto
        PurchaseDto purchaseDtoToValid = PurchaseDto.builder()
                .contentsIdx(purchaseDto.getContentsIdx())
                .episodeIdx(purchaseDto.getEpisodeIdx())
                .memberIdx(purchaseDto.getMemberIdx())
                .nowDate(dateLibrary.getDatetime())
                .build();

        // 대여중인 지 조회
        purchaseDtoToValid.setType(EPISODE_RENT);
        int rentCnt = purchaseDaoSub.getMemberPurchaseCnt(purchaseDtoToValid);

        // 소장중인 지 조회
        purchaseDtoToValid.setType(EPISODE_HAVE);
        int haveCnt = purchaseDaoSub.getMemberPurchaseCnt(purchaseDtoToValid);

        /** 소장 & 대여 여부 체크 **/
        if (rentCnt > 0 || haveCnt > 0) {
            // 소장중인데 소장
            if (purchaseDto.getType() == EPISODE_HAVE && haveCnt > 0) {
                throw new CustomException(CustomError.PURCHASE_ALREADY_HAVE); // 이미 소장중인 회차입니다.
                // 소장중인데 대여
            } else if (purchaseDto.getType() == EPISODE_RENT && haveCnt > 0) {
                throw new CustomException(CustomError.PURCHASE_ALREADY_HAVE); // 이미 소장중인 회차입니다.
                // 대여중인데 대여
            } else if (purchaseDto.getType() == EPISODE_RENT && rentCnt > 0) {
                throw new CustomException(CustomError.PURCHASE_ALREADY_RENT); // 이미 대여중인 회차입니다.
            }
        }

        /** 성인 컨텐츠 여부 체크 **/
        int contentAdult = contentDaoSub.getContentAdult(purchaseDto.getContentsIdx());
        // 회원 성인 여부
        int memberAdult = Integer.parseInt(super.getMemberInfo(ADULT));

        // 성인 컨텐츠 
        if (contentAdult == 1) {
            // 비성인
            if (memberAdult == 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        /** 무료 회차 개별 구매 불가 **/

        /** 4. 현재 회차 무료 OR 이벤트 무료 여부 조회 **/

        // 무료 회차 & 이벤트 무료 회차 & 이벤트 상태 DB 조회
        PurchaseDto freeInfo = purchaseDaoSub.getEpisodeFreeInfo(purchaseDto.getContentsIdx());

        int freeCnt = freeInfo.getFreeEpisodeCnt(); // 기본 무료 sort
        int eventFreeCnt = 0;                       // 이벤트 무료 sort

        // 이벤트 진행 중
        if (freeInfo != null && freeInfo.getEventFreeUsed() == 1) {
            String startDate = freeInfo.getStartDate(); // 이벤트 시작일
            String endDate = freeInfo.getEndDate();     // 이벤트 종료일

            // 현재 날짜 기준 이벤트 진행중인 경우
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                if (dateLibrary.checkEventState(startDate, endDate)) {
                    // 이벤트 무료 회차 초기화
                    eventFreeCnt = freeInfo.getEventFreeEpisodeCnt();
                }
            } // end of if
        } // end of 이벤트 진행 중

        // 최종 무료 회차 sort
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);
        
        // 구매하려는 회차 sort 조회
        int episodeSort = episodeDaoSub.getEpisodeSortByIdx(purchaseDto.getEpisodeIdx());

        if (episodeSort <= epFreeMaxSort) {
            // 본 회차는 무료 회차로 개별 구매가 불가합니다.
            throw new CustomException(CustomError.PURCHASE_CANT_BUY_FREE);
        }

        // 회차 이벤트 리스트 조회
        Integer episodeEventCoin = null;

        if (purchaseDto.getType() == EPISODE_RENT) {
            episodeEventCoin = episodeDaoSub.getEpisodeEventRentCoin(episodeDto);

        } else {
            episodeEventCoin = episodeDaoSub.getEpisodeEventCoin(episodeDto);
        }
        if (episodeEventCoin != null && episodeEventCoin == 0) {
            // 본 회차는 이벤트로 인해 무료이므로 구매가 불가합니다.
            throw new CustomException(CustomError.PURCHASE_CANT_BUY_EVENT);
        }

    }

    /**
     * 유료회차 전체 소장 유효성 검사
     *
     * @param purchaseDto
     */
    private void haveAllOnlyPaidEpisodeValidate(PurchaseDto purchaseDto) {
        /** 전체 소장 공통 유효성 검사 **/
        haveAllEpisodeValidate(purchaseDto);

        // 무료회차 포함 여부
        if (purchaseDto.getIncludeFree() == null || purchaseDto.getIncludeFree() != Boolean.FALSE) {
            throw new CustomException(CustomError.PURCHASE_INCLUDE_FREE_NOT_MATCH); // 무료 회차 포함여부가 불명확합니다.
        }
    }

    /**
     * 무료회차 포함 전체 소장 유효성 검사
     *
     * @param purchaseDto
     */
    private void haveAllIncludeFreeEpisodeValidate(PurchaseDto purchaseDto) {
        /** 전체 소장 공통 유효성 검사 **/
        haveAllEpisodeValidate(purchaseDto);

        // 무료회차 포함 여부
        if (purchaseDto.getIncludeFree() == null || purchaseDto.getIncludeFree() != Boolean.TRUE) {
            throw new CustomException(CustomError.PURCHASE_INCLUDE_FREE_NOT_MATCH); // 무료 회차 포함여부가 불명확합니다.
        }
    }

    /**
     * 전체 구매 공통 유효성 검사
     *
     * @param purchaseDto
     */
    private void haveAllEpisodeValidate(PurchaseDto purchaseDto) {

        // memberIdx 기본 유효성
        if (purchaseDto.getMemberIdx() == null || purchaseDto.getMemberIdx() < 1L) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);   // 로그인 후 이용 가능합니다.
        }

        /** 컨텐츠 idx 유효성 검사 **/
        contentIdxValidate(purchaseDto.getContentsIdx());

        /** 구매 경로 유효성 검사 **/
        routeValidate(purchaseDto.getRoute());

        /** 성인 컨텐츠 여부 체크 **/
        int contentAdult = contentDaoSub.getContentAdult(purchaseDto.getContentsIdx());

        // 회원 성인 여부
        int memberAdult = Integer.parseInt(super.getMemberInfo(ADULT));

        // 성인 컨텐츠
        if (contentAdult == 1) {
            // 비성인
            if (memberAdult == 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }
    }

    /**
     * 회차 소장 or 대여 리스트 기본 유효성 검사
     *
     * @param searchDto
     */
    private void purchaseEpisodeListValidate(SearchDto searchDto) {
        /** 1. 회원 idx 기본 검사 **/
        if (searchDto.getIdx() == null || searchDto.getIdx() < 1) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);    // 로그인 후 이용해주세요.
        }
        // 구매 유형 -> rent: 대여, have : 소장
        String searchType = searchDto.getSearchType().trim();
        /** 2. 구매 기본 유효성 검사 **/
        if (searchType == null || !(searchType.equals("rent") || searchType.equals("have"))) {
            throw new CustomException(CustomError.PURCHASE_TYPE_ERROR); // 구매유형이 올바르지 않습니다.
        }
    }

    /**
     * 코인 사용내역 삭제 유효성 검사
     */
    private void deleteUsedCoinValidate(PurchaseDto purchaseDto) {
        /** 1. 회원 idx 기본 검사 **/
        if (purchaseDto.getMemberIdx() == null || purchaseDto.getMemberIdx() < 1) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);    // 로그인 후 이용해주세요.
        }

        List<Long> idxList = purchaseDto.getIdxList();

        /** 2. 구매 idx 기본 검사 **/
        for (Long idx : idxList) {
            if (idx == null || idx < 1) {
                throw new CustomException(CustomError.PURCHASE_IDX_ERROR); // 유효하지않은 구매내역입니다.
            }
        }

        /** 3.유효한 사용내역인지 DB 조회 **/
        for (Long idx : idxList) {
            PurchaseDto purchase = PurchaseDto.builder()
                    .memberIdx(purchaseDto.getMemberIdx())
                    .idx(idx)
                    .build();

            int purchaseCnt = purchaseDaoSub.getPurchaseCnt(purchase);

            if (purchaseCnt < 1) {
                throw new CustomException(CustomError.PURCHASE_IDX_ERROR);  // 유효하지 않은 구매내역입니다.
            }
        }
    }

    /**
     * 관심 작품 리스트 삭제 유효성 검사
     *
     * @param searchDto
     */
    private void deleteMemberLastViewValidate(SearchDto searchDto) {
        // 회원 idx 빈값
        if (searchDto.getMemberIdx() == null || searchDto.getMemberIdx() < 1L) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);          // 로그인 후 이용해주세요.
        }
        // idx 리스트가 빈값
        if (searchDto.getIdxList() == null || searchDto.getIdxList().isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_DELETE_IDX_EMPTY); // 삭제할 작품이 없습니다.
        }
        // 유효한 idx 인지 조회
        int idxCnt = searchDto.getIdxList().size();                           // 앞단에서 넘겨받은 idx 리스트 길이
        int viewContentCnt = purchaseDaoSub.getLastViewIdxListCnt(searchDto); // 조회한 idx 카운트

        // 넘겨 받은 idx 유효하지 않음
        if (idxCnt != viewContentCnt) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);        // 요청하신 작품정보를 찾을 수 없습니다.
        }
    }

    /**
     * 구매 회차 리스트 삭제 유효성 검사
     *
     * @param searchDto
     */
    private void deleteMemberPurchaseValidate(SearchDto searchDto) {
        // 회원 idx 빈값
        if (searchDto.getMemberIdx() == null || searchDto.getMemberIdx() < 1L) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);          // 로그인 후 이용해주세요.
        }
        // idx 리스트가 빈값
        if (searchDto.getIdxList() == null || searchDto.getIdxList().isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_DELETE_IDX_EMPTY); // 삭제할 작품이 없습니다.
        }

        // 유효한 idx 인지 조회
        int idxCnt = searchDto.getIdxList().size();                           // 앞단에서 넘겨받은 idx 리스트 길이
        int viewContentCnt = purchaseDaoSub.getPurchaseIdxListCnt(searchDto); // 조회한 idx 카운트

        // 넘겨 받은 idx 유효하지 않음
        if (idxCnt != viewContentCnt) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);        // 요청하신 작품정보를 찾을 수 없습니다.
        }
    }

    /**
     * 유입 경로 유효성 검사
     *
     * @param route
     */
    private void routeValidate(Integer route) {

        if (route == null) {
            throw new CustomException(CustomError.PURCHASE_ROUTE_ERROR); // 잘못된 유입 경로입니다.
        } else if (route != 1 && route != 2) {
            throw new CustomException(CustomError.PURCHASE_ROUTE_ERROR); // 잘못된 유입 경로입니다.
        }
    }

    /*********************************************************************
     * 문자변환
     *********************************************************************/

    /**
     * List text 변환
     *
     * @param purchaseDtoList
     */
    private void stateText(List<PurchaseDto> purchaseDtoList) {
        for (PurchaseDto purchaseDto : purchaseDtoList) {
            stateText(purchaseDto);
        }
    }

    /**
     * text 변환
     *
     * @param purchaseDto
     */
    private void stateText(PurchaseDto purchaseDto) {
        // 소장, 대여 텍스트
        if (purchaseDto.getType() != null) {
            if (purchaseDto.getType() == EPISODE_RENT) {
                purchaseDto.setTypeText(super.langMessage("lang.contents.rent"));
            } else if (purchaseDto.getType() == EPISODE_HAVE) {
                purchaseDto.setTypeText(super.langMessage("lang.contents.have"));
            }
        }
        // 2. 회차 번호 텍스트
        if (purchaseDto.getEpisodeNumber() != null) {
            if (purchaseDto.getCategory().equals(COMIC_TEXT)) {
                purchaseDto.setEpisodeNumTitle(purchaseDto.getEpisodeNumber() + "권");
            } else {
                purchaseDto.setEpisodeNumTitle(purchaseDto.getEpisodeNumber() + "화");
            }
        }
    }

    /**
     * 날짜 빈값으로 설정(내 서재 소장 및 대여 조회 시 사용)
     *
     * @param purchaseList
     */
    private void setRegDateEmpty(List<PurchaseDto> purchaseList) {
        for (PurchaseDto purchaseDto : purchaseList) {
            if (purchaseDto != null) {
                purchaseDto.setRegdate("");
            }
        }
    }

    /**
     * 내 서재 타입 문자변환(최근 내가 본 작품)
     *
     * @param purchaseDtoList
     */
    private void typeText(List<PurchaseDto> purchaseDtoList) {
        for (PurchaseDto purchaseDto : purchaseDtoList) {
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
    }
}