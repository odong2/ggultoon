package com.architecture.admin.models.daosub.board;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.board.BoardDto;

import java.util.List;

public interface BoardDaoSub {

    /**
     * 유효한 공지사항인지 조회
     *
     * @param idx : 공지사항 idx
     * @return
     */
    int getNoticeIdxCnt(Long idx);

    /**
     * 공지사항 개수 조회
     *
     * @param searchDto
     * @return
     */
    int getNoticesTotalCnt(SearchDto searchDto);

    /**
     * 공지사항 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<BoardDto> getNoticeList(SearchDto searchDto);

    /**
     * 공지사항 상세 조회
     *
     * @param idx : 공지사항 idx
     * @return
     */
    List<BoardDto> getNoticeInfo(Long idx);

    /**
     * 공지사항 리스트 전체 조회
     * 페이징 X
     */
    List<BoardDto> getAllNoticeIdxList();
}
