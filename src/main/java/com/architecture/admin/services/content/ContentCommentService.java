package com.architecture.admin.services.content;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.content.ContentCommentDao;
import com.architecture.admin.models.daosub.content.ContentCommentDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.BadgeDto;
import com.architecture.admin.models.dto.content.ContentCommentDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.architecture.admin.config.SessionConfig.LOGIN_NICK;
import static com.architecture.admin.libraries.utils.BadgeUtils.*;

@RequiredArgsConstructor
@Service
@Transactional
public class ContentCommentService extends BaseService {

    private final ContentCommentDaoSub contentCommentDaoSub;
    private final ContentCommentDao contentCommentDao;
    private static final int MAX_COMMENT_LENGTH = 200;  // 댓글 최대 글자수
    private static final int MAX_BEST_COMMENT_COUNT = 3; // 베플 선정 개수

    /*********************************************************************
     * SELECT
     *********************************************************************/

    /**
     * 베플 댓글 리스트(로그인, 비로그인)
     * 미리보기
     * 베플 선정 기준 : 좋아요 10개 이상
     *
     * @param searchDto : contentsIdx(컨텐츠 idx)
     * @return JSONObject
     */
    @Transactional(readOnly = true)
    public JSONObject getBestContentCommentList(SearchDto searchDto) {

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(searchDto.getContentsIdx());

        //return value
        JSONObject jsonData = new JSONObject();
        List<ContentCommentDto> contentCommentList = null;

        // 컨텐츠 댓글 리스트 카운트
        int totalCount = contentCommentDaoSub.getContentCommentTotalCnt(searchDto);

        // 컨텐츠 댓글 리스트가 있을 경우
        if (totalCount > 0) {

            // 베플 3개만 노출하도록 set
            searchDto.setRecordSize(MAX_BEST_COMMENT_COUNT);

            // 로그인한 회원 정보
            Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

            if (memberInfo == null) { // 비로그인

                // 컨텐츠 베플 댓글 리스트 조회(비로그인)
                contentCommentList = contentCommentDaoSub.getBestContentCommentList(searchDto);

                // 로그인 상태 set
                jsonData.put("isLogin", false);

            } else { // 로그인

                String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
                Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
                searchDto.setMemberIdx(memberIdx);              // 회원 idx set

                // 컨텐츠 베플 댓글 리스트 조회(로그인)
                contentCommentList = contentCommentDaoSub.getLoginBestContentCommentList(searchDto);

                // 로그인 상태 set
                jsonData.put("isLogin", true);

            }
            // 베스트 댓글 추가 정보 세팅
            setCommentAndReplyInfo(contentCommentList, searchDto, BEST_COMMENT_LIST);

        }
        // list 담기
        jsonData.put("contentCommentList", contentCommentList);


        return jsonData;
    }

