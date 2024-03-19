package com.architecture.admin.models.dao.content;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.ContentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentDao {

    /**
     * 관심 상품 리스트 삭제
     *
     * @param searchDto
     * @return
     */
    int deleteMemberFavoriteList(SearchDto searchDto);

    /**
     * 작품 조회수 +1
     *
     * @param contentDto
     * @return
     */
    void updateViewCnt(ContentDto contentDto);

    /**
     * 작품 찜하기
     *
     * @param contentDto
     * @return
     */
    int favoriteContent(ContentDto contentDto);

    /**
     * 작품 찜 개수 업데이트
     *
     * @param contentDto
     * @return
     */
    void updateFavoriteCnt(ContentDto contentDto);

    /**
     * 작품 찜하기 취소
     *
     * @param contentDto
     * @return
     */
    int deleteFavoriteContent(ContentDto contentDto);

    /**
     * 작품 신고하기
     *
     * @param contentDto
     * @return
     */
    int insertContentReport(ContentDto contentDto);

    /**
     * 뷰 카운트 및 구매 수 증가
     *
     * @param contentInfo
     */
    void updateViewCntAndPurchase(ContentDto contentInfo);
}
