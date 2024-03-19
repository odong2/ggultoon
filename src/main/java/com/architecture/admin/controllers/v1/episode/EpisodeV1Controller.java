package com.architecture.admin.controllers.v1.episode;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeCommentDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.services.episode.EpisodeCommentService;
import com.architecture.admin.services.episode.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/episodes")
public class EpisodeV1Controller extends BaseController {

    private static final String SEARCH_SUCCESS = "lang.common.success.search"; // 조회 완료하였습니다.
    private final EpisodeService episodeService;
    private final EpisodeCommentService commentService;


    /**************************************************************************************
     * 회차 뷰어
     **************************************************************************************/

    /**
     * 뷰어
     *
     * @param episodeIdx
     * @return
     */
    @GetMapping("/{idx}")
    public String episodeViewer(@PathVariable(name = "idx") Long episodeIdx,
                                @ModelAttribute @Valid SearchDto searchDto,
                                HttpServletRequest request) {

        EpisodeDto episodeDto = EpisodeDto.builder()
                .idx(episodeIdx) // 회차 idx set
                .route(searchDto.getSearchType()) // 진입경로 구분값 set
                .nowDate(dateLibrary.getDatetime()) // 현재 시간 set
                .build();

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");
        // 모바일
        if (isMobile(device)) {
            episodeDto.setDevice(ORIGIN);
            // 태블릿
        } else if (isTablet(device)) {
            episodeDto.setDevice(ORIGIN);
            // pc
        } else {
            episodeDto.setDevice(ORIGIN);
        }

        // 회차 뷰어
        JSONObject data = episodeService.getViewer(episodeDto, request);

        String message = super.langMessage(SEARCH_SUCCESS);

        return displayJson(true, "1000", message, data);
    }

    /**
     * 뷰어 회차 리스트
     *
     * @param episodeIdx : 회차 idx
     * @param searchDto  : page(현재 페이지), recordSize(한 페이지 개수)
     * @param result
     * @return
     */
    @GetMapping("/{idx}/list")
    public String episodeList(@PathVariable(name = "idx") Long episodeIdx,
                              @ModelAttribute @Valid SearchDto searchDto,
                              BindingResult result) {

        if (result.hasErrors()) {
            return displayError(result);
        }

        searchDto.setEpisodeIdx(episodeIdx);

        // 회차 리스트 정보 조회
        JSONObject data = episodeService.getEpisodeList(searchDto);
        String message = super.langMessage(SEARCH_SUCCESS);

        return displayJson(true, "1000", message, data);
    }


    /**************************************************************************************
     * 회차 댓글
     **************************************************************************************/

