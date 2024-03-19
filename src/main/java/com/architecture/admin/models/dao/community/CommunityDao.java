package com.architecture.admin.models.dao.community;

import com.architecture.admin.models.dto.community.CommunityContentDto;

import java.util.HashMap;
import java.util.List;

public interface CommunityDao {

    /**
     * 커뮤니티 게시물 상세 정보
     *
     * @param idx : 커뮤니티 게시물 idx
     * @return : viewCnt(조회 수), likeCnt(좋아요 수), commentCnt(댓글 수), reportCnt(신고 수)
     */
    CommunityContentDto getContentsInfoByIdx(Long idx);


    /***************************************************************************
     * Insert
     ***************************************************************************/

    /**
     * 게시물 신고하기
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx)
     * @return
     */
    int insertContentsReport(CommunityContentDto contentDto);

    /**
     * 게시물 좋아요 등록
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx), regdate(등록일), state(1)
     */
    int insertMemContentsLike(CommunityContentDto contentDto);

    /***************************************************************************
     * Update
     ***************************************************************************/

    /**
     * 댓글 수 업데이트
     *
     * @param communityDto : communityContentsIdx(
     * @return
     */
    int updateContentsCommentCnt(CommunityContentDto communityDto);

    /**
     * 게시물 좋아요 업데이트
     *
     * @param contentDto
     */
    int updateMemContentsLike(CommunityContentDto contentDto);

    /**
     * 게시물 좋아요 수 업데이트
     *
     * @param contentDto
     * @return
     */
    int updateContentsLikeCnt(CommunityContentDto contentDto);

    /**
     * 커뮤니티 이미지 등록
     *
     * @param uploadResponse
     */
    int registerImage(List<HashMap<String, Object>> uploadResponse);

    /**
     * 커뮤니티 게시물 등록
     *
     * @param contentDto
     * @return
     */
    int insertContents(CommunityContentDto contentDto);

    /**
     * 게시물 상세 등록
     *
     * @param contentDto
     * @return
     */
    int insertContentsInfo(CommunityContentDto contentDto);

    /**
     * 게시물 삭제
     *
     * @param idx
     * @return
     */
    int deleteContent(Long idx);

    /**
     * 게시물 상세 삭제
     *
     * @param idx
     * @return
     */
    int deleteContentInfo(Long idx);

    /**
     * 커뮤니티 게시물 이미지 삭제
     *
     * @param idx
     * @return
     */
    int deleteContentImage(Long idx);
}
