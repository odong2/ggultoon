package com.architecture.admin.models.daosub.content;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.ContentCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentCommentDaoSub {

    /**
     * 유효한 컨텐츠인지 조회
     *
     * @param searchDto
     * @return
     */
    int getContentCountByIdx(SearchDto searchDto);

    /**
     * 유효한 댓글인지 조회
     *
     * @param contentCommentDto
     * @return
     */
    int getCommentCnt(ContentCommentDto contentCommentDto);

    /**
     * 댓글인지 대댓글인지 조회
     *
     * @param contentCommentDto
     * @return
     */
    long checkIsCommentOrReply(ContentCommentDto contentCommentDto);

    /**
     * 컨텐츠 베스트 댓글 리스트 조회(비로그인)
     *
     * @param searchDto
     * @return
     */
    List<ContentCommentDto> getBestContentCommentList(SearchDto searchDto);

    /**
     * 컨텐츠 베스트 댓글 리스트 조회(로그인)
     *
     * @param searchDto
     * @return
     */
    List<ContentCommentDto> getLoginBestContentCommentList(SearchDto searchDto);

    /**
     * 컨텐츠 댓글 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getContentCommentTotalCnt(SearchDto searchDto);

    /**
     * 컨텐츠 댓글 리스트 조회(로그인)
     *
     * @param searchDto
     * @return
     */
    List<ContentCommentDto> getLoginContentCommentList(SearchDto searchDto);

    /**
     * 컨텐츠 대댓글 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getContentReplyTotalCnt(SearchDto searchDto);

    /**
     * 컨텐츠 대댓글 리스트 조회(로그인)
     *
     * @param searchDto
     * @return
     */
    List<ContentCommentDto> getLoginContentReplyList(SearchDto searchDto);

    /**
     * 컨텐츠 부모 댓글 정보 조회 (좋아요, 댓글 수, 그룹 번호)
     *
     * @param parentIdx : 부모 댓글 번호
     * @return
     */
    ContentCommentDto getParentCommentInfo(Long parentIdx);

    /**
     * 컨텐츠 댓글 작성자 member_idx 조회
     *
     * @param idx
     * @return
     */
    Long getCommentWriterIdx(Long idx);

    /**
     * 좋아요한 적 있는지 조회(comment_like 테이블)
     *
     * @param commentDto
     * @return
     */
    ContentCommentDto getMemCommentLike(ContentCommentDto commentDto);

    /**
     * 댓글 좋아요 개수 (comment_table)
     *
     * @param commentDto
     * @return
     */
    int getCommentLikeCnt(ContentCommentDto commentDto);

    /**
     * 댓글 정보 조회
     *
     * @param contentCommentDto
     * @return
     */
    ContentCommentDto getCommentInfo(ContentCommentDto contentCommentDto);

    /**
     * 신고한 댓글 조회
     *
     * @param contentCommentDto
     * @return
     */
    ContentCommentDto getContentCommentReport(ContentCommentDto contentCommentDto);
    
    /**
     * 베스트 댓글 idx 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<Long> getCommentBestIdxList(SearchDto searchDto);

    /**
     * 회원이 좋아요한 댓글 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getCommentLikeIdxList(Long memberIdx);

    /**
     * 회원이 신고한 댓글 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getCommentReportIdxList(Long memberIdx);
}
