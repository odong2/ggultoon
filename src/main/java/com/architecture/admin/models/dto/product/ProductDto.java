package com.architecture.admin.models.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {

    /**
     * Product 테이블 컬럼
     **/
    private Integer idx;        // Product.idx
    private String title;       // 상품명
    private Integer type;       // 상품 구분 (1:첫결제, 2:재결제, 3:장기미결제)
    private String method;      // 결제수단
    private Integer coin;       // 지급코인
    private Integer coinFree;   // 지급코인
    private Integer coinFree2;  // 지급코인
    private Integer mileage;    // 마일리지
    private String price;       // 결제금액
    private String regdate;     // 등록일

}
