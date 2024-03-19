package com.architecture.admin.services.coin;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.coin.CoinDao;
import com.architecture.admin.models.dao.gift.GiftDao;
import com.architecture.admin.models.daosub.coin.CoinDaoSub;
import com.architecture.admin.models.daosub.gift.GiftDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.gift.GiftDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.architecture.admin.libraries.utils.CoinUtils.*;

@Service
@RequiredArgsConstructor
public class CoinService extends BaseService {

    private final CoinDao coinDao;
    private final CoinDaoSub coinDaoSub;
    private final GiftDao giftDao;
    private final GiftDaoSub giftDaoSub;

    /**********************************************************************
     * Select
     *********************************************************************/

    /**
     * 소멸 코인 리스트 조회
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getExpireCoinList(SearchDto searchDto) {

        // totalRecordCount
        int totalCnt = 0;

        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        // 코인 소멸내역 카운트(코인 검색)
        if (searchDto.getSearchType().equals("coin")) {
            totalCnt = coinDaoSub.getExpireCoinTotalCnt(searchDto);
            // 마일리지 소멸내역 카운트 (마일리지 검색)
        } else if (searchDto.getSearchType().equals("mileage")) {
            totalCnt = coinDaoSub.getExpireMileageTotalCnt(searchDto);
        } else {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR);  // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 소멸 예정일(7일 setting)
        searchDto.setEndDate(getExpectedCoinExpireDate());

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        // return value
        JSONObject jsonData = new JSONObject();
        List<CoinDto> expireCoinList = null;

        // 코인 소멸 or 마일리지 소멸 리스트 존재
        if (totalCnt > 0) {
            if (searchDto.getSearchType().equals("coin")) {
                // 소멸 및 소멸예정 코인 조회
                expireCoinList = coinDaoSub.getExpireCoinList(searchDto);
                // 마일리지 소멸 리스트 조회
            } else if (searchDto.getSearchType().equals("mileage")) {
                // 소멸 및 소명예정 마일리지 조회
                expireCoinList = coinDaoSub.getExpireMileageList(searchDto);
                // 마일리지 타입 지정(3)
                setMileageType(expireCoinList);
            }
            // 상태 문자 변환
            stateText(expireCoinList);
            // 소멸 & 소멸 예정 문자 set
            expireText(expireCoinList);
            jsonData.put("params", new JSONObject(searchDto)); // 페이징
        }
        jsonData.put("expireCoinList", expireCoinList);      // 이미 소멸된 코인 리스트

        return jsonData;
    }

    /**
     * 마일리지 지급 내역 조회(만료되지 않은 마일리지만)
     * 1. 페이백 지급
     * 2. 관리자 지급
     * 3. 이벤트 지급
     *
     * @param searchDto : memberIdx(회원 idx)
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getGivenMileageList(SearchDto searchDto) {

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 비로그인 상태일 경우
        if (memberInfo == null) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해주세요.
        }

        // return value
        JSONObject jsonData = new JSONObject();
        List<CoinDto> givenMileageList = null;

        // 현재 시간 set : 지급받은 마일리지 만료 여부 체크용
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 마일리지 지급 내역 개수 카운트
        int totalCnt = coinDaoSub.getGivenMileageTotalCnt(searchDto);

        // 마일리지 지급 내역이 있는 경우
        if (totalCnt > 0) {

            // 마일리지 지급 내역 조회
            givenMileageList = coinDaoSub.getGivenMileageList(searchDto);

            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        jsonData.put("givenMileageList", givenMileageList);
        return jsonData;
    }

    /**
     * 오늘 받은 로그인 마일리지 지급 내역 조회
     *
     * @param searchDto : memberIdx(회원 idx)
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getTodayLoginMileageInfo(SearchDto searchDto) {

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 비로그인 상태일 경우
        if (memberInfo == null) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해주세요.
        }

        // return value
        JSONObject jsonData = new JSONObject();
        List<CoinDto> loginMileageInfo;

        // 현재 시간(YYYY-MM-DD) set
        searchDto.setNowDate(dateLibrary.getDay(dateLibrary.getDatetime()));
        
        // 검색 유형 set
        searchDto.setSearchType("로그인");

        // 오늘 받은 로그인 마일리지 내역 조회
        loginMileageInfo = coinDaoSub.getTodayLoginMileageInfo(searchDto);

        // 지급 내역 담기
        jsonData.put("loginMileageInfo", loginMileageInfo);
        return jsonData;
    }

    /**********************************************************************
     * 이용권 차감 로직(개별 회차 대여만 가능)
     *********************************************************************/

