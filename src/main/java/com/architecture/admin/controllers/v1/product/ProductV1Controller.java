package com.architecture.admin.controllers.v1.product;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.product.ProductDto;
import com.architecture.admin.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/product")
public class ProductV1Controller extends BaseController {

    private final ProductService productService;

    /**
     * 상품 리스트 가져오기
     * @param productDto
     * @return
     * @throws Exception
     */
    @RequestMapping("list")
    public String product(ProductDto productDto) {

        // 상품 리스트
        JSONObject data = productService.getProductList(productDto);
        
        // 결과 메세지 처리
        String sErrorMessage = "lang.common.success.search"; // 조회를 완료하였습니다.
        String message = super.langMessage(sErrorMessage);
        
        return displayJson(true, "1000", message, data);
    }

    /**
     * 상품 정보 가져오기
     * @param productDto
     * @return
     * @throws Exception
     */
    @RequestMapping("info")
    public String info(ProductDto productDto) {

        productDto.setIdx(2);

        JSONObject data = new JSONObject();
        data.put("list",productService.getProduct(productDto));

        String sErrorMessage = "lang.common.success.search"; // 조회를 완료하였습니다.
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }

    @RequestMapping("method/{idx}")
    public String method(@PathVariable(name = "idx") Integer productIdx) {

        JSONObject data = new JSONObject();

        data.put("list",productService.getMethodList(productIdx));



        String sErrorMessage = "lang.common.success.search"; // 조회를 완료하였습니다.
        String message = super.langMessage(sErrorMessage);
        return displayJson(true, "1000", message, data);
    }
}