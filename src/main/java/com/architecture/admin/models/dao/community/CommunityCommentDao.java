package com.architecture.admin.models.dao.community;

import com.architecture.admin.models.dto.community.CommunityCommentDto;

public interface CommunityCommentDao {


    /**
     * 다음 댓글 group_idx 조회
     *
     * @return
     */
    long getNextGroupIdx();

    /**
     * 댓글 & 대댓글 등록
     *
     * @param commentDto
     * @return
     */
    int registComment(CommunityCommentDto commentDto);

    /**
     * 부모 댓글 정보 조회(대댓글 등록시)
     *
     * @param parentIdx
     * @return
     */
    CommunityCommentDto getParentCommentInfo(Long parentIdx);

    /**
     * 댓글 수 업데이트
     *
     * @param updateCommentDto
     * @return
     */
    int updateCommentCnt(CommunityCommentDto updateCommentDto);

    /**
     * 댓글 삭제
     *
     * @param commentDto
     * @return
     */
    int deleteComment(CommunityCommentDto commentDto);

    /**
     * 대댓글 삭제
     *
     * @param idx
     * @return
     */
    int deleteReplyComment(long idx);

    /**
     * 대댓글 개수
     *
     * @param parentIdx
     * @return
     */
    int getReplyCommentCntByIdx(long parentIdx);

    /**
     * 좋아요 테이블 insert
     *
     * @param commentDto
     */
    int insertMemCommentLike(CommunityCommentDto commentDto);

    /**
     * 댓글 좋아요 개수 조회
     *
     * @param commentDto
     * @return
     */
    int getCommentLikeCnt(CommunityCommentDto commentDto);

    /**
     * 댓글 좋아요 수 업데이트
     *
     * @param commentDto
     * @return
     */
    int updateCommentLikeCnt(CommunityCommentDto commentDto);

    /**
     * 댓글 좋아요 업데이트
     *
     * @param commentDto
     */
    int updateMemCommentLike(CommunityCommentDto commentDto);

    /**
     * 댓글 신고하기
     *
     * @param commentDto
     * @return
     */
    int insertCommentReport(CommunityCommentDto commentDto);
}
