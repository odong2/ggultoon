package com.architecture.admin.services.product;

import com.architecture.admin.models.daosub.payment.PaymentDaoSub;
import com.architecture.admin.models.daosub.product.ProductDaoSub;
import com.architecture.admin.models.dto.payment.PaymentDto;
import com.architecture.admin.models.dto.product.ProductDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.architecture.admin.config.SessionConfig.IDX;
import static com.architecture.admin.config.SessionConfig.MEMBER_INFO;

@Service
@RequiredArgsConstructor
public class ProductService extends BaseService {
    private final ProductDaoSub productDaoSub;
    private final PaymentDaoSub paymentDaoSub;

    /********************************************************************************
     * SELECT
     ********************************************************************************/

    /**
     * 결제 상품 리스트 (타입별로 구분해서 반환)
     * 1. 첫결제 : 비로그인 상태 OR 결제 내역이 없는 회원
     * 2. 재결제 : 결제 내역이 1건 이상 있는 회원
     * 3. 장기미결제 : 결제 내역이 1건 이상 있지만 마지막 충전 일자가 30일 이상 경과한 회원 >> 첫결제와 동일한 리스트 반환
     *
     * @param productDto
     * @return
     */
    public JSONObject getProductList(ProductDto productDto) {

        // return value
        JSONObject data = new JSONObject();

        // 세션 정보 가져오기
        Object memberInfo = session.getAttribute(MEMBER_INFO);

        // 결제 상품 리스트 타입 문자변환
        int type;

        if (memberInfo == null) { // 비로그인
            productDto.setType(1); // 첫결제 set
            type = 1; // 첫결제 set

        } else { // 로그인

            // 세션에서 회원 idx 가져오기
            Long memberIdx = Long.valueOf(getMemberInfo(IDX));

            // 회원 결제 정보 가져오기
            int memberPaymentCnt = paymentDaoSub.getPaymentCnt(memberIdx);

            if (memberPaymentCnt < 1) { // 결제 내역이 없는 회원일 경우
                productDto.setType(1); // 첫결제 set
                type = 1; // 첫결제 set

            } else { // 결제 내역이 1건 이상 있는 회원일 경우

                // 결제 내역 조회 (최근 30일)
                List<PaymentDto> memberPaymentList = paymentDaoSub.getMemberPaymentList(memberIdx);

                // 최근 30일 동안의 결제 내역이 없는 경우
                if (memberPaymentList.isEmpty()) {
                    productDto.setType(3); // 장기미결제 set
                    type = 3; // 장기미결제 set

                // 최근 30일 동안의 결제 내역이 있는 경우
                } else {
                    productDto.setType(2); // 재결제 set
                    type = 2; // 재결제 set
                }
            }
        }
        // type 담기
        data.put("type", type);
        // list 담기
        data.put("list", productDaoSub.getProductList(productDto));
        return data;
    }
    /**
     * 상품 정보
     *
     * @param productDto
     * @return
     */
    public ProductDto getProduct(ProductDto productDto) {

        return productDaoSub.getProduct(productDto);
    }

    /**
     * 상품 결제수단 정보
     *
     * @param productIdx
     * @return
     */
    public List<String> getMethodList(Integer productIdx) {

        return productDaoSub.getMethodList(productIdx);
    }
}