    /**
     * 이용권 차감
     *
     * @param purchaseDto  : restTicket(보유 이용권), episodeIdx(회차 idx)
     * @return
     */
    @Transactional
    public int subtractTicket(PurchaseDto purchaseDto, int buyCoinPrice) {

        // 총 남은 이용권
        int totalRestTicket = purchaseDto.getRestTicket();

        // 보유 이용권이 존재
        if (totalRestTicket > 0) {
            
            // searchDto set
            SearchDto searchDto = new SearchDto();
            searchDto.setMemberIdx(purchaseDto.getMemberIdx());
            searchDto.setContentsIdx(purchaseDto.getContentsIdx());
            searchDto.setNowDate(dateLibrary.getDatetime());

            // 회원이 특정 작품에 대해 지급 받은 이용권 중 지급 제외 회차(최신 회차)개수 최소값 조회
            int minExceptCnt = giftDaoSub.getMemberMinExceptCnt(searchDto);
            searchDto.setMinExceptCnt(minExceptCnt);

            // 회원이 특정 작품에 대해 지급 받은 유효한 선물 리스트 조회(이용권 지급 제외 회차 최소값 기준 + 종료일 빠른순 정렬)
            List<GiftDto> memberGiftList = giftDaoSub.getMemberGiftList(searchDto);

            if (memberGiftList != null && !memberGiftList.isEmpty()) {

                // 선물 리스트 순회
                for (GiftDto dto : memberGiftList) {

                    // 이용권 지급 제외 회차(최신 회차) idx 리스트 조회
                    List<Long> exceptEpisodeIdxList = dto.getExceptEpisodeIdxList();

                    // 현재 감상하려는 회차가 이용권 사용 가능 회차일 경우
                    if (!exceptEpisodeIdxList.contains(purchaseDto.getEpisodeIdx())) {

                        // 이용권 차감 결과 = 음수일 경우
                        if (dto.getRestCnt() - 1 < 0) {
                            continue;

                            // 이용권 차감 결과 = 0 또는 양수일 경우
                        } else {

                            // dto set
                            GiftDto giftDto = GiftDto.builder()
                                    .saveIdx(dto.getSaveIdx())
                                    .idx(dto.getIdx())
                                    .memberIdx(purchaseDto.getMemberIdx())
                                    .ticketCnt(dto.getTicketCnt())   // 지급 개수
                                    .restCnt(dto.getRestCnt() - 1)   // 잔여 개수 = 기존 잔여 개수 - 1
                                    .useCnt(1)                       // 사용 개수
                                    .contentsTitle("[" + dto.getContentsTitle() + "] 이용권 사용")
                                    .state(1)
                                    .regdate(dateLibrary.getDatetime())
                                    .build();

                            /** contents_ticket_used 테이블 update **/
                            giftDao.updateMemberGiftUsed(giftDto);

                            /** contents_ticket_used_log 테이블 insert **/
                            giftDao.insertMemberGiftUsedLog(giftDto);

                            /** contents_ticket_stat 테이블 통계 반영 **/
                            giftDao.insertGiftUseCnt(giftDto);

                            // purchaseDto set
                            purchaseDto.setUsedTicket(1);               // 사용한 이용권 개수
                            purchaseDto.setPeriod(dto.getUsePeriod());  // 이용권 사용 시 유효 시간
                        }

                        // 남은 결제 금액 0원으로 set
                        buyCoinPrice = 0;

                        // 남은 결제 금액 return
                        return buyCoinPrice;
                    }
                }
            }
        }
        // 남은 결제 금액 return
        return buyCoinPrice;
    }

    /**********************************************************************
     * 마일리지 차감 로직(회차 구매, 대여 시)
     *********************************************************************/

    /**
     * 마일리지 차감
     *
     * @param purchaseDto  : restMileage(보유 마일리지)
     * @param buyCoinPrice : 결제할 금액(코인)
     * @return
     */
    @Transactional
    public int subtractMileage(PurchaseDto purchaseDto, int buyCoinPrice) {

        buyCoinPrice = buyCoinPrice * MILEAGE_PERCENTAGE;    // 차감할 코인 -> 마일리지 단위로 변경

        int totalRestMileage = purchaseDto.getRestMileage(); // 총 남은 마일리지

        // 보유 마일리지가 존재
        if (totalRestMileage / MILEAGE_PERCENTAGE > 1) {

            int unavailableMileage = totalRestMileage % MILEAGE_PERCENTAGE;  // 사용 불가한 마일리지(10의 자리)
            int availableMileage = totalRestMileage - unavailableMileage;    // 사용 가능한 마일리지(100의 배수)

            // 차감할 금액이 보유 마일리지보다 큰 경우
            if (buyCoinPrice > availableMileage) {
                // 구매 코인 - 사용 가능 마일리지(리턴 할 결과값) 초기화
                buyCoinPrice = (buyCoinPrice - availableMileage) / MILEAGE_PERCENTAGE;

                // 마일리지로만 차감이 가능한 경우
            } else {
                availableMileage = buyCoinPrice;
                buyCoinPrice = 0;
            } // end of else

            /** mileage_used 테이블 update 및 mileage_used_log 테이블 insert **/
            updateMileageUsedAndInsertUsedLog(purchaseDto, availableMileage);
            purchaseDto.setMileage(availableMileage); // 회차 구매에 사용한 마일리지 set
            // 사용 가능한 마일리지 존재하지 않음
        } else {
            buyCoinPrice = buyCoinPrice / MILEAGE_PERCENTAGE; // 결제할 금액 다시 코인 기준으로 변경
        }

        return buyCoinPrice; // 마일리지를 차감 후 남은 결제 금액 return
    }

