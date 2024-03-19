package com.architecture.admin.models.dao.coin;

import com.architecture.admin.models.dto.coin.CoinDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CoinDao {

    /**************************************************************
     * SELECT
     **************************************************************/

    /**
     * 남은 코인 & coin_used.idx 값 구하기(차감 시 사용)
     *
     * @param coinDto
     * @return : 1.coin_used.idx / 2.rest_coin
     */

    CoinDto getRestCoinAndIdxFromCoinUsed(CoinDto coinDto);

    /**
     * 남은 마일리지 조회 & mileage_used.idx 값 구하기(차감 시 사용)
     *
     * @param memberIdx
     * @return : 1. mileage_used.idx / 2. rest_mileage
     */
    CoinDto getRestMileageFromMileageUsed(long memberIdx);

    /**
     * 만료된 코인 정보 조회
     *
     * @param memberIdx
     * @return coinDto : idx(used_idx), coin(코인), rest_coin(남은 코인), type(코인 유형)
     */
    List<CoinDto> getExpireCoinInfoList(Long memberIdx);

    /**
     * 만료된 마일리지 정보 조회
     *
     * @param memberIdx
     * @return coinDto : idx(used_idx), mileage(마일리지), rest_mileage(남은 마일리지)
     */
    List<CoinDto> getExpireMileageInfoList(Long memberIdx);

    /**************************************************************
     * INSERT
     **************************************************************/

    /**
     * coin_used_log 테이블 등록
     *
     * @param coinDto
     */
    void insertCoinUsedLog(CoinDto coinDto);

    /**
     * mileage_used_log 테이블 등록
     *
     * @param coinDto
     */
    void insertMileageUsedLog(CoinDto coinDto);

    /**
     * member_coin 등록
     **/
    void insertMemberCoin(CoinDto coinDto);

    /**
     * coin_used 테이블 insert
     *
     * @param benefitCoinDto
     */
    void insertCoinUsed(CoinDto benefitCoinDto);

    /**
     * coin_used 테이블 insert - 무료코인
     *
     * @param coinDto
     */
    void insertCoinUsedFree(CoinDto coinDto);

    /**
     * member_coin_save_log 등록
     **/
    void insertCoinSaveLog(CoinDto coinDto);

    /**
     * member_coin_save 등록
     **/
    void insertCoinSave(CoinDto coinDto);

    /**
     * member_mileage_used 테이블 등록
     **/
    void insertMileageUsed(CoinDto coinDto);

    /**
     * member_mileage_save_log 등록
     **/
    void insertMileageSaveLog(CoinDto coinDto);

    /**
     * member_mileage_save 등록
     **/
    void insertMileageSave(CoinDto coinDto);

    /**
     * 만료된 코인 로그 등록
     *
     * @param insertExpireCoinLogList
     */
    void insertExpireCoinLog(List<CoinDto> insertExpireCoinLogList);

    /**
     * 만료된 마일리지 로그 등록
     *
     * @param insertExpireCoinLogList
     */
    void insertExpireMileageLog(List<CoinDto> insertExpireCoinLogList);


    /**********************************************************
     * UPDATE
     **********************************************************/

    /**
     * 회원 코인 & 마일리지 update [member_coin 테이블]
     *
     * @param coinDto
     * @return
     */
    int updateMemberCoin(CoinDto coinDto);

    /**
     * coin_used 테이블 update(차감된 결과값으로 남은 코인 update)
     *
     * @param coinDto : 1.memberIdx /  2.idx(coin_used.idx) /  3.subResultCnt 이용
     */
    void updateCoinUsed(CoinDto coinDto);

    /**
     * mileage_used 테이블 update(차감된 결과 값으로 남은 마일리지 update)
     *
     * @param coinDto
     */
    void updateMileageUsed(CoinDto coinDto);


    /**
     * member_coin 정보 update
     **/
    void updateCoinPlus(CoinDto coinDto);

    /**
     * 만료된 코인 update
     *
     * @param coinDto
     */
    void updateExpireCoin(CoinDto coinDto);

    /**
     * 만료된 마일리지 update
     *
     * @param coinDto
     */
    void updateExpireMileage(CoinDto coinDto);

    /**
     * 코인 & 보너스 코인 update
     *
     * @param restCoinDto
     */
    void updateMemberCoinAndCoinFree(CoinDto restCoinDto);

    /**
     * 회원 마일리지 update
     *
     * @param coinDto
     */
    void updateMileageFromMemberCoin(CoinDto coinDto);

    /**
     * 1일 1회 로그인 마일리지 지급
     *
     * @param loginMileage
     */
    int insertMemberLoginMileage(CoinDto loginMileage);
}