    /**
     * 댓글 리스트 조회
     *
     * @param episodeIdx : 회차 idx
     * @param searchDto  : page(페이지 번호)
     * @return
     */
    @GetMapping("/{idx}/comments")
    public String commentList(@PathVariable(name = "idx") Long episodeIdx,
                              @ModelAttribute @Valid SearchDto searchDto,
                              BindingResult result) {

        // 에피소드 idx set
        searchDto.setEpisodeIdx(episodeIdx);

        if (result.hasErrors()) {
            return displayError(result);
        }
        
        // 댓글 리스트 조회
        JSONObject data = commentService.getCommentList(searchDto);
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 대댓글 리스트
     *
     * @param episodeIdx : 회차 idx
     * @param commentIdx : 컨텐츠 idx
     * @param searchDto  : page, recordSize
     * @param result
     * @return
     */
    @GetMapping("/{idx}/comments/{commentIdx}")
    public String replyCommentList(@PathVariable(name = "idx") Long episodeIdx,
                                   @PathVariable Long commentIdx,
                                   @ModelAttribute @Valid SearchDto searchDto,
                                   BindingResult result) {

        if (result.hasErrors()) {
            return displayError(result);
        }

        searchDto.setEpisodeIdx(episodeIdx); // 회차 idx
        searchDto.setIdx(commentIdx);        // 부모 댓글 번호

        // 댓글 리스트 조회
        JSONObject data = commentService.getReplyCommentList(searchDto);
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 댓글 좋아요 & 좋아요 취소
     *
     * @param episodeIdx : 회차 idx
     * @param commentIdx : 댓글 idx
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}/like")
    public String likeComment(@PathVariable(name = "idx") Long episodeIdx,
                              @PathVariable Long commentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // comment dto set
        EpisodeCommentDto commentDto = EpisodeCommentDto.builder()
                .idx(commentIdx)        // 댓글 idx
                .episodeIdx(episodeIdx) // 에피소드 idx
                .memberIdx(memberIdx)   // 회원 idx
                .build();
        // 댓글 좋아요
        String likeType = commentService.updateCommentLike(commentDto);

        String message = super.langMessage("lang.episode.success.comment.like");     // 좋아요 완료.

        if (likeType.equals("likeCancel")) {
            message = super.langMessage("lang.episode.success.comment.like.cancel"); // 좋아요 취소.
        }

        return displayJson(true, "1000", message);
    }


    /**
     * 댓글 등록
     *
     * @param episodeIdx : 회차 idx
     * @param commentDto : content(댓글 내용)
     * @return
     */
    @PostMapping("/{idx}/comments")
    public String registComment(@PathVariable(name = "idx") Long episodeIdx,
                                @RequestBody EpisodeCommentDto commentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        commentDto.setMemberIdx(memberIdx);   // 회원 idx set
        commentDto.setEpisodeIdx(episodeIdx); // 에피스드 idx set

        // 댓글 등록
        commentService.registComment(commentDto);
        String message = super.langMessage("lang.episode.success.comment.regist"); // 댓글을 등록하였습니다

        return displayJson(true, "1000", message);
    }

    /**
     * 대댓글 등록
     *
     * @param episodeIdx : 회차 idx
     * @param commentIdx : 댓글 idx
     * @param commentDto : content(댓글 내용)
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}")
    public String registReplyComment(@PathVariable(name = "idx") Long episodeIdx,
                                     @PathVariable Long commentIdx,
                                     @RequestBody EpisodeCommentDto commentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        commentDto.setMemberIdx(memberIdx);   // 회원 idx set
        commentDto.setEpisodeIdx(episodeIdx); // 에피스드 번호 set
        commentDto.setIdx(commentIdx);        // 부모 댓글 번호 set
        commentDto.setParentIdx(commentIdx);  // 부모 댓글 번호 set
        // 대댓글 등록
        commentService.registReplyComment(commentDto);
        String message = super.langMessage("lang.episode.success.comment.regist"); // 댓글을 등록하였습니다

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 삭제
     *
     * @param episodeIdx : 에피소드 idx
     * @return
     */
    @DeleteMapping("/{idx}/comments/{commentIdx}")
    public String deleteComment(@PathVariable(name = "idx") Long episodeIdx,
                                @PathVariable Long commentIdx) {
        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        EpisodeCommentDto commentDto = EpisodeCommentDto.builder()
                .idx(commentIdx)
                .episodeIdx(episodeIdx)
                .memberIdx(memberIdx)
                .build();

        // 댓글 삭제
        commentService.deleteComment(commentDto);

        String message = super.langMessage("lang.episode.success.comment.delete"); // 댓글을 삭제하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 신고
     *
     * @param episodeIdx
     * @param commentIdx
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}/report")
    public String reportComment(@PathVariable(name = "idx") Long episodeIdx,
                                @PathVariable Long commentIdx) {
        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        EpisodeCommentDto commentDto = EpisodeCommentDto.builder()
                .idx(commentIdx)
                .episodeIdx(episodeIdx)
                .memberIdx(memberIdx)
                .build();

        // 댓글 신고
        commentService.reportComment(commentDto);

        String message = super.langMessage("lang.episode.success.comment.report"); // 댓글을 신고하였습니다.

        return displayJson(true, "1000", message);
    }

}
