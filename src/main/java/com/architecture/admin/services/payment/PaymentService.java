package com.architecture.admin.services.payment;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.models.daosub.payment.PaymentDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.payment.PaymentDto;
import com.architecture.admin.models.dto.payment.PaymentMethodDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService extends BaseService {
    private final PaymentDaoSub paymentDaoSub;

    /********************************************************************************
     * SELECT
     ********************************************************************************/
    /**
     * 회원 결제내역 리스트 조회
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getPaymentList(SearchDto searchDto) {

        // 코인 + 마일리지 개수 조회 (전체 검색)
        int totalCnt = paymentDaoSub.getPaymentTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        // return value
        JSONObject jsonData = new JSONObject();
        List<PaymentDto> paymentDtoList = null;

        if (totalCnt > 0) {
            // 결제 리스트 조회
            paymentDtoList = paymentDaoSub.getPaymentList(searchDto);
            // 문자 변환
            stateText(paymentDtoList);
            jsonData.put("params", new JSONObject(searchDto)); // 페이징
        }

        jsonData.put("paymentList", paymentDtoList); // 리스트 set

        return jsonData;
    }

    /**
     * 사용 결제 수단 출력
     *
     * @param paymentMethodDto
     * @return
     */
    public List<PaymentMethodDto> getPaymentMethodList(PaymentMethodDto paymentMethodDto) {

        return paymentDaoSub.getPaymentMethodList(paymentMethodDto);
    }

    /**
     * 사용 결제 수단 정보
     *
     * @param paymentMethodDto
     * @return
     */
    public PaymentMethodDto getPaymentMethod(PaymentMethodDto paymentMethodDto) {

        return paymentDaoSub.getPaymentMethod(paymentMethodDto);
    }

    /********************************************************************************
     * SUB
     ********************************************************************************/

    /**
     * 문자변환 List(결제)
     *
     * @param paymentDtoList
     */
    private void stateText(List<PaymentDto> paymentDtoList) {
        for (PaymentDto paymentDto : paymentDtoList) {
            stateText(paymentDto);
            payTypeText(paymentDto);
        }
    }

    /**
     * 문자변환 dto(결제)
     *
     * @param payment
     */
    private void stateText(PaymentDto payment) {
        if (payment.getState() != null) {
            if (payment.getState() == 0) {
                payment.setStateText(super.langMessage("lang.payment.state.cancel")); // 결제 취소
            } else if (payment.getState() == 1) {
                payment.setStateText(super.langMessage("lang.payment.state.normal")); // 결제 정상
            }
        }
    }

    /**
     * 결제수단 문자변환 dto(결제)
     *
     * @param payment
     */
    private void payTypeText(PaymentDto payment) {
        Map<String, String> map = new HashMap<>() {{
            put("CA", "신용카드");
            put("RA", "계좌이체");
            put("VA", "가상계좌");
            put("MP", "휴대폰");
            put("TC", "틴캐시");
            put("HM", "해피머니");
            put("CG", "컬쳐랜드");
            put("SG", "스마트문상");
            put("BG", "도서상품권");
            put("TM", "티머니");
            put("CP", "포인트다모아");
            put("NVP", "네이버페이");
            put("KKP", "카카오페이");
            put("SSP", "삼성페이");
        }};
        if(Objects.equals(payment.getPayType(), "PZ")) {
            payment.setPayTypeText(map.get(payment.getPayMethod()));
        }else{
            payment.setPayTypeText(map.get(payment.getPayType()));
        }
    }
}