    /**
     * 마일리지 차감 & 마일리지 차감 로그 등록 (while)
     *
     * @param purchaseDto : buyCoinPrice : 결제할 금액(코인)
     */
    private void updateMileageUsedAndInsertUsedLog(PurchaseDto purchaseDto, int availableMileage) {

        long memberIdx = purchaseDto.getMemberIdx();
        long beforeIdx = 0;                              // 조회한 used.idx 값

        CoinDto restMileageDto;                          // rest_mileage 조회용

        CoinDto coinDto = CoinDto.builder()              // 회차 구매시 사용할 coinDto
                .memberIdx(purchaseDto.getMemberIdx())   // 회원 idx
                .title(purchaseDto.getTitle())           // 마일리지 사용한 컨텐츠 제목
                .regdate(dateLibrary.getDatetime())      // 등록일
                .build();

        // 사용 가능 마일리지가 0이 될 때까지 차감
        while (availableMileage > 0) {
            // 음수일 경우 이전 값 들어있으므로 초기화 후 진행
            coinDto.setSubResultCoin(null);

            /** 1. mileage_used 한개 로우에서 남은 마일리지 조회(idx & rest_mileage 조회) **/
            restMileageDto = coinDao.getRestMileageFromMileageUsed(memberIdx);
            coinDto.setIdx(restMileageDto.getIdx()); // 해당 row idx set
            coinDto.setMileageUsedIdx(restMileageDto.getIdx()); // mileage_used_log 등록용 set

            // 무한 루프 방지 : 이전 used.idx 값 가져오면 exception
            if (restMileageDto.getIdx() == null || beforeIdx == restMileageDto.getIdx()) {
                throw new CustomException(CustomError.PURCHASE_IDX_DUPLE_ERROR); // 회차구매에 실패하였습니다.
            }
            // 조회한 used.idx 값 전역 변수에 초기화 (무한 루프 방지용)
            beforeIdx = restMileageDto.getIdx();
            coinDto.setIdx(restMileageDto.getIdx()); // 해당 row idx set

            /** 2. restMileage 와 차감할 금액 coin 차감하여 음수인지 검사 **/
            int restMileage = restMileageDto.getRestMileage(); // mileage_used 테이블에서 남은 마일리지 조회(로우 하나)

            /** 3. 마일리지 퍼센티지로 구매 코인 - 마일리지 **/
            int subResult = restMileage - availableMileage; // 남은 마일리지 - 총 차감할 마일리지

            // 차감 결과 양수
            if (subResult > 0) {

                coinDto.setSubResultCoin(subResult);                  // 해당 로우 남은 마일리지 차감된 결과값으로 set
                // subResult(차감 결과값)를 rest_mileage 로 update
                coinDao.updateMileageUsed(coinDto);
                coinDto.setMileage(availableMileage);                 // mileage_used 테이블에 사용한 mileage set(4번에서 활용)
                availableMileage = 0;                                 // breakCondition
                // 차감 결과 음수
            } else if (subResult < 0) {

                availableMileage = availableMileage - restMileage;     // 차감할 마일리지 초기화
                coinDto.setSubResultCoin(0);                           // 해당 로우 남은 마일리지 0으로 set
                // subResult 를 0으로 rest_mileage 업데이트
                coinDao.updateMileageUsed(coinDto);
                coinDto.setMileage(restMileage);                       // mileage_used 테이블에 사용한 mileage set(4번에서 활용)
                // 차감 결과 0
            } else if (subResult == 0) {

                coinDto.setSubResultCoin(subResult);
                // subResult(차감 결과값)를 rest_mileage 로 update
                coinDao.updateMileageUsed(coinDto);
                coinDto.setMileage(restMileage);                       // mileage_used 테이블에 사용한 mileage set(4번에서 활용)
                availableMileage = 0;                                  // 값 = 0, breakCondition
            }

            /** 4. mileage_used_log 테이블 등록 **/
            coinDao.insertMileageUsedLog(coinDto);
        } // end of while

    }

    /**********************************************************************
     * 일반 코인, 보너스 코인 차감 로직(회차 구매, 대여 시)
     *********************************************************************/

    /**
     * 코인 or 보너스 코인 차감
     * coinType 에 따라 코인 또는 보너스 코인 차감 진행
     *
     * @param purchaseDto  : memberIdx, restCoin(남은 코인), restCoinFree(남은 보너스 코인), title(회차 제목)
     * @param coinType     : COIN(일반 코인), COIN_FREE(보너스 코인)
     * @param buyCoinPrice : 결제할 금액(코인)
     * @return resultRestBuyCoin : 남은 결제 금액
     */
    @Transactional
    public int subtractCoinOrCoinFree(PurchaseDto purchaseDto, int coinType, int buyCoinPrice) {

        int resultRestBuyCoin = 0;                           // 코인을 차감한 남은 결재금액(코인)
        purchaseDto.setCoinType(coinType);                   // ** 코인유형 : '1'(코인) , '2'(무료코인) **

        int totalRestCoin = getRestCoinByType(purchaseDto);  // 코인 or 보너스 코인 남은 금액 반환
        purchaseDto.setBuyCoinPrice(buyCoinPrice);

        int totalUsedCoin = 0;

        // 차감할 금액(코인)이 보유 코인보다 많은 경우
        if (buyCoinPrice > totalRestCoin) {
            // 코인일 경우 마지막에 차감하므로 결제 금액이 더 많으면 보유 코인이 부족
            if (coinType == COIN) {
                throw new CustomException(CustomError.HAVE_COIN_LACK);  // 보유 코인이 부족합니다.
            }
            // 리턴할 남은 결제금액(코인) 초기화
            resultRestBuyCoin = buyCoinPrice - totalRestCoin; // 결제할 코인 - 남은 코인
            totalUsedCoin = updateCoinUsedAndInsertUsedLog(purchaseDto); // 사용한 코인 반환
            // 차감할 금액(코인) 보다 보유 코인이 많은 경우
        } else {
            /** resultRestBuyCoin  0 리턴 **/
            totalUsedCoin = updateCoinUsedAndInsertUsedLog(purchaseDto);

        } // end of else

        // 회차 구매에 사용한 코인 or 보너스 코인 setting
        if (coinType == COIN) {
            purchaseDto.setCoin(totalUsedCoin);
        } else {
            purchaseDto.setCoinFree(totalUsedCoin);
        }
        purchaseDto.setCoinType(null); // 코인 유형 초기화(이후 로직에서 사용 될 수 있으므로 미리 초기화)

        return resultRestBuyCoin; // 남은 결제 금액
    }

