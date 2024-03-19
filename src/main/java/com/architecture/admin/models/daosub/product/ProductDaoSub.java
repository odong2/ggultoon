package com.architecture.admin.models.daosub.product;

import com.architecture.admin.models.dto.product.ProductDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ProductDaoSub {

    /**
     * 상품목록
     *
     * @param productDto
     * @return
     */
    List<ProductDto> getProductList(ProductDto productDto);

    /**
     * 상품정보
     *
     * @param productDto
     * @return
     */
    ProductDto getProduct(ProductDto productDto);
    /**
     * 상품 결제수단 정보
     *
     * @param productIdx
     * @return
     */
    List<String> getMethodList(Integer productIdx);
}
