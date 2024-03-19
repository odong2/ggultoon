package com.architecture.admin.controllers.v1.board;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.architecture.admin.services.board.BoardService;
import org.json.JSONObject;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/board")
@RequiredArgsConstructor
public class BoardV1Controller extends BaseController {

    private static final String SEARCH_SUCCESS = "lang.common.success.search"; // 조회 완료하였습니다.
    private final BoardService boardService;

    /**************************************************************************************
     * 공지사항 게시판
     **************************************************************************************/

    /**
     * 공지사항 리스트
     *
     * @param searchDto
     * @param result
     * @return
     */
    @GetMapping("/notices")
    public String getNoticeList(@ModelAttribute @Valid SearchDto searchDto,
                                BindingResult result) {

        // recordSize 유효성 체크
        if (result.hasErrors()) {
            return displayError(result);
        }

        // 공지사항 리스트 조회
        JSONObject data = boardService.getNoticeList(searchDto);

        // 결과 메세지 처리
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 공지사항 상세
     *
     * @param idx
     * @return
     */
    @GetMapping("/notices/{idx}")
    public String getNoticeInfo(@PathVariable(name = "idx") Long idx) {

        // 공지사항 상세 조회
        JSONObject data = boardService.getNoticeInfo(idx);

        // 결과 메세지 처리
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }
}