    /**
     * 코인 차감 & 코인 차감 로그 등록 (while)
     *
     * @param purchaseDto : coinType: 코인 유형, buyCoinPrice: 결제 금액(코인)
     */
    private int updateCoinUsedAndInsertUsedLog(PurchaseDto purchaseDto) {

        long beforeIdx = 0;                                  // 조회한 used.idx 값
        int coinType = purchaseDto.getCoinType();            // 코인 타입(1: 코인, 2: 보너스 코인)
        int totalRestCoin = getRestCoinByType(purchaseDto);  // 코인 or 보너스 코인 남은 금액
        int buyCoinPrice = purchaseDto.getBuyCoinPrice();    // 결제 금액(코인)
        CoinDto restCoinDto;                                 // 남은 코인 조회 용 dto

        CoinDto coinDto = CoinDto.builder()
                .memberIdx(purchaseDto.getMemberIdx())       // 회원 idx
                .type(coinType)                              // 코인 유형
                .title(purchaseDto.getTitle())               // 코인 사용한 컨텐츠 제목
                .regdate(dateLibrary.getDatetime())          // 등록일
                .build();                                    // 회차 구매시 사용할 coinDto return

        /**
         * 1.  while 조건 문 변수 초기화 
         * -> 차감할 금액이 보유 코인보다 많으면 보유 코인이 기준. 보유 코인이 0 이 될 때까지 반복
         * -> 차감할 금액보다 보유코인이 많으면 차감할 금액이 기준. 차감할 금액이 0이 될 때까지 반복
         **/
        int subCoin = 0; // 차감할 코인
        int totalUsedCoin = 0;

        // 차감할 금액(코인)이 보유 코인보다 많은 경우
        if (buyCoinPrice > totalRestCoin) {
            subCoin = totalRestCoin;
            // 차감할 금액(코인) 보다 보유 코인이 많은 경우
        } else {
            subCoin = buyCoinPrice;
        }

        // 차감할 코인이 0이 될 때까지 반복
        while (subCoin > 0) {
            // 음수일 경우 이전 값 들어있으므로 초기화 후 진행
            coinDto.setSubResultCoin(null);  // db에 차감할 코인 초기화
            /** 1. coin_used 한개 로우에서 rest_coin 조회(idx & rest_coin 조회) **/
            restCoinDto = coinDao.getRestCoinAndIdxFromCoinUsed(coinDto);

            // 무한 루프 방지
            if (restCoinDto.getIdx() == null) {
                throw new CustomException(CustomError.PURCHASE_EPISODE_BUY_FAIL); // 회차 구매에 실패하였습니다.
            }
            if (beforeIdx == restCoinDto.getIdx()) { // 음수 였을 경우 이전에 조회한 used.idx 값과 같은 값이 조회 되었을 때
                throw new CustomException(CustomError.PURCHASE_IDX_DUPLE_ERROR); // 회차 구매에 실패하였습니다.
            }

            // 조회한 used.idx 값 저장(무한 루프 방지용)
            beforeIdx = restCoinDto.getIdx();

            /** 2. restCoin 이랑 차감할 금액 coin 차감하여 음수인지 검사(idx, restCoin 같이 들고 온다) **/
            int restCoin = restCoinDto.getRestCoin();
            coinDto.setIdx(restCoinDto.getIdx()); // 해당 row idx set
            coinDto.setCoinUsedIdx(restCoinDto.getIdx()); // coin_used_log 등록용 set

            /** 3. 남은 코인 - 차감할 코인 = subResult **/
            int subResult = restCoin - subCoin;

            // 차감 결과 양수
            if (subResult > 0) {

                totalUsedCoin += subCoin;
                coinDto.setSubResultCoin(subResult);   // 차감된 결과값 set
                // subResult(차감 결과값)를 rest_coin 으로 update
                coinDao.updateCoinUsed(coinDto);
                coinDto.setCoin(subCoin);            // coin_used 테이블에 사용한 coin set (4번에서 활용)
                subCoin = 0;                         // while 문 탈출 조건

                // 차감 결과 음수
            } else if (subResult < 0) {

                totalUsedCoin += restCoin;
                coinDto.setSubResultCoin(0);  // 남은 코인 0으로 set
                // 코인 or 무료 코인 update
                coinDao.updateCoinUsed(coinDto);
                coinDto.setCoin(restCoin); // coin_used 테이블에 사용한 coin set (4번에서 활용)
                subCoin = subCoin - restCoin; // 차감할 코인 초기화
                // 차감 결과 0
            } else if (subResult == 0) {

                totalUsedCoin += restCoin;
                coinDto.setSubResultCoin(0);          // 차감된 결과값 set
                // subResult(차감 결과값)를 rest_coin 으로 update
                coinDao.updateCoinUsed(coinDto);
                coinDto.setCoin(restCoin);            // coin_used 테이블에 사용한 coin set (4번에서 활용)
                subCoin = 0;                          // while 문 탈출 조건
            } // end of else if

            /** 4. coin_used_log 테이블 등록(코인 & 보너스 코인) **/
            coinDao.insertCoinUsedLog(coinDto);

        } // end of while
        return totalUsedCoin; // 회차 구매에 사용한 총 코인 return
    }

