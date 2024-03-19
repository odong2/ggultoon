package com.architecture.admin.models.dao.purchase;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.purchase.PurchaseBuyAllDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface PurchaseDao {

    /**
     * 회차 구매(등록)
     *
     * @param purchaseDto
     */
    void insertMemberPurchase(PurchaseDto purchaseDto);

    /**
     * 회차 구매내역 삭제
     *
     * @param purchaseDto
     */
    int deleteMemberUsedCoinList(PurchaseDto purchaseDto);


    /**
     * 대여 및 소장 작품 리스트 삭제
     *
     * @param searchDto
     * @return
     */
    int deleteMemberPurchaseList(SearchDto searchDto);

    /**
     * 회원 전체 구매 등록
     *
     * @param purchaseBuyAllDto
     */
    void insertMemberBuyAll(PurchaseBuyAllDto purchaseBuyAllDto);

    /**
     * 회원 구매 리스트 등록
     *
     * @param memberPurchaseList
     */
    void insertMemberPurchaseList(List<PurchaseDto> memberPurchaseList);
}
