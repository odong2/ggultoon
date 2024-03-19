package com.architecture.admin.models.daosub.community;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.community.CommunityContentDto;
import com.architecture.admin.models.dto.community.CommunityImageDto;
import com.architecture.admin.models.dto.content.ContentDto;

import java.util.List;

public interface CommunityDaoSub {

    /**
     * 유효한 게시물인지 조회
     *
     * @param idx : 커뮤니티 게시물 idx
     * @return
     */
    int getContentsCnt(Long idx);

    /**
     * 게시물 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getContentsTotalCnt(SearchDto searchDto);

    /**
     * 게시물 신고한적 있는 지 조회
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx)
     * @return
     */
    int getReportCnt(CommunityContentDto contentDto);

    /**
     * 게시물 좋아요 한적 있는 지 조회
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx)
     * @return
     */
    Integer getMemContentLikeState(CommunityContentDto contentDto);

    /**
     * 커뮤니티 게시물 조회
     *
     * @param searchDto
     * @return
     */
    List<CommunityContentDto> getContentList(SearchDto searchDto);

    /**
     * 게시물 신고 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getContentsReportList(Long memberIdx);

    /**
     * 커뮤니티 게시물 상세 조회
     *
     * @param searchDto : idx(커뮤니티 게시물 idx), memberIdx(회원 idx)
     * @return
     */
    CommunityContentDto getContentInfo(SearchDto searchDto);

    /**
     * 컨텐츠 상세 조회
     *
     * @param contentDto : idx(컨텐츠 idx), nowDate(현재 시간)
     * @return
     */
    ContentDto getContent(ContentDto contentDto);

    /**
     * 게시물 성인 여부 조회
     *
     * @param idx
     * @return
     */
    int getContentsAdult(Long idx);

    /**
     * 게시물 작성자 조회
     *
     * @param idx
     * @return
     */
    Long getContentWriterIdx(Long idx);

    /**
     * 커뮤니티 이미지 조회
     *
     * @param idx
     * @return
     */
    CommunityImageDto getContentImage(Long idx);
}