    /*********************************************************************
     * SUB
     *********************************************************************/

    /**
     * 회원 코인 & 마일리지 조회
     *
     * @param memberIdx : 회원 idx
     * @return coinDto : restCoin(잔여 코인), restMileage(잔여 마일리지)
     */
    public CoinDto getRestCoinAndRestMileage(Long memberIdx) {

        // 회원 잔여 코인 dto
        CoinDto restCoinDto = CoinDto.builder()
                .memberIdx(memberIdx)
                .nowDate(dateLibrary.getDatetime()).build();

        /** 회원 코인 정보 조회 (만료된 코인은 제외) **/
        CoinDto coinDto = coinDaoSub.getMemberCoinAndCoinFree(restCoinDto); // 코인 & 보너스 코인 조회
        Integer mileage = coinDaoSub.getMemberMileage(restCoinDto);         // 마일리지 조회

        restCoinDto.setRestCoin(coinDto.getCoin());         // 잔여 코인 set
        restCoinDto.setRestCoinFree(coinDto.getCoinFree()); // 잔여 보너스 코인 set
        restCoinDto.setRestMileage(mileage);                // 잔여 마일리지 set

        return restCoinDto;
    }

    /**
     * 남은 코인 반환(일반 코인 or 보너스 코인)
     *
     * @param purchaseDto : coinType(코인 유형), restCoin(남은 코인), restCoinFree(남은 보너스 코인)
     * @return
     */
    private int getRestCoinByType(PurchaseDto purchaseDto) {

        int restCoin = 0;

        // 코인
        if (purchaseDto.getCoinType() == COIN) {
            restCoin = purchaseDto.getRestCoin();
            // 보너스 코인
        } else if (purchaseDto.getCoinType() == COIN_FREE) {
            restCoin = purchaseDto.getRestCoinFree();
        }
        return restCoin;
    }

