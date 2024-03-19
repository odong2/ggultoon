package com.architecture.admin.services.episode;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.DateLibrary;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.episode.EpisodeCommentDao;
import com.architecture.admin.models.daosub.episode.EpisodeCommentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeCommentDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.architecture.admin.config.SessionConfig.LOGIN_NICK;

@Service
@RequiredArgsConstructor
public class EpisodeCommentService extends BaseService {

    private static final int MAX_COMMENT_LENGTH = 200;
    private final EpisodeCommentDao commentDao;
    private final EpisodeCommentDaoSub commentDaoSub;
    private final EpisodeDaoSub episodeDaoSub;
    private final EpisodeCommentDaoSub episodeCommentDaoSub;
    private final DateLibrary dateLibrary;

    /**************************************************************************************
     * Select
     **************************************************************************************/

    /**
     * 댓글 리스트(로그인 상태일 때만 열람 가능)
     *
     * @param searchDto : episodeIdx(회차 idx), page(페이지 번호)
     * @return : 댓글 리스트
     */
    @Transactional(readOnly = true)
    public JSONObject getCommentList(SearchDto searchDto) {

        /** 에피소드 idx 유효성 검사 **/
        episodeIdxValidate(searchDto.getEpisodeIdx());

        // return value
        JSONObject jsonData = new JSONObject();
        List<EpisodeCommentDto> commentList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo == null) { // 비로그인 상태
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);  // 로그인 후 이용해주세요.
           
        } else { // 로그인 상태

            // 로그인 상태 set
            jsonData.put("login", true);

            // 닉네임 유무 검사
            checkIsMemberHaveNick();

            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회차 댓글 개수 카운트
            int totalCnt = commentDaoSub.getCommentsTotalCnt(searchDto);

            // 회차 댓글이 있는 경우
            if (totalCnt > 0) {

                // paging 처리
                PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
                searchDto.setPagination(pagination);

                // 회차 댓글 리스트 조회
                commentList = commentDaoSub.getCommentListLogin(searchDto);

                // 댓글 추가 정보 세팅
                setCommentAndReplyInfo(commentList, searchDto);

                // paging 담기
                jsonData.put("params", new JSONObject(searchDto));
            }
            // list set
            jsonData.put("commentList", commentList);
        }
        return jsonData;
    }

    /**
     * 대댓글 리스트(로그인 상태일 때만 열람 가능)
     *
     * @param searchDto : idx(부모 댓글 idx), episodeIdx(회차 idx), page(현재 페이지), recordSize(한 페이지 개수)
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getReplyCommentList(SearchDto searchDto) {

        /** 대댓글 리스트 유효성 검사 **/
        replyCommentListValidate(searchDto);
        //return value
        JSONObject jsonData = new JSONObject();

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 로그인 상태
        if (memberInfo != null) {
            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx);                  // Long 형변환
            searchDto.setMemberIdx(memberIdx);                               // 회원 idx set

            /** 신고한 목록 조회 **/
            List<Long> reportIdxList = commentDaoSub.getCommentReportList(memberIdx);
            // 신고한 내역이 있음
            if (reportIdxList != null && !reportIdxList.isEmpty()) {
                searchDto.setIdxList(reportIdxList);
            }
        }

        // 대댓글 개수 카운트
        int totalCnt = commentDaoSub.getReplyCommentTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        // 대댓글 리스트 조회
        List<EpisodeCommentDto> replyCommentList = null;

        // 댓글이 존재함
        if (totalCnt > 0) {
            // 비로그인
            if (memberInfo == null) {
                replyCommentList = commentDaoSub.getReplyCommentList(searchDto);
                // 로그인 상태 불리언 set
                jsonData.put("login", false);

            // 로그인
            } else {
                // 대댓글 리스트 조회(로그인 회원 전용)
                replyCommentList = commentDaoSub.getReplyCommentListLogin(searchDto);
                // 로그인 상태 불리언 set
                jsonData.put("login", true);
                searchDto.setIdxList(null);   // 앞단에서 쓰지 않으므로 null 처리
                searchDto.setMemberIdx(null);
            }
            // 댓글 추가 정보 세팅
            setCommentAndReplyInfo(replyCommentList, searchDto);

            // 페이징
            jsonData.put("params", new JSONObject(searchDto));
        }
        jsonData.put("replyCommentList", replyCommentList);

        return jsonData;
    }


    /**************************************************************************************
     * Insert
     **************************************************************************************/

    /**
     * 댓글 등록
     *
     * @param commentDto : episodeIdx(회차 idx), memberIdx(회원 idx), content(댓글 내용)
     */
    @Transactional
    public void registComment(EpisodeCommentDto commentDto) {

        // 댓글 공백 제거
        commentDto.setContent(commentDto.getContent().trim());

        /** 댓글 등록 유효성 검사 **/
        registCommentValidate(commentDto);

        commentDto.setRegdate(dateLibrary.getDatetime());
        commentDto.setModifyDate(dateLibrary.getDatetime());

        // 댓글 등록
        commentDao.registComment(commentDto);
    }

    /**
     * 대댓글 등록
     *
     * @param commentDto : episodeIdx(회차 idx), idx(부모 댓글 idx), parentIdx(부모 댓글 idx), content(댓글 내용)
     */
    @Transactional
    public void registReplyComment(EpisodeCommentDto commentDto) {

        /** 대댓글 등록 유효성 검사 **/
        registReplyCommentValidate(commentDto);

        // 날짜 set
        commentDto.setRegdate(dateLibrary.getDatetime());
        commentDto.setModifyDate(dateLibrary.getDatetime());

        // 1. 부모 댓글 정보 조회(idx, 댓글 개수, 좋아요, 그룹 번호 조회)
        Long parentIdx = commentDto.getParentIdx(); // 부모 댓글 번호
        EpisodeCommentDto parentCommentDto = commentDaoSub.getParentCommentInfo(parentIdx);

        // 2. 대댓글 등록
        commentDao.registComment(commentDto);

        // 3. 부모 댓글수 증가
        int commentCnt = parentCommentDto.getCommentCnt();
        Long commentIdx = parentCommentDto.getIdx();

        // 업데이트 할 dto set
        EpisodeCommentDto updateCommentDto = EpisodeCommentDto.builder()
                .parentIdx(commentIdx)
                .commentCnt(commentCnt + 1)
                .build();

        // 4. 부모 댓글 수 update
        int result = commentDao.updateCommentCnt(updateCommentDto);

        if (result < 1) {
            throw new CustomException(CustomError.EPISODE_COMMENT_REGIST_FAIL); // 댓글 등록에 실패하였습니다.
        }
    }

    /**
     * 댓글 신고
     *
     * @param commentDto
     */
    public void reportComment(EpisodeCommentDto commentDto) {

        /** 에피소드 IDX 유효성 검사 **/
        episodeIdxValidate(commentDto.getEpisodeIdx());

        /** 댓글 IDX 유효성 검사 **/
        commentIdxValidate(commentDto);

        // 댓글 신고 한적 있는지 조회
        int reportCnt = commentDaoSub.getReportCnt(commentDto);

        if (reportCnt == 1) {
            throw new CustomException(CustomError.EPISODE_COMMENT_REPORT_EXIST); // 이미 신고한 댓글입니다.
        }
        // 댓글 신고하기
        commentDto.setRegdate(dateLibrary.getDatetime()); // 현재 시간 set
        try {
            int result = commentDao.insertCommentReport(commentDto);

            if (result < 1) {
                throw new CustomException(CustomError.EPISODE_COMMENT_REPORT_FAIL); // 댓글 신고를 실패하였습니다.
            }
        } catch (DuplicateKeyException e) {
            throw new CustomException(CustomError.EPISODE_COMMENT_REPORT_EXIST);    // 이미 신고한 댓글입니다.
        }
    }

    /**
     * 댓글 좋아요
     *
     * @param commentDto : idx(댓글 idx), episodeIdx(회차 idx), memberIdx(회원 idx)
     */
    @Transactional
    public String updateCommentLike(EpisodeCommentDto commentDto) {

        /** 에피소드 IDX 유효성 검사 **/
        episodeIdxValidate(commentDto.getEpisodeIdx());
        /** 댓글 IDX 유효성 검사 **/
        commentIdxValidate(commentDto);

        // 1. 좋아요 한적 있는지 조회 (state 및 LikeCnt 조회)
        EpisodeCommentDto commentLikeDto = commentDaoSub.getMemCommentLike(commentDto);
        String likeType = "like";

        commentDto.setState(1); // 정상 set
        commentDto.setRegdate(dateLibrary.getDatetime()); // 날짜 set

        // 1. 좋아요 한적 없음
        if (commentLikeDto == null) {
            commentDao.insertMemCommentLike(commentDto);  // 좋아요 테이블 insert
            commentLikeCntUpdate(commentDto, likeType);   // 댓글 테이블 좋아요수 증가

            // 2. 좋아요 한적있음
        } else if (commentLikeDto != null) {
            int likeState = commentLikeDto.getState(); // 좋아요 상태(0: 좋아요 취소상태, 1: 좋아요 상태)

            // 현재 좋아요 상태 -> 좋아요 취소
            if (likeState == 1) {
                likeType = "likeCancel";
                commentDto.setState(0); // 삭제 set
                commentDao.updateMemCommentLike(commentDto); // 좋아요 취소

                // 댓글 테이블 좋아요수 감소
                commentLikeCntUpdate(commentDto, likeType);

                // 현재 좋아요 상태 아님 -> 좋아요
            } else {
                commentDao.updateMemCommentLike(commentDto); // 좋아요 테이블 update
                commentLikeCntUpdate(commentDto, likeType);  // 댓글 테이블 좋아요수 증가
            }
        }

        return likeType;
    }

    /**
     * 댓글 테이블 좋아요 수 업데이트
     *
     * @param commentDto
     * @param likeType   : like(좋아요), likeCancel(좋아요 취소)
     */
    private void commentLikeCntUpdate(EpisodeCommentDto commentDto, String likeType) {

        /** 1. 현재 댓글 좋아요 개수 가지고 오기 **/
        int commentLikeCnt = commentDaoSub.getCommentLikeCnt(commentDto);

        // 좋아요 취소
        if (likeType.equals("likeCancel")) {
            commentLikeCnt = commentLikeCnt - 1; // 좋아요 수 감소
            // 좋아요
        } else if (likeType.equals("like")) {
            commentLikeCnt = commentLikeCnt + 1; // 좋아요 수 증가
        }
        // 좋아요 수 set
        commentDto.setLikeCnt(commentLikeCnt);

        /** 2. 댓글 테이블 좋아요수 업데이트 **/
        int updateResult = commentDao.updateCommentLikeCnt(commentDto);

        /** 3. 업데이트 결과에 따른 rollback **/
        if (updateResult < 1 && likeType.equals("regist")) {
            throw new CustomException(CustomError.EPISODE_COMMENT_LIKE_FAIL); // 댓글 좋아요를 실패하였습니다.
        } else if (updateResult < 1 && likeType.equals("cancel")) {
            throw new CustomException(CustomError.EPISODE_COMMENT_LIKE_CANCEL_FAIL);// 댓글 좋아요 취소를 실패하였습니다.
        }
    }


    /**************************************************************************************
     * Delete
     **************************************************************************************/

    /**
     * 댓글 삭제
     *
     * @param commentDto : idx(댓글 idx), episodeIdx(회차 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void deleteComment(EpisodeCommentDto commentDto) {

        /** 댓글 삭제 유효성 검사 **/
        deleteCommentValidate(commentDto);

        long idx = commentDto.getIdx(); // 댓글 idx
        // 댓글 삭제(view, state 0으로 업데이트)
        int result = commentDao.deleteComment(commentDto);

        if (result < 1) {
            throw new CustomException(CustomError.EPISODE_COMMENT_DELETE_FAIL); // 댓글 삭제를 실패하였습니다.
        }
        // 삭제한 댓글 parentIdx & comment_cnt 조회
        EpisodeCommentDto episodeCommentDto = commentDaoSub.getCommentParentAndCommentCnt(idx);
        long parentIdx = episodeCommentDto.getParentIdx();  // 부모 댓글 idx
        int commentCnt = episodeCommentDto.getCommentCnt(); // 대댓글 개수
        result = 0;

        // 삭제한 댓글이 댓글이고 대댓글이 달린경우
        if (parentIdx == 0 && commentCnt > 0) {
            result = commentDao.deleteReplyComment(idx);

        // 삭제한 댓글이 댓글이고 대댓글이 없는 경우
        } else if (parentIdx == 0 && commentCnt == 0) {
            result = 1;

        // 삭제한 댓글이 대댓글인 경우
        } else if (parentIdx != 0) {
            int replyCommentCnt = commentDaoSub.getReplyCommentCntByIdx(parentIdx); // 대댓글 개수

            // 댓글의 대댓글 개수 업데이트
            episodeCommentDto.setCommentCnt(null);
            episodeCommentDto.setCommentCnt(replyCommentCnt); // 조회한 대댓글 개수 set
            result = commentDao.updateCommentCnt(episodeCommentDto);
        }

        if (result < 1) {
            throw new CustomException(CustomError.EPISODE_COMMENT_DELETE_FAIL); // 댓글 삭제를 실패하였습니다.
        }

    }

    /**************************************************************************************
     * SUB - 댓글 및 대댓글 표시 정보 세팅
     **************************************************************************************/

    /**
     * 댓글 및 대댓글 표시 정보 세팅
     * 비로그인 : 댓글/대댓글 등록일
     * 로그인 : 댓글/대댓글 등록일, 좋아요 세팅, 신고한 댓글 숨김처리
     *
     * @param episodeCommentList
     */
    private void setCommentAndReplyInfo(List<EpisodeCommentDto> episodeCommentList, SearchDto searchDto) {

        if (!episodeCommentList.isEmpty()) {

            for (EpisodeCommentDto commentDto : episodeCommentList) {

                // 세팅할 댓글이 있는 경우
                if (commentDto != null) {

                    /** 댓글 & 대댓글 등록일 표시 **/
                    // 등록일 변환
                    String regdate = dateLibrary.getConvertRegdate(commentDto.getRegdate());
                    commentDto.setRegdate(regdate);

                    /** 로그인 상태일 때 추가 세팅 정보 **/
                    if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0) {

                        /** 회원이 좋아요 누른 댓글 & 대댓글 표시 **/
                        // 회원이 좋아요 누른 댓글 idx 리스트 조회
                        List<Long> likeIdxList = episodeCommentDaoSub.getCommentLikeIdxList(searchDto.getMemberIdx());

                        if (likeIdxList.contains(commentDto.getIdx())) {
                            commentDto.setIsMemberLike(true);  // 좋아요 함
                        } else {
                            commentDto.setIsMemberLike(false); // 좋아요 안함
                        }
                    }
                }
            }
        }
    }

    /**************************************************************************************
     * Validate
     **************************************************************************************/

    /**
     * 대댓글 리스트 유효성 검사
     */
    private void replyCommentListValidate(SearchDto searchDto) {
        // 조회할 대댓글의 부모 댓글 dto
        EpisodeCommentDto commentDto = EpisodeCommentDto.builder()
                .episodeIdx(searchDto.getEpisodeIdx()) // 회차 idx
                .idx(searchDto.getIdx())               // 부모 댓글 idx
                .build();

        // 1. 에피소드 IDX 유효성 검사(공통)
        episodeIdxValidate(commentDto.getEpisodeIdx());
        // 2. 부모 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
    }

    /**
     * 댓글 등록 유효성 검사
     *
     * @param commentDto
     */
    private void registCommentValidate(EpisodeCommentDto commentDto) {
        // 1. 에피소드 IDX 유효성 검사(공통)
        episodeIdxValidate(commentDto.getEpisodeIdx());
        // 2. 댓글 작성 공통 유효성 검사
        writeCommentCommonValidate(commentDto);
    }

    /**
     * 댓글 삭제 유효성 검사
     *
     * @param commentDto
     */
    private void deleteCommentValidate(EpisodeCommentDto commentDto) {
        // 1. 에피소드 IDX 유효성 검사(공통)
        episodeIdxValidate(commentDto.getEpisodeIdx());
        // 2. 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
        // 3. 댓글 작성자와 삭제하는 회원 같은지 검사
        commentWriterValidate(commentDto);
    }

    /**
     * 대댓글 등록 유효성 검사
     *
     * @param commentDto
     */
    private void registReplyCommentValidate(EpisodeCommentDto commentDto) {

        // 1. 댓글 등록 유효성 검사(공통)
        registCommentValidate(commentDto);
        // 2. 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
    }

    /**
     * 회차 idx 유효성 검사(공통)
     */
    private void episodeIdxValidate(Long idx) {
        // idx 기본 유효성 검사
        if (idx == null || idx < 1L) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

        // 조회할 episodeDto set
        EpisodeDto episodeDto = EpisodeDto.builder()
                .nowDate(dateLibrary.getDatetime())// 현재 시간
                .idx(idx) // 에피소드 idx
                .build();

        // idx db 조회 후 검사
        int episodeCnt = episodeDaoSub.getEpisodeCnt(episodeDto);

        if (episodeCnt < 1) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }
    }

    /**
     * 댓글 idx 유효성 검사(공통)
     *
     * @param commentDto
     */
    private void commentIdxValidate(EpisodeCommentDto commentDto) {
        // 1. commentIdx 값 검사
        if (commentDto.getIdx() == null || commentDto.getIdx() < 1L) {
            throw new CustomException(CustomError.EPISODE_COMMENT_IDX_EMPTY); // 존재하지 않는 댓글입니다.
        }

        // 2. 유효한 댓글인지 db 조회
        int commentCnt = commentDaoSub.getCommentCnt(commentDto);

        // 삭제되었거나 존재하지 않는 댓글
        if (commentCnt < 1) {
            throw new CustomException(CustomError.EPISODE_COMMENT_IDX_ERROR); // 존재하지 않는 댓글입니다.
        }
    }

    /**
     * 댓글 작성 공통 유효성 검사 (댓글 등록)
     */
    private void writeCommentCommonValidate(EpisodeCommentDto commentDto) {

        // 1. 댓글 내용 빈값
        if (commentDto.getContent() == null || commentDto.getContent().isEmpty()) {
            throw new CustomException(CustomError.EPISODE_COMMENT_CONTENT_EMPTY); // 댓글 내용을 입력해주세요.
        }
        // 2. 댓글 최대 길이 제한
        if (commentDto.getContent().length() > MAX_COMMENT_LENGTH) {
            throw new CustomException(CustomError.EPISODE_COMMENT_LENGTH_ERROR); // 최대 200자 까지 입력 가능합니다.
        }
        // 회원 닉네임 조회
        String nickName = super.getMemberInfo(LOGIN_NICK);

        // 3. 닉네임 빈값
        if (nickName == null || nickName.isEmpty()) {
            throw new CustomException(CustomError.EPISODE_COMMENT_NICK_EMPTY); // 닉네임 설정 후 작성해주세요.
        }
    }

    /**
     * 댓글 작성자와 일치하는지 유효성 검사
     * (삭제)
     *
     * @param commentDto
     */
    private void commentWriterValidate(EpisodeCommentDto commentDto) {
        // 1. 댓글 작성자와 수정자 같은 지 검사
        long memberIdx = commentDto.getMemberIdx(); // 수정하는 회원 idx

        // 2. 작성자 회원 idx 조회
        Long writerIdx = commentDaoSub.getCommentWriterIdx(commentDto.getIdx()); // 작성자 member_idx 조회
        // 3. 작성자가 null 이면 존재하지 않는 댓글
        if (writerIdx == null) {
            throw new CustomException(CustomError.EPISODE_COMMENT_IDX_ERROR);    // 존재하지 않는 댓글입니다.
        }

        // 4. 댓글 작성자 및 수정자 일치하지 않음
        if (memberIdx != (long) writerIdx) {
            throw new CustomException(CustomError.EPISODE_COMMENT_WRITER_DIFF);  // 본인 작성 댓글이 아닙니다.
        }
    }

    /**
     * 댓글 목록을 열람하는 회원의 닉네임 유무 검사
     */
    private void checkIsMemberHaveNick() {

        // 세션에 저장된 회원 닉네임 가져오기
        String nick = super.getMemberInfo(LOGIN_NICK);

        // 댓글 목록을 열람하려는 회원이 아직 닉네임을 등록하지 않은 경우
        if (nick == null || nick.isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_NICK_EMPTY);    // 닉네임 등록 후 댓글 이용이 가능합니다.
        }
    }
}
