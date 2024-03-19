package com.architecture.admin.models.dao.episode;

import com.architecture.admin.models.dto.episode.EpisodeCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface EpisodeCommentDao {

    /**************************************************************
     * Insert
     **************************************************************/

    /**
     * 댓글 등록
     *
     * @param episodeCommentDto
     */
    void registComment(EpisodeCommentDto episodeCommentDto);


    /**************************************************************
     * Update
     **************************************************************/

    /**
     * 댓글 개수 업데이트
     *
     * @param updateCommentDto
     * @return
     */
    int updateCommentCnt(EpisodeCommentDto updateCommentDto);

    /**
     * 댓글 좋아요 테이블 등록
     *
     * @param commentDto
     */
    int insertMemCommentLike(EpisodeCommentDto commentDto);

    /**
     * 댓글 좋아요 업데이트
     * comment_like 테이블
     *
     * @param commentDto
     * @return
     */
    int updateMemCommentLike(EpisodeCommentDto commentDto);

    /**
     * 댓글 좋아요 개수 업데이트
     * episode_comment 테이블
     *
     * @param commentDto
     * @return
     */
    int updateCommentLikeCnt(EpisodeCommentDto commentDto);

    /**
     * 댓글 삭제
     *
     * @param commentDto
     * @return
     */
    int deleteComment(EpisodeCommentDto commentDto);

    /**
     * 대대글 삭제 (view 컬럼 업데이트)
     *
     * @param idx
     * @return
     */
    int deleteReplyComment(long idx);

    /**
     * 댓글 신고하기
     *
     * @param commentDto
     */
    int insertCommentReport(EpisodeCommentDto commentDto);
}
