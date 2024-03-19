package com.architecture.admin.models.dao.event;

import com.architecture.admin.models.dto.purchase.PurchaseDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface EventDao {

    /**
     * 전작품 무료 감상 이벤트
     * 이벤트 참여 내역 등록
     * (24.02.05 ~ 24.02.12)
     *
     * @param purchaseDto 이벤트 참여 정보
     */
    void insertFreeViewInfo(PurchaseDto purchaseDto);

    /**
     * 전작품 무료 감상 이벤트
     * 이벤트 참여 통계 집계
     * (24.02.05 ~ 24.02.12)
     *
     * @param purchaseDto 이벤트 참여 정보
     */
    void insertFreeViewStat(PurchaseDto purchaseDto);
}
