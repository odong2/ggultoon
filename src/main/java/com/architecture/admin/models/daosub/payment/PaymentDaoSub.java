package com.architecture.admin.models.daosub.payment;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.payment.PaymentDto;
import com.architecture.admin.models.dto.payment.PaymentMethodDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface PaymentDaoSub {

    /**
     * 회원 결제내역 개수
     *
     * @param searchDto
     * @return
     */
    int getPaymentTotalCnt(SearchDto searchDto);

    /**
     * 회원 결재내역 리스트
     *
     * @param searchDto
     * @return
     */
    List<PaymentDto> getPaymentList(SearchDto searchDto);

    /**
     * 정상 결제수단 리스트
     *
     * @param paymentMethodDto
     * @return
     */
    List<PaymentMethodDto> getPaymentMethodList(PaymentMethodDto paymentMethodDto);

    /**
     * 정상 결제수단 정보
     *
     * @param paymentMethodDto
     * @return
     */
    PaymentMethodDto getPaymentMethod(PaymentMethodDto paymentMethodDto);

    /**
     * 정상 noti결제수단 정보
     *
     * @param paymentMethodDto
     * @return
     */
    PaymentMethodDto getPaymentMethodNoti(PaymentMethodDto paymentMethodDto);


    /**
     * 정상 noti결제수단 정보
     *
     * @param paymentMethodDto
     * @return
     */
    PaymentMethodDto getPaymentMethodKey(PaymentMethodDto paymentMethodDto);


    /**
     * 결제 정보 가져오기
     *
     * @param paymentDto
     * @return
     */
    PaymentDto getPayment(PaymentDto paymentDto);

    /**
     * 회원 결제 내역 개수 조회 (첫 결제 여부)
     * @param memberIdx
     * @return
     */
    int getPaymentCnt(Long memberIdx);

    /**
     * 회원 결제 내역 조회 (최근 30일)
     * @param memberIdx
     * @return
     */
    List<PaymentDto> getMemberPaymentList(Long memberIdx);
}
