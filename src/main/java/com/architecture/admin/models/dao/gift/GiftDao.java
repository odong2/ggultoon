package com.architecture.admin.models.dao.gift;

import com.architecture.admin.models.dto.gift.GiftDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GiftDao {

    /*****************************************************
     * INSERT
     ****************************************************/
    /**
     * 회원 선물 지급
     * save 테이블 등록
     * @param memberGiftList : 회원이 받을 수 있는 선물 리스트
     */
    void insertMemberGiftSave(List<GiftDto> memberGiftList);

    /**
     * 회원 선물 지급
     * save_log 테이블 등록
     * @param memberGiftList : 회원이 받을 수 있는 선물 리스트
     */
    void insertMemberGiftSaveLog(List<GiftDto> memberGiftList);

    /**
     * 회원 선물 지급
     * used 테이블 등록
     * @param memberGiftList : 회원이 받을 수 있는 선물 리스트
     */
    void insertMemberGiftUsed(List<GiftDto> memberGiftList);

    /**
     * 선물 지급 내용 통계 반영
     * 통계 데이터 신규 등록
     * @param giftDto : 회원이 실제 지급 받은 선물
     */
    void insertGiftGiveCnt(GiftDto giftDto);

    /**
     * 회원 선물 사용
     * used 테이블 잔여 개수 업데이트
     *
     * @param giftDto
     * @return usedIdx
     */
    void updateMemberGiftUsed(GiftDto giftDto);

    /**
     * 회원 선물 사용
     * used_log 테이블 등록
     * @param giftDto
     */
    void insertMemberGiftUsedLog(GiftDto giftDto);

    /**
     * 선물 사용 내용 통계 반영
     * 통계 데이터 신규 등록
     * @param giftDto
     */
    void insertGiftUseCnt(GiftDto giftDto);
}