    /**
     * 전체 구매(소장 & 대여) member_purchase 코인 setting 메서드
     *
     * @param episodeCoinDto : idx(회차 idx), buyCoinPrice(한 회차 금액)
     * @param usedCoinDto    : usedCoin, usedCoinFree, usedMileage
     * @param purchaseDto    : buyCoinPrice(총 결제 금액)
     * @return
     */
    public PurchaseDto purchaseCoinCalculate(EpisodeDto episodeCoinDto, CoinDto usedCoinDto, PurchaseDto purchaseDto) {

        // 사용한 코인
        int usedCoin = usedCoinDto.getUsedCoin();
        int usedCoinFree = usedCoinDto.getUsedCoinFree();
        int usedMileage = usedCoinDto.getUsedMileage();

        // 회차 대여 및 소장 가격
        int episodeBuyCoin = episodeCoinDto.getBuyCoinPrice(); // 구매할 회차 금액

        int buyCoinPrice = purchaseDto.getBuyCoinPrice();      // 결제 할 총 금액

        // 결제할 금액 - 회차 구매 금액
        buyCoinPrice = buyCoinPrice - episodeBuyCoin;
        /** 결제 금액 - 회차 구매 금액 = 결제금액 갱신 **/
        purchaseDto.setBuyCoinPrice(buyCoinPrice);             // 차감한 결제 금액 set

        // purchase 에 등록할 dto set
        PurchaseDto memberPurchaseDto = PurchaseDto.builder()
                .memberIdx(purchaseDto.getMemberIdx())
                .contentsIdx(purchaseDto.getContentsIdx())
                .episodeIdx(episodeCoinDto.getIdx())
                .coin(0)
                .coinFree(0)
                .mileage(0)
                .usedTicket(0)
                .type(purchaseDto.getType())
                .title(purchaseDto.getTitle())
                .route(purchaseDto.getRoute())
                .buyAllIdx(purchaseDto.getBuyAllIdx())
                .regdate(purchaseDto.getRegdate())
                .expiredate(purchaseDto.getExpiredate())
                .build();

        /** 이후 로직부터는 memberPurchaseDto 에 들어갈 coin, coinFree, mileage 정보 계산 **/
        int subResult = 0; // 차감 결과 & 남은 회차 가격

        /** 마일리지 **/
        if (usedMileage > 0) {
            // 사용 마일리지 - 회차 구매 금액
            subResult = usedMileage - (episodeBuyCoin * MILEAGE_PERCENTAGE);

            // 양수거나 0 이면
            if (subResult >= 0) {
                memberPurchaseDto.setMileage(episodeBuyCoin * MILEAGE_PERCENTAGE); // 회차 구입에 사용한 마일리지 set
                usedCoinDto.setUsedMileage(usedMileage - (episodeBuyCoin * MILEAGE_PERCENTAGE));   // 총 사용 마일리지 set

                return memberPurchaseDto;
            }
            // 마일리지로 구매할 코인 부족(음수)
            usedCoinDto.setUsedMileage(0);                 // 총 사용 마일리지 -> 0 set
            memberPurchaseDto.setMileage(usedMileage);     // 회차 구입에 사용한 마일리지 -> 총 사용 마일리지 set
            episodeBuyCoin = ((episodeBuyCoin * MILEAGE_PERCENTAGE) - usedMileage) / MILEAGE_PERCENTAGE; // 회차 금액 -> 구매할 회차 금액 - 사용한 마일리지 set
            subResult = Math.abs(subResult) / MILEAGE_PERCENTAGE;
        } // end of 마일리지

        /** 보너스 코인 **/
        if (subResult == 0 && usedCoinFree > 0) {
            // 사용 보너스 코인 - 회차 구매 금액
            subResult = usedCoinFree - episodeBuyCoin;

            // 양수거나 0 이면
            if (subResult >= 0) {
                memberPurchaseDto.setCoinFree(episodeBuyCoin); // 회차 구입에 사용한 보너스 코인 -> 회차 금액으로 set
                usedCoinDto.setUsedCoinFree(subResult);        // 총 사용 보너스 코인 -> 사용한 보너스 코인 - 회차 구매 비용

                return memberPurchaseDto;
            }
            // 보너스 코인으로 구매할 코인 부족(음수)
            usedCoinDto.setUsedCoinFree(0);                 // 총 사용한 보너스 코인 -> 0 set
            memberPurchaseDto.setCoinFree(usedCoinFree);    // 회차 구입에 사용한 보너스 코인 -> 총 사용 보너스 코인 set
            episodeBuyCoin = episodeBuyCoin - usedCoinFree; // 회차 금액 -> 구매할 회차 금액 - 사용한 보너스 코인 set

            /** 보너스 코인 (마일리지로 구매 금액이 부족한 경우) **/
        } else if (subResult != 0 && usedCoinFree > 0) {
            // 음수를 양수로 변환
            subResult = Math.abs(subResult);

            // 보너스 코인 - 회차 남은 금액
            subResult = usedCoinFree - subResult;

            // 양수거나 0 이면
            if (subResult >= 0) {
                memberPurchaseDto.setCoinFree(episodeBuyCoin); // 회차 구입에 사용한 보너스 코인 -> 회차 금액으로 set
                usedCoinDto.setUsedCoinFree(subResult);        // 총 사용 보너스 코인 -> 사용한 보너스 코인 - 회차 구매 비용

                return memberPurchaseDto;
            }
            // 보너스 코인으로 구매할 코인 부족(음수)
            usedCoinDto.setUsedCoinFree(0);                 // 사용한 보너스 코인 0
            memberPurchaseDto.setCoinFree(usedCoinFree);    // 회차 구입에 사용한 보너스 코인-> 총 사용 보너스 코인 set
            episodeBuyCoin = episodeBuyCoin - usedCoinFree; // 회차 금액 -> 구매할 회차 금액 - 사용한 보너스 코인 set
        }

        /** 코인 **/
        if (subResult == 0 && usedCoin > 0) {
            // 사용 코인 - 회차 구매 금액
            subResult = usedCoin - episodeBuyCoin;

            // 양수거나 0 이면
            if (subResult >= 0) {
                memberPurchaseDto.setCoin(episodeBuyCoin); // 회차 구입에 사용한 코인
                usedCoinDto.setUsedCoin(subResult);        // 사용한 코인 - 회차 구매 비용

                return memberPurchaseDto;
            }
            // 음수면
            usedCoinDto.setUsedCoin(0);              // 사용한 코인 -> 0 set
            memberPurchaseDto.setCoin(usedCoin);     // 회차 구입에 사용한 코인-> 총 사용 코인 set

            /** 코인 (보너스 코인으로 구매 금액이 부족한 경우) **/
        } else if (subResult != 0 && usedCoin > 0) {
            usedCoin = usedCoin - subResult;

            memberPurchaseDto.setCoin(subResult);      // 회차 구입에 사용한 코인
            usedCoinDto.setUsedCoin(usedCoin);         // 총 사용 코인 -> 총 사용 코인 - 구매할 회차 금액 set
        }

        return memberPurchaseDto;
    }


    /*********************************************************************
     * 문자 변환 & 타입 지정
     *********************************************************************/

    private void stateText(List<CoinDto> coinDtoList) {
        for (CoinDto coinDto : coinDtoList) {
            stateText(coinDto);
        }
    }

    private void stateText(CoinDto coinDto) {
        if (coinDto.getType() != null) {
            // 일반 코인
            if (coinDto.getType() == 1) {
                coinDto.setTypeText(super.langMessage("lang.coin.type.coin"));      // 코인
                // 보너스 코인
            } else if (coinDto.getType() == 2) {
                coinDto.setTypeText(super.langMessage("lang.coin.type.bonusCoin")); // 보너스 코인
            } else if (coinDto.getType() == 3) {
                coinDto.setTypeText(super.langMessage("lang.coin.type.mileage"));   // 마일리지
            }
        }
    }

    /**
     * 코인 리스트 소멸 & 소멸 예정 문자변환
     *
     * @param coinDtoList
     */
    private void expireText(List<CoinDto> coinDtoList) {
        for (CoinDto coinDto : coinDtoList) {
            expireText(coinDto);
        }
    }

