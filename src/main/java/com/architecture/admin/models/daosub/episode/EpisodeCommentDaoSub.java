package com.architecture.admin.models.daosub.episode;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface EpisodeCommentDaoSub {

    /****************************************************************
     * SELECT - 카운트 조회
     ****************************************************************/

    /**
     * 유효한 댓글인지 조회
     *
     * @param commentDto : idx(댓글 idx), episodeIdx(회차 idx)
     * @return
     */
    int getCommentCnt(EpisodeCommentDto commentDto);

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
     * 부모 댓글 번호 & 대댓글 개수 조회
     *
     * @param idx
     * @return
     */
    EpisodeCommentDto getCommentParentAndCommentCnt(long idx);

    /**
     * 댓글 신고 한적 있는지 조회
     *
     * @param commentDto : idx(댓글 idx), memberIdx(회원 idx)
     * @return
     */
    int getReportCnt(EpisodeCommentDto commentDto);

    /**
     * 댓글 좋아요 개수 (comment_table)
     *
     * @param commentDto
     * @return
     */
    int getCommentLikeCnt(EpisodeCommentDto commentDto);

    /**
     * 대댓글 개수 조회
     *
     * @param parentIdx
     */
    int getReplyCommentCntByIdx(long parentIdx);

    /****************************************************************
     * SELECT - 단일 값 조회
     ****************************************************************/

    /**
     * 좋아요 한적 있는 지 조회 (comment_like 테이블)
     *
     * @param commentDto
     * @return
     */
    EpisodeCommentDto getMemCommentLike(EpisodeCommentDto commentDto);

    /**
     * 댓글 작성자 member_idx 조회
     *
     * @param idx
     * @return
     */
    Long getCommentWriterIdx(Long idx);

    /**
     * 부모 댓글 정보 조회 (좋아요, 댓글 수, 그룹 번호)
     *
     * @param parentIdx : 부모 댓글 번호
     * @return
     */
    EpisodeCommentDto getParentCommentInfo(Long parentIdx);

    /****************************************************************
     * SELECT - 리스트 조회
     ****************************************************************/

    /**
     * 댓글 리스트 조회(로그인)
     *
     * @param searchDto
     * @return
     */
    List<EpisodeCommentDto> getCommentListLogin(SearchDto searchDto);

    /**
     * 대댓글 리스트 조회(로그인)
     *
     * @param searchDto
     * @return
     */
    List<EpisodeCommentDto> getReplyCommentListLogin(SearchDto searchDto);

    /**
     * 대댓글 리스트 조회(비로그인)
     *
     * @param searchDto
     * @return
     */
    List<EpisodeCommentDto> getReplyCommentList(SearchDto searchDto);

    /**
     * 댓글 신고 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getCommentReportList(Long memberIdx);

    /**
     * 회원이 좋아요한 댓글 idx 리스트 조회
     *
     * @param memberIdx
     * @return
     */
    List<Long> getCommentLikeIdxList(Long memberIdx);
}
