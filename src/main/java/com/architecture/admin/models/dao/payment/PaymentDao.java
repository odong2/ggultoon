package com.architecture.admin.models.dao.payment;

import com.architecture.admin.models.dto.payment.PaymentAppDto;
import com.architecture.admin.models.dto.payment.PaymentDto;
import com.architecture.admin.models.dto.payment.PaymentNotiDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface PaymentDao {
    // 결제결과 입력
    int insertPayLog(PaymentNotiDto paymentNotiDto);
    // 결제 테이블 입력
    int insertPayment(PaymentDto paymentDto);

    // paymentInfo 테이블 입력
    int insertPaymentInfo(PaymentDto paymentDto);

    // paymentApp 테이블 입력
    int insertPaymentApp(PaymentAppDto paymentAppDto);
}