    /**
     * 댓글 리스트(로그인 상태일 때만 열람 가능)
     * 전체보기
     *
     * @param searchDto : contentsIdx(컨텐츠 idx), sortType(정렬 타입)
     * @return JSONObject
     */
    @Transactional(readOnly = true)
    public JSONObject getAllContentCommentList(SearchDto searchDto) {

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(searchDto.getContentsIdx());

        // 정렬 타입 유효성 체크
        sortTypeValidate(searchDto);

        // return value
        JSONObject jsonData = new JSONObject();
        List<ContentCommentDto> contentCommentList = null;

        // 세션 정보 확인
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo == null) { // 비로그인
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);  // 로그인 후 이용해주세요.

        } else { // 로그인

            // 로그인 상태 set
            jsonData.put("isLogin", true);

            // 닉네임 유무 검사
            checkIsMemberHaveNick();

            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 컨텐츠 댓글 개수 카운트
            int totalCountLogin = contentCommentDaoSub.getContentCommentTotalCnt(searchDto);

            // 컨텐츠 댓글이 있는 경우
            if (totalCountLogin > 0) {

                // paging 처리
                PaginationLibray pagination = new PaginationLibray(totalCountLogin, searchDto);
                searchDto.setPagination(pagination);

                // 컨텐츠 댓글 리스트 조회
                contentCommentList = contentCommentDaoSub.getLoginContentCommentList(searchDto);

                // 댓글 추가 정보 세팅
                setCommentAndReplyInfo(contentCommentList, searchDto, COMMENT_LIST);

                // paging 담기
                jsonData.put("params", new JSONObject(searchDto));
            }
            // list 담기
            jsonData.put("contentCommentList", contentCommentList);
        }

        return jsonData;
    }

    /**
     * 대댓글 리스트(로그인 상태일 때만 열람 가능)
     *
     * @param searchDto : parentIdx(부모 댓글 idx), contentsIdx(컨텐츠 idx)
     * @return : 대댓글 리스트
     */
    @Transactional(readOnly = true)
    public JSONObject getReplyCommentList(SearchDto searchDto) {

        // 대댓글 리스트 유효성 검사
        contentReplyListValidate(searchDto);

        // 정렬 타입 유효성 체크
        sortTypeValidate(searchDto);

        //return value
        JSONObject jsonData = new JSONObject();
        List<ContentCommentDto> contentReplyList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo == null) { // 비로그인

            throw new CustomException(CustomError.MEMBER_IDX_ERROR);  // 로그인 후 이용해주세요.

        } else { // 로그인

            // 로그인 상태 set
            jsonData.put("isLogin", true);

            // 닉네임 유무 검사
            checkIsMemberHaveNick();

            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 컨텐츠 대댓글 개수 카운트
            int totalCountLogin = contentCommentDaoSub.getContentReplyTotalCnt(searchDto);

            // 컨텐츠 대댓글이 있는 경우
            if (totalCountLogin > 0) {

                // paging 처리
                PaginationLibray pagination = new PaginationLibray(totalCountLogin, searchDto);
                searchDto.setPagination(pagination);

                // 컨텐츠 대댓글 리스트 조회
                contentReplyList = contentCommentDaoSub.getLoginContentReplyList(searchDto);

                // 대댓글 추가 정보 세팅
                setCommentAndReplyInfo(contentReplyList, searchDto, REPLY_LIST);

                // paging 담기
                jsonData.put("params", new JSONObject(searchDto));
            }
            // list 담기
            jsonData.put("contentReplyList", contentReplyList);
        }
        return jsonData;
    }

    /**************************************************************************************
     * INSERT
     **************************************************************************************/

    /**
     * 댓글 등록
     *
     * @param contentCommentDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx), content(댓글 내용)
     * @return
     */
    public void registerContentComment(ContentCommentDto contentCommentDto) {

        // 댓글 공백 제거
        contentCommentDto.setContent(contentCommentDto.getContent().trim());

        // 등록할 댓글 유효성 검사
        registerCommentValidate(contentCommentDto);

        // 댓글 등록일 set
        contentCommentDto.setRegdate(dateLibrary.getDatetime());

        // 댓글 등록
        int result = contentCommentDao.registerCommentOrReply(contentCommentDto);

        // 댓글 등록 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_REGISTER_ERROR); // 댓글 등록을 실패하였습니다.
        }
    }

    /**
     * 대댓글 등록
     *
     * @param contentCommentDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx), parentIdx(부모 댓글 idx), content(댓글 내용)
     */
    @Transactional
    public void registerContentReply(ContentCommentDto contentCommentDto) {

        // 대댓글 공백 제거
        contentCommentDto.setContent(contentCommentDto.getContent().trim());

        // 대댓글 등록 유효성 검사
        registerReplyValidate(contentCommentDto);

        // 대댓글 등록일 set
        contentCommentDto.setRegdate(dateLibrary.getDatetime());

        // 부모 댓글 정보 조회(idx, 댓글 개수, 좋아요, 그룹 번호 조회)
        Long parentIdx = contentCommentDto.getParentIdx(); // 부모 댓글 번호
        ContentCommentDto parentInfo = contentCommentDaoSub.getParentCommentInfo(parentIdx);

        // 대댓글 등록
        contentCommentDao.registerCommentOrReply(contentCommentDto);

        // 부모 댓글수 증가
        int commentCnt = parentInfo.getCommentCnt();
        Long commentIdx = parentInfo.getIdx();

        // 업데이트 할 dto set
        ContentCommentDto updateCommentDto = ContentCommentDto.builder()
                .parentIdx(commentIdx)
                .commentCnt(commentCnt + 1) // 댓글 수 +1
                .build();

        // 부모 댓글 수 update
        int result = contentCommentDao.updateCommentCnt(updateCommentDto);

        // 대댓글 등록 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_REGISTER_ERROR); // 댓글 등록을 실패하였습니다.
        }
    }

    /**
     * 댓글 좋아요 OR 좋아요 취소
     *
     * @param contentCommentDto : idx(댓글 idx), contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    @Transactional
    public String updateCommentLike(ContentCommentDto contentCommentDto) {

        // 컨텐츠 idx & 댓글 idx 유효성 검사
        contentAndCommentIdxValidate(contentCommentDto);

        // 좋아요 or 좋아요 취소할 댓글의 현재 좋아요 상태 체크
        ContentCommentDto commentLikeState = contentCommentDaoSub.getMemCommentLike(contentCommentDto);

        // update data 기본값 세팅
        String likeType = "like";
        contentCommentDto.setState(1); // 정상 set
        contentCommentDto.setRegdate(dateLibrary.getDatetime()); // 등록일 set

        if (commentLikeState == null) {  // 좋아요 누른 적이 없는 경우

            contentCommentDao.insertMemCommentLike(contentCommentDto);  // 좋아요 테이블 insert
            commentLikeCntUpdate(contentCommentDto, likeType);  // 댓글 테이블 좋아요 수 증가

        } else { // 좋아요를 누른 적이 있는 경우

            // 현재 좋아요 상태 가져오기
            int likeState = commentLikeState.getState();

            // 현재 좋아요 상태일 경우
            if (likeState == 1) {

                // 좋아요 취소 set
                likeType = "likeCancel";
                contentCommentDto.setState(0);

            }

            contentCommentDao.updateMemCommentLike(contentCommentDto); // 좋아요 상태 업데이트
            commentLikeCntUpdate(contentCommentDto, likeType); // 댓글 테이블 좋아요 개수 업데이트
        }

        return likeType;
    }

    /**
     * 댓글 OR 대댓글 신고하기
     *
     * @param contentCommentDto : idx(댓글 idx), contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void reportContentCommentOrReply(ContentCommentDto contentCommentDto) {

        // 컨텐츠 idx 유효성 검사
        contentIdxValidate(contentCommentDto.getContentsIdx());

        // 댓글 idx 유효성 검사
        commentIdxValidate(contentCommentDto);

        // 신고 기록 가져오기
        ContentCommentDto contentCommentReport = contentCommentDaoSub.getContentCommentReport(contentCommentDto);

        // 이미 신고한 댓글일 경우
        if (contentCommentReport != null) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_REPORT_DUPLE_ERROR);  // 이미 신고한 댓글입니다.
        }

        // 신고 상태 set
        contentCommentDto.setState(1);

        // 신고한 날짜 현재 시간으로 set
        contentCommentDto.setRegdate(dateLibrary.getDatetime());

        // 댓글 or 대댓글 신고하기
        int result = contentCommentDao.insertContentCommentReport(contentCommentDto);

        // 댓글 or 대댓글 신고 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_REPORT_ERROR);  // 댓글을 신고할 수 없습니다.
        }
    }

    /**************************************************************************************
     * Delete
     **************************************************************************************/

    /**
     * 댓글 OR 대댓글 삭제
     *
     * @param contentCommentDto : idx(댓글 idx), contentsIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void deleteCommentOrReply(ContentCommentDto contentCommentDto) {

        // 댓글 or 대댓글 삭제 유효성 검사
        deleteCommentOrReplyValidate(contentCommentDto);

        // 삭제일 set
        contentCommentDto.setModifyDate(dateLibrary.getDatetime());

        // 댓글 or 대댓글 삭제
        int result = contentCommentDao.deleteCommentOrReply(contentCommentDto);

        // 댓글 or 대댓글 삭제 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_DELETE_ERROR); // 댓글 삭제를 실패하였습니다.
        }

        // 삭제할 댓글 or 대댓글 정보 DB 조회
        ContentCommentDto commentInfo = contentCommentDaoSub.getCommentInfo(contentCommentDto);

        // 삭제할 댓글 or 대댓글이 있는 경우
        if (commentInfo != null) {

            if (commentInfo.getParentIdx() == 0) { // 댓글 삭제할 경우

                if (commentInfo.getCommentCnt() > 0) { // 해당 댓글에 달린 대댓글이 있을 때
                    // 삭제할 댓글에 달린 대댓글 노출값 변경 (1 -> 0)
                    result = contentCommentDao.updateReplyView(contentCommentDto);

                    // 대댓글 노출값 변경 실패 시
                    if (result < 1) {
                        throw new CustomException(CustomError.CONTENTS_COMMENT_DELETE_ERROR); // 댓글 삭제를 실패하였습니다.
                    }
                }

            } else { // 대댓글 삭제할 경우

                // 부모 댓글 정보 조회(idx, 댓글 개수, 좋아요, 그룹 번호 조회)
                ContentCommentDto parentInfo = contentCommentDaoSub.getParentCommentInfo(commentInfo.getParentIdx());

                // 업데이트 할 dto set
                ContentCommentDto updateCommentDto = ContentCommentDto.builder()
                        .parentIdx(commentInfo.getParentIdx())
                        .commentCnt(parentInfo.getCommentCnt() - 1) // 댓글 수 -1
                        .build();

                // 삭제할 대댓글의 부모 댓글 카운트 변경 (-1)
                result = contentCommentDao.updateCommentCnt(updateCommentDto);

                // 부모 댓글 카운트 변경 실패 시
                if (result < 1) {
                    throw new CustomException(CustomError.CONTENTS_COMMENT_DELETE_ERROR); // 댓글 삭제를 실패하였습니다.
                }
            }
        }
    }

    /**************************************************************************************
     * UPDATE
     **************************************************************************************/

    /**
     * 댓글 테이블 좋아요 수 업데이트
     *
     * @param contentCommentDto
     * @param likeType          : like(좋아요), likeCancel(좋아요 취소)
     */
    private void commentLikeCntUpdate(ContentCommentDto contentCommentDto, String likeType) {

        /** 1. 현재 댓글 좋아요 개수 가지고 오기 **/
        int commentLikeCnt = contentCommentDaoSub.getCommentLikeCnt(contentCommentDto);

        // 좋아요 취소
        if (likeType.equals("likeCancel")) {
            commentLikeCnt = commentLikeCnt - 1; // 좋아요 수 감소

            // 좋아요
        } else if (likeType.equals("like")) {
            commentLikeCnt = commentLikeCnt + 1; // 좋아요 수 증가
        }

        // 좋아요 개수 set
        contentCommentDto.setLikeCnt(commentLikeCnt);

        /** 2. 댓글 테이블 좋아요 개수 업데이트 **/
        int updateResult = contentCommentDao.updateCommentLikeCnt(contentCommentDto);

        /** 3. 업데이트 결과에 따른 rollback **/
        if (updateResult < 1 && likeType.equals("register")) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_LIKE_ERROR);         // 댓글 좋아요를 실패하였습니다.

        } else if (updateResult < 1 && likeType.equals("cancel")) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_LIKE_CANCEL_ERROR);  // 댓글 좋아요 취소를 실패하였습니다.
        }
    }

    /**************************************************************************************
     * SUB - 댓글 및 대댓글 표시 정보 세팅
     **************************************************************************************/

    /**
     * 댓글 및 대댓글 표시 정보 세팅
     * 비로그인 : 댓글/대댓글 등록일, 베스트 댓글 배지 세팅
     * 로그인 : 댓글/대댓글 등록일, 베스트 댓글 배지, 좋아요 세팅
     *
     * @param contentCommentList
     */
    private void setCommentAndReplyInfo(List<ContentCommentDto> contentCommentList, SearchDto searchDto, String type) {

        if (!contentCommentList.isEmpty()) {

            for (ContentCommentDto commentDto : contentCommentList) {

                // 세팅할 댓글이 있는 경우
                if (commentDto != null) {

                    /** 댓글 & 대댓글 등록일 표시 **/
                    // 등록일 변환
                    String regdate = dateLibrary.getConvertRegdate(commentDto.getRegdate());
                    commentDto.setRegdate(regdate);

                    /** 좋아요 순으로 정렬 후 상위 3개 댓글 best 배지 세팅 **/
                    if (type.equals(BEST_COMMENT_LIST)) {

                        // 베스트 댓글 idx 리스트 DB 조회
                        SearchDto dto = new SearchDto();
                        dto.setContentsIdx(searchDto.getContentsIdx());
                        dto.setRecordSize(MAX_BEST_COMMENT_COUNT); // 베플(3개)만 조회
                        List<Long> bestIdxList = contentCommentDaoSub.getCommentBestIdxList(dto);

                        if (bestIdxList.contains(commentDto.getIdx())) {

                            // 배지 리스트 생성
                            commentDto.setBadgeList(new ArrayList<>());
                            List<BadgeDto> badgeList = commentDto.getBadgeList();

                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_BEST); // best 코드 set
                            badgeList.add(badgeDto);
                        }
                    }

                    /** 로그인 상태일 때 추가 세팅 정보 **/
                    if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0) {

                        /** 회원이 좋아요 누른 댓글 & 대댓글 표시 **/
                        // 회원이 좋아요 누른 댓글 idx 리스트 조회
                        List<Long> likeIdxList = contentCommentDaoSub.getCommentLikeIdxList(searchDto.getMemberIdx());

                        if (likeIdxList.contains(commentDto.getIdx())) {
                            commentDto.setIsMemberLike(true);  // 좋아요 함
                        } else {
                            commentDto.setIsMemberLike(false); // 좋아요 안함
                        }
                    }
                }
            }
            /** 베스트 댓글 목록 -> 회원이 신고한 댓글 숨김 처리 (페이징 필요 없으므로 앞단에서 처리) **/
            if (type.equals(BEST_COMMENT_LIST)) {
                if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0) {
                    // 회원이 신고한 댓글 idx 리스트 조회
                    List<Long> reportIdxList = contentCommentDaoSub.getCommentReportIdxList(searchDto.getMemberIdx());
                    for (Long idx : reportIdxList) {
                        // 회원이 신고한 댓글 idx와 동일할 경우 리스트에서 제거
                        contentCommentList.removeIf(item -> item.getIdx() == idx);
                    }
                }
            }
        }
    }

    /**************************************************************************
     * Validation
     **************************************************************************/

    /**
     * 컨텐츠 idx 기본 유효성 체크
     *
     * @param contentIdx (선택한 컨텐츠 idx)
     */
    private void contentIdxValidate(Integer contentIdx) {

        // 선택한 컨텐츠 idx 값이 없는 경우
        if (contentIdx == null || contentIdx < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_EMPTY); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 유효한 컨텐츠인지 DB 조회하기 위해 set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentIdx); // 컨텐츠 idx set
        searchDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set (pubdate 비교용)

        // 유효한 컨텐츠 idx 값인지 DB 조회
        int contentCnt = contentCommentDaoSub.getContentCountByIdx(searchDto);

        // 유효한 컨텐츠가 아닐 경우
        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
        }
    }

    /**
     * 댓글 idx 유효성 검사(공통)
     *
     * @param contentCommentDto
     */
    private void commentIdxValidate(ContentCommentDto contentCommentDto) {

        // 댓글 idx 없는 경우
        if (contentCommentDto.getIdx() == null || contentCommentDto.getIdx() < 1L) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_IDX_EMPTY); // 존재하지 않는 댓글입니다.
        }

        // 유효한 댓글인지 DB 조회
        int commentCnt = contentCommentDaoSub.getCommentCnt(contentCommentDto);

        // 삭제되었거나 존재하지 않는 댓글일 때
        if (commentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_NOT_EXIST); // 존재하지 않는 댓글입니다.
        }
    }

    /**
     * 정렬 타입 유효성 검사
     * (댓글 리스트에서 사용)
     *
     * @param searchDto (선택한 정렬 타입 : 베플순 / 최신순 / 대댓글 많은 순)
     */
    private void sortTypeValidate(SearchDto searchDto) {

        // 선택한 정렬 타입이 없는 경우
        if (searchDto.getSortType() == null || searchDto.getSortType() < 1) {
            throw new CustomException(CustomError.SORT_NOT_EXIST); // 요청하신 정렬 정보를 찾을 수 없습니다.
        }

        // 선택한 정렬 타입이 유효하지 않을 경우 (1:베플순 / 2:최신순 / 3:대댓글 많은 순)
        if (searchDto.getSortType() < 1 || searchDto.getSortType() > 3) {
            throw new CustomException(CustomError.SORT_NOT_EXIST); // 요청하신 정렬 정보를 찾을 수 없습니다.
        }
    }

    /**
     * 대댓글 리스트 유효성 검사
     */
    private void contentReplyListValidate(SearchDto searchDto) {

        // 작품 IDX 유효성 검사(공통)
        contentIdxValidate(searchDto.getContentsIdx());

        // 부모 댓글 IDX 유효성 검사
        ContentCommentDto dto = ContentCommentDto.builder()
                .idx(searchDto.getIdx()) // 부모 댓글 idx
                .contentsIdx(searchDto.getContentsIdx()) // 컨텐츠 idx
                .build();

        commentIdxValidate(dto);
    }

    /**
     * 댓글 & 대댓글 등록 공통 유효성 검사
     *
     * @param contentCommentDto
     */
    private void registerCommentValidate(ContentCommentDto contentCommentDto) {

        // 작품 IDX 유효성 검사(공통)
        contentIdxValidate(contentCommentDto.getContentsIdx());

        // 회원 닉네임 조회
        String nickName = super.getMemberInfo(LOGIN_NICK);

        // 댓글을 등록하려는 회원이 아직 닉네임을 등록하지 않은 경우
        if (nickName == null || nickName.isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_NICK_EMPTY);    // 닉네임 등록 후 댓글 이용이 가능합니다.
        }

        // 댓글 내용이 비어있을 경우
        if (contentCommentDto.getContent() == null || contentCommentDto.getContent().isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_CONTENT_EMPTY); // 댓글 내용을 입력해주세요.
        }
        // 댓글이 작성 가능한 최대 길이를 넘겼을 경우
        if (contentCommentDto.getContent().length() > MAX_COMMENT_LENGTH) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_LENGTH_ERROR);  // 최대 200자까지 입력이 가능합니다.
        }
    }

    /**
     * 대댓글 등록 유효성 검사
     *
     * @param contentCommentDto
     */
    private void registerReplyValidate(ContentCommentDto contentCommentDto) {

        // 댓글 등록 유효성 검사(공통)
        registerCommentValidate(contentCommentDto);

        // 부모 댓글 IDX 유효성 검사
        ContentCommentDto dto = ContentCommentDto.builder()
                .idx(contentCommentDto.getParentIdx()) // 부모 댓글 idx
                .contentsIdx(contentCommentDto.getContentsIdx()) // 컨텐츠 idx
                .build();

        commentIdxValidate(dto);
    }

    /**
     * 댓글 삭제 유효성 검사
     *
     * @param contentCommentDto
     */
    private void deleteCommentOrReplyValidate(ContentCommentDto contentCommentDto) {

        // 작품 IDX 유효성 검사(공통)
        contentIdxValidate(contentCommentDto.getContentsIdx());

        // 댓글 IDX 유효성 검사(공통)
        commentIdxValidate(contentCommentDto);

        // 댓글 작성자와 삭제하는 회원 같은지 검사
        commentWriterValidate(contentCommentDto);
    }

    /**
     * 댓글 작성한 회원과 댓글 삭제하는 회원이 일치하는지 유효성 검사
     *
     * @param contentCommentDto
     */
    private void commentWriterValidate(ContentCommentDto contentCommentDto) {

        // 삭제하는 회원 idx 가져오기
        long memberIdx = contentCommentDto.getMemberIdx();

        // 작성자 회원 idx DB 조회
        Long writerIdx = contentCommentDaoSub.getCommentWriterIdx(contentCommentDto.getIdx());

        // 작성자가 없는 경우
        if (writerIdx == null) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_IDX_EMPTY);      // 존재하지 않는 댓글입니다.
        }

        // 댓글 작성한 회원과 댓글 삭제하는 회원이 일치하지 않는 경우
        if (memberIdx != writerIdx) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_WRITER_DIFF);    // 본인이 작성한 댓글만 삭제가 가능합니다.
        }
    }

    /**
     * 좋아요 등록/취소할 댓글 유효성 검사
     *
     * @param contentCommentDto
     */
    private void contentAndCommentIdxValidate(ContentCommentDto contentCommentDto) {

        // 선택한 컨텐츠 idx 값이 없는 경우
        if (contentCommentDto.getContentsIdx() == null || contentCommentDto.getContentsIdx() < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_EMPTY); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 유효한 컨텐츠인지 DB 조회하기 위해 set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentCommentDto.getContentsIdx()); // 컨텐츠 idx set
        searchDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set (pubdate 비교용)

        // 유효한 컨텐츠 idx 값인지 DB 조회
        int contentCnt = contentCommentDaoSub.getContentCountByIdx(searchDto);

        // 유효한 컨텐츠가 아닐 경우
        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 댓글 idx 없는 경우
        if (contentCommentDto.getIdx() == null || contentCommentDto.getIdx() < 1L) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_IDX_EMPTY); // 존재하지 않는 댓글입니다.
        }

        // 유효한 댓글인지 DB 조회
        int commentCnt = contentCommentDaoSub.getCommentCnt(contentCommentDto);

        // 삭제되었거나 존재하지 않는 댓글일 때
        if (commentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_COMMENT_NOT_EXIST); // 존재하지 않는 댓글입니다.
        }

        // 대댓글은 좋아요 기능 없음 -> 댓글인지 대댓글인지 판단
        long parentIdx = contentCommentDaoSub.checkIsCommentOrReply(contentCommentDto);
        if (parentIdx != 0) { // 대댓글인 경우
            throw new CustomException(CustomError.CONTENTS_REPLY_LIKE_ERROR); // 대댓글에는 좋아요를 등록할 수 없습니다.
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