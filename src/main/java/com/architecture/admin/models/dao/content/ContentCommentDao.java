package com.architecture.admin.models.dao.content;

import com.architecture.admin.models.dto.content.ContentCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentCommentDao {

    /**************************************************************
     * Insert
     **************************************************************/

    /**
     * 컨텐츠 댓글 OR 대댓글 등록
     *
     * @param contentCommentDto : content(댓글 내용)
     * @return
     */
    int registerCommentOrReply(ContentCommentDto contentCommentDto);

    /**
     * 댓글 좋아요 테이블 등록
     *
     * @param contentCommentDto
     */
    int insertMemCommentLike(ContentCommentDto contentCommentDto);

    /**************************************************************
     * Update
     **************************************************************/

    /**
     * 컨텐츠 댓글 개수 업데이트
     *
     * @param updateCommentDto
     * @return
     */
    int updateCommentCnt(ContentCommentDto updateCommentDto);

    /**
     * 댓글 좋아요 업데이트
     * comment_like 테이블
     *
     * @param contentCommentDto
     * @return
     */
    int updateMemCommentLike(ContentCommentDto contentCommentDto);

    /**
     * 댓글 좋아요 개수 업데이트
     * contents_comment 테이블
     *
     * @param contentCommentDto
     * @return
     */
    int updateCommentLikeCnt(ContentCommentDto contentCommentDto);

    /**
     * 컨텐츠 댓글 OR 대댓글 삭제
     *
     * @param contentCommentDto : idx(댓글 idx), contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    int deleteCommentOrReply(ContentCommentDto contentCommentDto);

    /**
     * 삭제할 댓글에 달린 대댓글 노출값 변경 (1 -> 0)
     *
     * @param contentCommentDto : idx(댓글 idx), contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    int updateReplyView(ContentCommentDto contentCommentDto);

    /**
     * 댓글 OR 대댓글 신고하기
     *
     * @param contentCommentDto
     * @return
     */
    int insertContentCommentReport(ContentCommentDto contentCommentDto);
}
