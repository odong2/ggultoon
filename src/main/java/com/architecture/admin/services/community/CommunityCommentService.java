package com.architecture.admin.services.community;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.community.CommunityCommentDao;
import com.architecture.admin.models.dao.community.CommunityDao;
import com.architecture.admin.models.daosub.community.CommunityCommentDaoSub;
import com.architecture.admin.models.daosub.community.CommunityDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.community.CommunityCommentDto;
import com.architecture.admin.models.dto.community.CommunityContentDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.architecture.admin.config.SessionConfig.LOGIN_NICK;

@RequiredArgsConstructor
@Service
public class CommunityCommentService extends BaseService {

    private static final int MAX_COMMENT_LENGTH = 200;
    private static final String COMMENT_LIKE = "like";
    private static final String COMMENT_LIKE_CANCEL = "likeCancel";
    private static final String COMMENT_REGIST = "regist";
    private static final String COMMENT_DELETE = "delete";
    private final CommunityCommentDao commentDao;
    private final CommunityCommentDaoSub commentDaoSub;
    private final CommunityDao communityDao;
    private final CommunityDaoSub communityDaoSub;


    /**************************************************************************************
     * Select
     **************************************************************************************/

    /**
     * 댓글 리스트
     *
     * @param searchDto : communityIdx(커뮤니티 게시물 idx),
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getCommentList(SearchDto searchDto) {

        /** 커뮤니티 컨텐츠 idx 유효성 검사 **/
        communityIdxValidate(searchDto.getCommunityIdx());

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
        // 댓글 개수 조회
        int totalCnt = commentDaoSub.getCommentsTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<CommunityCommentDto> commentList = null;

        if (totalCnt > 0) {
            // 비로그인
            if (memberInfo == null) {
                // 댓글 리스트 조회
                commentList = commentDaoSub.getCommentList(searchDto);
            // 로그인
            } else {
                // 댓글 리스트 조회
                commentList = commentDaoSub.getCommentListLogin(searchDto);
                searchDto.setIdxList(null); // 앞단에서 필요없는 정보 null 처리
                searchDto.setMemberIdx(null);
            }

            // 댓글 등록일 변환
            setCommentTime(commentList);
            // 좋아요 여부 불리언 변환
            likeBoolean(commentList);

            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set
        }
        jsonData.put("commentList", commentList);

