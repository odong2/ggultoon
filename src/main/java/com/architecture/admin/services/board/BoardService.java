package com.architecture.admin.services.board;

import com.architecture.admin.libraries.DateLibrary;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.board.BoardDto;
import com.architecture.admin.models.dto.content.BadgeDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.models.daosub.board.BoardDaoSub;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.architecture.admin.libraries.utils.BadgeUtils.*;

@RequiredArgsConstructor
@Service
public class BoardService extends BaseService {

    private final BoardDaoSub boardDaoSub;
    private final DateLibrary dataLibrary;

    /**************************************************************************************
     * 공지사항 게시판
     **************************************************************************************/

    /**
     * 공지사항 리스트
     *
     * @param searchDto
     * @return
     */
    public JSONObject getNoticeList(SearchDto searchDto) {

        // 공지사항 개수 조회
        int totalCnt = boardDaoSub.getNoticesTotalCnt(searchDto);

        // return value
        JSONObject jsonData = new JSONObject();
        List<BoardDto> noticeList = null;

        if (totalCnt > 0) {

            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            // 공지사항 리스트 조회
            noticeList = boardDaoSub.getNoticeList(searchDto);

            // 공지사항 세부 정보 set
            setNoticeInfo(noticeList);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        jsonData.put("noticeList", noticeList);

        return jsonData;
    }

    /**
     * 공지사항 상세
     *
     * @param idx
     * @return
     */
    public JSONObject getNoticeInfo(Long idx) {

        // 공지사항 idx 유효성 검사
        noticeIdxValidate(idx);

        // return value
        JSONObject jsonData = new JSONObject();
        List<BoardDto> noticeInfo;

        // 공지사항 상세 조회
        noticeInfo = boardDaoSub.getNoticeInfo(idx);

        // 공지사항 상세 정보가 있는 경우
        if (noticeInfo != null) {

            // 공지사항 세부 정보 set
            setNoticeInfo(noticeInfo);
        }
        // 공지사항 상세 정보 담기
        jsonData.put("noticeInfo", noticeInfo);

        return jsonData;
    }

    /**************************************************************************************
     * SUB
     **************************************************************************************/
    /**
     * 공지사항 세부 정보 세팅 - list
     *
     * 1. 등록일 변환
     * 2. 필독 배지 세팅
     * 3. 공지사항 내용에 포함된 태그 정보 변환
     *
     * @param noticeList
     */
    private void setNoticeInfo(List<BoardDto> noticeList) {

        for (BoardDto boardDto : noticeList) {

            /** 등록일 변환 **/
            // yy년 M월 d일 형태로 등록일 변환
            boardDto.setRegdate(dataLibrary.formatDay(boardDto.getRegdate()));

            /** 타입 구분값 문자 변환 **/
            if (boardDto.getType() == 1) { // 구분값이 TEXT일 경우
                boardDto.setTypeText("이미지 미포함");

            } else if (boardDto.getType() == 2) { // 구분값이 HTML일 경우
                boardDto.setTypeText("이미지 포함");
            }

            /** 필독 배지 세팅 **/
            boardDto.setBadgeList(new ArrayList<>());
            List<BadgeDto> badgeList = boardDto.getBadgeList();

            // 필독 공지사항일 경우
            if (boardDto.getMustRead() == 1) {

                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(CODE_MUST_READ); // 필독 배지 set
                badgeList.add(badgeDto);
            }
            // 배지로 변환했으므로 필독 상태값 null 처리
            boardDto.setMustRead(null);
        }
    }

    /**************************************************************************************
     * Validation
     **************************************************************************************/

    /**
     * 공지사항 idx 유효성 검사
     *
     * @param idx
     */
    private void noticeIdxValidate(Long idx) {

        // 공지사항 idx가 없는 경우
        if (idx == null || idx < 1) {
            throw new CustomException(CustomError.BOARD_NOTICE_IDX_EMPTY); // 요청하신 공지사항을 찾을 수 없습니다.
        }

        // 공지사항 idx가 유효하지 않은 값일 경우
        int noticeIdxCnt = boardDaoSub.getNoticeIdxCnt(idx);
        if (noticeIdxCnt < 1) {
            throw new CustomException(CustomError.BOARD_NOTICE_IDX_ERROR); // 요청하신 공지사항을 찾을 수 없습니다.
        }
    }
}
