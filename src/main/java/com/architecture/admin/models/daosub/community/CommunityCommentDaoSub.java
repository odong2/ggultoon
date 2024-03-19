package com.architecture.admin.models.daosub.community;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.community.CommunityCommentDto;

import java.util.List;

public interface CommunityCommentDaoSub {

    /**
     * 댓글 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getCommentsTotalCnt(SearchDto searchDto);

    /**
     * 대댓글 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getReplyCommentTotalCnt(SearchDto searchDto);


    /**
     * 댓글 리스트 조회 (비로그인 시)
     *
     * @param searchDto
     * @return
     */
    List<CommunityCommentDto> getCommentList(SearchDto searchDto);


    /**
     * 댓글 리스트 조회 (로그인 시)
     *
     * @param searchDto
     * @return
     */
    List<CommunityCommentDto> getCommentListLogin(SearchDto searchDto);

    /**
     * 대댓글 리스트 조회 (비로그인 시)
     *
     * @param searchDto
     * @return
     */
    List<CommunityCommentDto> getReplyCommentList(SearchDto searchDto);

    /**
     * 대댓글 리스트 조회 (로그인 시)
     *
     * @param searchDto
     * @return
     */
    List<CommunityCommentDto> getReplyCommentListLogin(SearchDto searchDto);

    /**
     * 신고한 댓글 idx 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getCommentReportList(Long memberIdx);

    /**
     * 유효한 댓글 idx 인지 조회
     *
     * @param commentDto
     * @return
     */
    int getCommentCnt(CommunityCommentDto commentDto);

    /**
     * 댓글 작성자 member_idx 조회
     *
     * @param idx
     * @return
     */
    Long getCommentWriterIdx(Long idx);

    /**
     * 댓글의 parent_idx, comment_cnt 조회
     *
     * @param idx
     * @return
     */
    CommunityCommentDto getCommentParentAndCommentCnt(long idx);

    /**
     * 댓글 좋아요 한적 있는지 조회
     *
     * @param commentDto
     * @return
     */
    Integer getMemCommentLikeState(CommunityCommentDto commentDto);

    /**
     * 댓글 신고한적 있는 지 조회
     *
     * @param commentDto
     * @return
     */
    int getReportCnt(CommunityCommentDto commentDto);


}