        return jsonData;
    }

    /**
     * 대댓글 리스트
     *
     * @param searchDto :  communityIdx(커뮤니티 게시물 idx), commentIdx(댓글 idx)
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
        List<CommunityCommentDto> replyCommentList = null;

        // 댓글이 존재함
        if (totalCnt > 0) {

            // 비로그인
            if (memberInfo == null) {
                replyCommentList = commentDaoSub.getReplyCommentList(searchDto);
            // 로그인
            } else {
                // 대댓글 리스트 조회(로그인 회원 전용)
                replyCommentList = commentDaoSub.getReplyCommentListLogin(searchDto);
                searchDto.setIdxList(null); // 앞단에서 필요없는 정보 null 처리
                searchDto.setMemberIdx(null);
            }
            // 좋아요 여부 불리언 변환
            likeBoolean(replyCommentList);
            // 댓글 등록일 변환
            setCommentTime(replyCommentList);

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
     * @param commentDto : communityContentsIdx(커뮤니티 게시물 idx), memberIdx(회원 idx), content(댓글 내용)
     */
    @Transactional
    public void registComment(CommunityCommentDto commentDto) {

        /** 댓글 등록 유효성 검사 **/
        registCommentValidate(commentDto);

        commentDto.setRegdate(dateLibrary.getDatetime());
        commentDto.setModifyDate(dateLibrary.getDatetime());

        // 1. 다음 group 번호 조회
        long groupIdx = commentDao.getNextGroupIdx(); // 다음 그룹 번호 조회
        commentDto.setGroupIdx(groupIdx); // 그룹 번호 set

        // 2. 댓글 등록
        int result = commentDao.registComment(commentDto);

        // 댓글 등록 실패
        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_REGIST_FAIL);
        }

        /** community_contents_info 테이블 댓글 수 증가 **/
        Long communityIdx = commentDto.getCommunityContentsIdx();

        infoCommentCntUpdate(communityIdx, 1, COMMENT_REGIST); // 댓글 수 1 증가

    }

    /**
     * 대댓글 등록
     *
     * @param commentDto : communityContentsIdx(커뮤니티 게시물 idx), commentIdx(댓글 idx), content(댓글 내용)
     */
    @Transactional
    public void registReplyComment(CommunityCommentDto commentDto) {

        /** 대댓글 등록 유효성 검사 **/
        registReplyCommentValidate(commentDto);

        // 날짜 set
        commentDto.setRegdate(dateLibrary.getDatetime());
        commentDto.setModifyDate(dateLibrary.getDatetime());

        // 1. 부모 댓글 정보 조회(idx, 댓글 개수, 좋아요, 그룹 번호 조회)
        Long parentIdx = commentDto.getParentIdx(); // 부모 댓글 번호
        CommunityCommentDto parentCommentDto = commentDao.getParentCommentInfo(parentIdx);

        // 부모 그룹번호 set
        Long groupIdx = parentCommentDto.getGroupIdx();
        commentDto.setGroupIdx(groupIdx);

        // 2. 대댓글 등록
        commentDao.registComment(commentDto);

        // 3. 부모 댓글수 증가
        int commentCnt = parentCommentDto.getCommentCnt();
        Long commentIdx = parentCommentDto.getIdx();

        // 업데이트 할 dto set
        CommunityCommentDto updateCommentDto = CommunityCommentDto.builder()
                .parentIdx(commentIdx)
                .commentCnt(commentCnt + 1)
                .build();

        // 4. 부모 댓글 수 update
        int result = commentDao.updateCommentCnt(updateCommentDto);

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_REGIST_FAIL); // 댓글 등록에 실패하였습니다.
        }

        /** community_contents_info 테이블 댓글 수 증가 **/
        Long communityIdx = commentDto.getCommunityContentsIdx();
        infoCommentCntUpdate(communityIdx, 1, COMMENT_REGIST); // 댓글 수 1 증가
    }

    /**
     * 댓글 좋아요 & 좋아요 취소
     *
     * @param commentDto : communityContentsIdx(커뮤니티 게시물 idx), commentIdx(댓글 idx)
     * @return
     */
    @Transactional
    public String updateCommentLike(CommunityCommentDto commentDto) {

        /** 커뮤니티 게시물 IDX 유효성 검사 **/
        communityIdxValidate(commentDto.getCommunityContentsIdx());
        /** 댓글 IDX 유효성 검사 **/
        commentIdxValidate(commentDto);

        // 1. 좋아요 한적 있는지 조회 (state 및 LikeCnt 조회)
        Integer state   = commentDaoSub.getMemCommentLikeState(commentDto);
        String likeType = COMMENT_LIKE;                   // 디폴트 좋아요 set

        commentDto.setState(1);                           // 디폴트 좋아요 set
        commentDto.setRegdate(dateLibrary.getDatetime()); // 날짜 set
        int result = 0; // 업데이트 결과

        // 1. 좋아요 한적 없음
        if (state == null) {
            result = commentDao.insertMemCommentLike(commentDto);  // 좋아요 insert

        // 2. 좋아요 한적있음
        } else {
            // 현재 좋아요 상태 -> 좋아요 취소
            if (state == 1) {
                likeType = COMMENT_LIKE_CANCEL; // 좋아요 취소 set
                commentDto.setState(0);         // 좋아요 취소 set

                result = commentDao.updateMemCommentLike(commentDto); // 좋아요 취소 업데이트

            // 현재 좋아요 상태 아님 -> 좋아요
            } else {
                result = commentDao.updateMemCommentLike(commentDto); // 좋아요 업데이트
            }
        }

        // insert 또는 update 실패 시
        if (result < 1) {
            if (likeType.equals(COMMENT_LIKE)) { // 좋아요
                throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_FAIL);         // 댓글 좋아요를 실패하였습니다.
            } else { // 좋아요 취소
                throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_CANCEL_FAIL);  // 댓글 좋아요 취소를 실패하였습니다.
            }
        }

        /** 댓글 테이블 좋아요수 업데이트 **/
        commentLikeCntUpdate(commentDto, likeType);

        return likeType;
    }

    /**
     * 댓글 테이블 좋아요 수 업데이트
     *
     * @param commentDto
     * @param likeType   : like(좋아요), likeCancel(좋아요 취소)
     */
    private void commentLikeCntUpdate(CommunityCommentDto commentDto, String likeType) {

        /** 1. 현재 댓글 좋아요 개수 가지고 오기 **/
        int commentLikeCnt = commentDao.getCommentLikeCnt(commentDto);

        // 좋아요 취소
        if (likeType.equals(COMMENT_LIKE_CANCEL)) {
            commentLikeCnt = commentLikeCnt - 1; // 좋아요 수 감소
        // 좋아요
        } else {
            commentLikeCnt = commentLikeCnt + 1; // 좋아요 수 증가
        }
        // 좋아요 수 set
        commentDto.setLikeCnt(commentLikeCnt);

        /** 2. 댓글 테이블 좋아요수 업데이트 **/
        int updateResult = commentDao.updateCommentLikeCnt(commentDto);

        /** 3. 업데이트 결과에 따른 rollback **/
        if (updateResult < 1 && likeType.equals(COMMENT_LIKE)) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_FAIL); // 댓글 좋아요를 실패하였습니다.

        } else if (updateResult < 1 && likeType.equals(COMMENT_LIKE_CANCEL)) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_CANCEL_FAIL);// 댓글 좋아요 취소를 실패하였습니다.
        }
    }


    /**************************************************************************************
     * Delete
     **************************************************************************************/

    /**
     * 댓글 삭제
     *
     * @param commentDto : idx(댓글 idx), communityContentsIdx(커뮤니티 게시물 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void deleteComment(CommunityCommentDto commentDto) {

        /** 댓글 삭제 유효성 검사 **/
        deleteCommentValidate(commentDto);

        long idx = commentDto.getIdx();                           // 댓글 idx
        long communityIdx = commentDto.getCommunityContentsIdx(); // 커뮤니티 게시물 idx

        // 댓글 삭제(view, state 0으로 업데이트)
        int result = commentDao.deleteComment(commentDto);

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_DELETE_FAIL); // 댓글 삭제를 실패하였습니다.
        }

        // 삭제한 댓글 parentIdx & comment_cnt 조회
        CommunityCommentDto communityCommentDto = commentDaoSub.getCommentParentAndCommentCnt(idx);

        long parentIdx = communityCommentDto.getParentIdx();  // 부모 댓글 idx

        int commentCnt = communityCommentDto.getCommentCnt(); // 대댓글 개수

        result = 0;
        int updateCommentCnt = 1; // contents_info 테이블에 업데이트할 댓글 수

        // 삭제한 댓글이 댓글이고 대댓글이 달린경우
        if (parentIdx == 0 && commentCnt > 0) {
            result = commentDao.deleteReplyComment(idx); // 대댓글 view 업데이트
            updateCommentCnt += commentCnt;              // 대댓글 수 더하기

            // 삭제한 댓글이 댓글이고 대댓글이 없는 경우
        } else if (parentIdx == 0 && commentCnt == 0) {
            result = 1;

        }
        // 삭제한 댓글이 대댓글인 경우
        else if (parentIdx != 0) {
            int replyCommentCnt = commentDao.getReplyCommentCntByIdx(parentIdx); // 대댓글 개수

            // 댓글의 대댓글 개수 업데이트
            communityCommentDto.setCommentCnt(null);
            communityCommentDto.setCommentCnt(replyCommentCnt); // 조회한 대댓글 개수 set
            result = commentDao.updateCommentCnt(communityCommentDto);
        }

        /** community_contents_info 테이블 댓글 수 감소 **/
        infoCommentCntUpdate(communityIdx, updateCommentCnt, COMMENT_DELETE);     // 댓글 수 감소

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_DELETE_FAIL); // 댓글 삭제를 실패하였습니다.
        }

    }

    /**
     * 댓글 신고
     *
     * @param commentDto
     */
    @Transactional
    public void reportComment(CommunityCommentDto commentDto) {

        /** 커뮤니티 게시물 IDX 유효성 검사 **/
        communityIdxValidate(commentDto.getCommunityContentsIdx());
        /** 댓글 IDX 유효성 검사 **/
        commentIdxValidate(commentDto);

        // 댓글 신고 한적 있는지 조회
        int reportCnt = commentDaoSub.getReportCnt(commentDto);

        if (reportCnt == 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_REPORT_EXIST); // 이미 신고한 댓글입니다.
        }

        // 댓글 신고하기
        commentDto.setRegdate(dateLibrary.getDatetime()); // 현재 시간 set

        try {
            int result = commentDao.insertCommentReport(commentDto);

            if (result < 1) {
                throw new CustomException(CustomError.COMMUNITY_COMMENT_REPORT_FAIL); // 댓글 신고를 실패하였습니다.
            }
        } catch (DuplicateKeyException e) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_REPORT_EXIST); // 이미 신고한 댓글입니다.
        }

    }

    /**************************************************************************************
     * Update
     **************************************************************************************/

    /**
     * 커뮤니티 게시물 댓글 수 업데이트 (community_contents_info 테이블)
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param updateCnt    : 업데이트 할 댓글 수
     */
    private void infoCommentCntUpdate(Long communityIdx, int updateCnt, String type) {

        // 커뮤니티 게시물 상세정보 조회
        CommunityContentDto communityDto = communityDao.getContentsInfoByIdx(communityIdx);

        int contentsCommentCnt = communityDto.getCommentCnt();      // 게시물 댓글 수
        communityDto.setIdx(communityIdx);                          // 커뮤니티 게시물 idx set

        // 댓글 등록
        if (type.equals(COMMENT_REGIST)) {
            communityDto.setCommentCnt(contentsCommentCnt + updateCnt); // 댓글 수 증가
            // 게시물 상세 댓글 수 증가
            int result = communityDao.updateContentsCommentCnt(communityDto);

            if (result < 1) {
                throw new CustomException(CustomError.COMMUNITY_COMMENT_INFO_REGIST_FAIL); // 댓글 등록에 실패하였습니다.
            }

            // 댓글 삭제
        } else if (type.equals(COMMENT_DELETE)) {
            communityDto.setCommentCnt(contentsCommentCnt - updateCnt); // 댓글 수 감소

            // 게시물 상세 댓글 수 감소
            int result = communityDao.updateContentsCommentCnt(communityDto);

            if (result < 1) {
                throw new CustomException(CustomError.COMMUNITY_COMMENT_INFO_DELETE_FAIL); // 댓글 삭제를 실패하였습니다.
            }
        }
    }

    /**************************************************************************************
     * SUB
     **************************************************************************************/


    /**
     * 좋아요 여부 불리언 변환
     *
     * @param commentList
     */
    private void likeBoolean(List<CommunityCommentDto> commentList) {
        for (CommunityCommentDto commentDto : commentList) {
            if (commentDto != null) {
                // 좋아요 안한 상태
                if (commentDto.getMemberLike() == null || commentDto.getMemberLike() == 0) {
                    commentDto.setIsMemberLike(false); // 좋아요 안함
                    commentDto.setMemberLike(null);    // 변환 후 null 처리
                    // 좋아요 상태
                } else if (commentDto.getMemberLike() == 1) {
                    commentDto.setIsMemberLike(true);  // 좋아요 함
                    commentDto.setMemberLike(null);    // 변환 후 null 처리
                }
            }
        }
    }

    /**
     * 댓글 등록일 지난 시간 변환
     * ex. 3분 전 / 1시간 전 / 4월 26일 등
     *
     * @param commentList
     */
    private void setCommentTime(List<CommunityCommentDto> commentList) {

        for (CommunityCommentDto commentDto : commentList) {

            if (commentDto != null) {

                // 댓글 등록일 지난 시간 변환
                String regdate = dateLibrary.getConvertRegdate(commentDto.getRegdate());
                commentDto.setRegdate(regdate);
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
        CommunityCommentDto commentDto = CommunityCommentDto.builder()
                .communityContentsIdx(searchDto.getCommunityIdx()) // 커뮤니티 게시물 idx
                .idx(searchDto.getIdx())                           // 부모 댓글 idx
                .build();

        // 1. 커뮤니티 게시물 IDX 유효성 검사(공통)
        communityIdxValidate(commentDto.getCommunityContentsIdx());
        // 2. 부모 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
    }

    /**
     * 커뮤니티 게시물 idx 유효성 검사(공통)
     *
     * @param idx
     */
    private void communityIdxValidate(Long idx) {

        // idx 기본 유효성 검사
        if (idx == null || idx < 1L) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_IDX_EMPTY); // 요청하신 게시물을 찾을 수 없습니다.
        }

        // idx db 조회 후 검사
        int contentsCnt = communityDaoSub.getContentsCnt(idx);

        if (contentsCnt < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_IDX_ERROR); // 요청하신 게시물을 찾을 수 없습니다.
        }
    }

    /**
     * 댓글 idx 유효성 검사(공통)
     *
     * @param commentDto
     */
    private void commentIdxValidate(CommunityCommentDto commentDto) {

        // 1. commentIdx 값 검사
        if (commentDto.getIdx() == null || commentDto.getIdx() < 1L) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_IDX_EMPTY); // 존재하지 않는 댓글입니다.
        }

        // 2. 유효한 댓글인지 db 조회
        int commentCnt = commentDaoSub.getCommentCnt(commentDto);

        // 삭제되었거나 존재하지 않는 댓글
        if (commentCnt < 1) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_IDX_ERROR); // 존재하지 않는 댓글입니다.
        }
    }

    /**
     * 댓글 등록 유효성 검사
     *
     * @param commentDto
     */
    private void registCommentValidate(CommunityCommentDto commentDto) {
        // 1. 커뮤니티 게시물 IDX 유효성 검사(공통)
        communityIdxValidate(commentDto.getCommunityContentsIdx());
        // 2. 댓글 작성 공통 유효성 검사
        writeCommentCommonValidate(commentDto);
    }

    /**
     * 대댓글 등록 유효성 검사
     *
     * @param commentDto
     */
    private void registReplyCommentValidate(CommunityCommentDto commentDto) {

        // 1. 댓글 등록 유효성 검사(공통)
        registCommentValidate(commentDto);
        // 2. 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
    }


    /**
     * 댓글 작성 공통 유효성 검사 (댓글 등록)
     */
    private void writeCommentCommonValidate(CommunityCommentDto commentDto) {

        // 1. 댓글 내용 빈값
        if (commentDto.getContent() == null || commentDto.getContent().isEmpty()) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_CONTENT_EMPTY); // 댓글 내용을 입력해주세요.
        }
        // 2. 댓글 최대 길이 제한
        if (commentDto.getContent().length() > MAX_COMMENT_LENGTH) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_LENGTH_ERROR); // 최대 200자 까지 입력 가능합니다.
        }

        // 회원 닉네임 조회(세션)
        String nickName = super.getMemberInfo(LOGIN_NICK);

        // 3. 닉네임 빈값
        if (nickName == null || nickName.isEmpty()) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_NICK_EMPTY); // 닉네임 설정 후 작성해주세요.
        }
    }

    /**
     * 댓글 삭제 유효성 검사
     *
     * @param commentDto
     */
    private void deleteCommentValidate(CommunityCommentDto commentDto) {
        // 1. 커뮤니티 게시물 IDX 유효성 검사(공통)
        communityIdxValidate(commentDto.getCommunityContentsIdx());
        // 2. 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(commentDto);
        // 3. 댓글 작성자와 삭제하는 회원 같은지 검사
        commentWriterValidate(commentDto);
    }

    /**
     * 댓글 작성자와 일치하는지 유효성 검사
     * (삭제)
     *
     * @param commentDto
     */
    private void commentWriterValidate(CommunityCommentDto commentDto) {

        // 1. 댓글 작성자와 수정자 같은 지 검사
        long memberIdx = commentDto.getMemberIdx(); // 수정하는 회원 idx

        // 2. 작성자 회원 idx 조회
        Long writerIdx = commentDaoSub.getCommentWriterIdx(commentDto.getIdx()); // 작성자 member_idx 조회

        // 3. 작성자가 null 이면 존재하지 않는 댓글
        if (writerIdx == null) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_IDX_ERROR);    // 존재하지 않는 댓글입니다.
        }

        // 4. 댓글 작성자 및 수정자 일치하지 않음
        if (memberIdx != (long) writerIdx) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_WRITER_DIFF);  // 본인 작성 댓글이 아닙니다.
        }
    }
}