    /**
     * 소멸 & 소멸 예정 타입 지정 및 문자변환
     *
     * @param coinDto
     */
    private void expireText(CoinDto coinDto) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {

            Date nowDate = sdf.parse(dateLibrary.getDatetime()); // 오늘 날짜
            Date expireDate = sdf.parse(coinDto.getExpiredate());

            // 소멸
            if (nowDate.after(expireDate)) {
                coinDto.setExpireType(1); // 소멸
                coinDto.setExpireTypeText(super.langMessage("lang.coin.expire.already")); // 소멸
                // 소멸 예정
            } else if (nowDate.before(expireDate)) {
                coinDto.setExpireType(2); // 소멸 예정
                coinDto.setExpireTypeText(super.langMessage("lang.coin.expire.notYet")); // 소멸 예정

            }
        } catch (Exception e) {
            throw new CustomException(CustomError.SERVER_NORMAL_ERROR);
        }
    }

    /**
     * 마일리지 타입 지정 (3)
     *
     * @param coinDtoList
     */
    private void setMileageType(List<CoinDto> coinDtoList) {
        for (CoinDto coinDto : coinDtoList) {
            coinDto.setType(MILEAGE);
        }
    }


    /*********************************************************************
     * INSERT / UPDATE
     *********************************************************************/

    /**
     * 코인 & 무료 코인 & 마일리지 지급
     *
     * @param coinDto
     */
    @Transactional
    public void coinPayment(CoinDto coinDto) {

        // dto setting
        coinDto.setExpiredate(getExpireDate("COIN")); // 구매 코인 만료일
        coinDto.setRegdate(dateLibrary.getDatetime()); // 등록일(지급일)

        // 구매 코인, 보너스 코인, 마일리지 분기
        /** save & log 테이블 등록 **/
        if (coinDto.getMileage() > 0) { // 마일리지 지급 입력 시 등록

            // "1일 1회 로그인 마일리지"가 아닐 경우에만 만료일 30일로 세팅
            if (!coinDto.getPosition().isEmpty() && !coinDto.getPosition().equals("로그인")) {
                coinDto.setMileageExpireDate(getExpireDate("MILEAGE")); // 마일리지 만료일(30일) set
            }

            /** 2. member_mileage_save 등록 **/
            coinDao.insertMileageSave(coinDto);

            /** 3. member_mileage_save_log 등록 **/
            coinDao.insertMileageSaveLog(coinDto);

            /** 4. member_mileage_used 테이블 등록 **/
            coinDao.insertMileageUsed(coinDto);
        }

        if (coinDto.getCoin() > 0) { // 유료 코인 지급 입력 시 등록
            coinDto.setCoinType("coin"); // 유료 코인
            coinDto.setType(1); // 유료 코인
            coinDto.setState(1); // 상태
            coinDto.setRestCoin(coinDto.getCoin());
            coinDto.setCoinExpireDate(getExpireDate("COIN")); // 유료 코인 만료일 set

            /** 2. member_coin_save 등록 **/
            coinDao.insertCoinSave(coinDto);

            /** 3. member_coin_save_log 등록 **/
            coinDao.insertCoinSaveLog(coinDto);

            /** 4. member_coin_used 테이블 등록 **/
            coinDao.insertCoinUsed(coinDto);
        }

        if (coinDto.getCoinFree() > 0) { // 무료 코인 지급 입력 시 등록
            coinDto.setCoinType("coinFree"); // 무료 코인 타입
            coinDto.setType(2); // 무료 코인
            coinDto.setState(1); // 상태
            coinDto.setRestCoinFree(coinDto.getCoinFree());
            coinDto.setCoinFreeExpireDate(getExpireDate("COIN_FREE")); // 무료 코인 만료일 set
            /** 2. member_coin_save 등록 **/
            coinDao.insertCoinSave(coinDto);

            /** 3. member_coin_save_log 등록 **/
            coinDao.insertCoinSaveLog(coinDto);

            /** 4. member_coin_used 테이블 등록 **/
            coinDao.insertCoinUsedFree(coinDto);
        }

        //org.json.JSONObject data = new org.json.JSONObject(coinDto);
        //super.pushAlarm("플러스정보 : " + data,"LJH");

        //회원 코인 정보 업데이트
        coinDao.updateCoinPlus(coinDto);
    }

    /**
     * 코인 & 무료 코인 & 마일리지 만료일 구하는 메서드
     *
     * @param coinType : COIN : 코인 만료일, COIN_FREE : 무료 코인 만료일, MILEAGE : 마일리지 만료일
     * @return String 타입 : 만료일
     */
    private String getExpireDate(String coinType) {

        Calendar cal = Calendar.getInstance();
        Date expireDate = null;
        // 구매코인
        if (coinType.equals("COIN")) {
            // 5년 후
            cal.add(Calendar.YEAR, +5);
            cal.add(Calendar.HOUR, +1);
            expireDate = cal.getTime();
        }
        // 마일리지 & 보너스 코인
        else if (coinType.equals("MILEAGE") || coinType.equals("COIN_FREE")) {
            // 한달 후
            cal.add(Calendar.MONTH, +1);
            cal.add(Calendar.HOUR, +1);
            expireDate = cal.getTime();
        }
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:00:00");

        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(expireDate);
    }

    /**
     * 마일리지 지급
     */
    @Transactional
    public void addMileage(Long memberIdx, Integer mileage) {

        // 코인,마일리지 지급
        CoinDto coinDto = new CoinDto();
        coinDto.setMemberIdx(memberIdx);             // 회원번호
        coinDto.setTitle("꿀툰 가입 이벤트");           // 상품제목
        coinDto.setMileage(mileage);                 // 마일리지 지급
        coinDto.setPaymentIdx(0);                    // payment.idx
        coinDto.setPosition("이벤트");
        coinPayment(coinDto);
    }

    /**
     * 마일리지 지급 - 쿠폰이벤트
     */
    @Transactional
    public void addMileage(Long memberIdx, Integer mileage, String type, String title) {

        // 코인,마일리지 지급
        CoinDto coinDto = new CoinDto();
        coinDto.setMemberIdx(memberIdx);             // 회원번호
        coinDto.setTitle(title);                     // 상품제목
        coinDto.setMileage(mileage);                 // 마일리지 지급
        coinDto.setPaymentIdx(0);                    // payment.idx
        coinDto.setPosition(type);
        coinPayment(coinDto);
    }



    /**
     * 만료 코인 & 마일리지 update
     * 만료 로그 등록(coin_expire_log & mileage_expire_log)
     *
     * @param memberIdx
     */
    @Transactional
    public void updateExpireCoinAndMileage(Long memberIdx) {

        String nowDate = dateLibrary.getDatetime(); // 현재 시간
        
        CoinDto updateCoinDto = CoinDto.builder()
                .memberIdx(memberIdx)
                .nowDate(nowDate).build();

        /********************************** 코인 만료 ***********************************/

        // 업데이트 전 이미 만료된 코인 idxList 조회
        List<Long> coinIdxList = coinDaoSub.getExpireCoinIdxList(memberIdx);

        // 만료 코인 update
        coinDao.updateExpireCoin(updateCoinDto);
        // 업데이트 후 만료된 코인 idxList 조회
        List<CoinDto> coinExpireList = coinDao.getExpireCoinInfoList(memberIdx);

        // 업데이트 전 coin_expire_log 에 쌓인 기록은 제외
        if (coinIdxList != null && !coinIdxList.isEmpty()) {
            for (Long idx : coinIdxList) {
                coinExpireList.removeIf(coinDto -> coinDto.getIdx() == idx);
            }
        }

        if (coinExpireList != null && !coinExpireList.isEmpty()) {
            // 만료 코인 로그 등록용 리스트
            List<CoinDto> expireCoinLogList = new ArrayList<>();

            // expire_coin_log 등록 할 데이터 set
            for (CoinDto coinExpireDto : coinExpireList) {
                CoinDto coinDto = CoinDto.builder()
                        .memberIdx(memberIdx)
                        .coinUsedIdx(coinExpireDto.getIdx())
                        .coin(coinExpireDto.getCoin())
                        .restCoin(coinExpireDto.getRestCoin())
                        .type(coinExpireDto.getType())
                        .state(1)
                        .regdate(nowDate)
                        .build();

                // 등록할 리스트에 dto 추가
                expireCoinLogList.add(coinDto);
            }
            /** 만료된 코인 로그 insert **/
            coinDao.insertExpireCoinLog(expireCoinLogList);

            // 4. member_coin 업데이트
            CoinDto coinDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .type(COIN) // 코인 set
                    .nowDate(nowDate).build();

            // 회원 잔여 코인 조회
            CoinDto restCoinDto = coinDaoSub.getMemberCoinAndCoinFree(coinDto);

            // 회원 잔여 보너스 코인 조회
            coinDto.setType(COIN_FREE); // 보너스 코인 set
            CoinDto restCoinFreeDto = coinDaoSub.getMemberCoinAndCoinFree(coinDto);

            restCoinDto.setCoinFree(restCoinFreeDto.getCoinFree());// 보너스 코인 set
            restCoinDto.setMemberIdx(memberIdx);                   // 회원 idx set

            /** 회원 코인 & 보너스 코인 업데이트 **/
            coinDao.updateMemberCoinAndCoinFree(restCoinDto);
        }

        /********************************** 마일리지 만료 ************************************/

        // 업데이트 전 이미 만료된 코인 idxList 조회
        List<Long> mileageIdxList = coinDaoSub.getExpireMileageIdxList(memberIdx);

        // 만료 마일리지 update
        coinDao.updateExpireMileage(updateCoinDto);
        // 업데이트 후 만료된 코인 정보(idx, mileage, rest_mileage) 조회
        List<CoinDto> mileageExpireList = coinDao.getExpireMileageInfoList(memberIdx);

        // 업데이트 전 coin_expire_log 에 쌓인 기록은 제외
        if (mileageIdxList != null && !mileageIdxList.isEmpty()) {
            for (Long idx : coinIdxList) {
                coinExpireList.removeIf(coinDto -> coinDto.getIdx() == idx);
            }
        }

        if (mileageExpireList != null && !mileageExpireList.isEmpty()) {
            // 만료 마일리지 등록용 리스트
            List<CoinDto> expireMileageLogList = new ArrayList<>();

            // expire_coin_log 등록 할 데이터 set
            for (CoinDto mileageExpireDto : mileageExpireList) {
                CoinDto mileageDto = CoinDto.builder()
                        .memberIdx(memberIdx)
                        .mileageUsedIdx(mileageExpireDto.getIdx())
                        .mileage(mileageExpireDto.getMileage())
                        .restMileage(mileageExpireDto.getRestMileage())
                        .state(1)
                        .regdate(nowDate)
                        .build();

                // 등록할 리스트에 dto 추가
                expireMileageLogList.add(mileageDto);
            }

            /** 만료된 마일리지 로그 등록 **/
            coinDao.insertExpireMileageLog(expireMileageLogList);

            // 4. member_coin 업데이트
            CoinDto coinDto = CoinDto.builder()
                    .memberIdx(memberIdx)
                    .nowDate(nowDate).build();

            // 회원 마일리지 조회
            Integer restMileage = coinDaoSub.getMemberMileage(coinDto);
            coinDto.setMileage(restMileage);  // 잔여 마일리지 set

            /** 회원 마일리지 update **/
            coinDao.updateMileageFromMemberCoin(coinDto);
        }

    }
}
