package com.architecture.admin.controllers.v1.community;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.community.CommunityCommentDto;
import com.architecture.admin.models.dto.community.CommunityContentDto;
import com.architecture.admin.services.community.CommunityCommentService;
import com.architecture.admin.services.community.CommunityService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/community")
@RequiredArgsConstructor
public class CommunityV1Controller extends BaseController {

    private static final String SEARCH_SUCCESS = "lang.common.success.search"; // 조회 완료하였습니다.
    private final CommunityCommentService commentService;
    private final CommunityService communityService;

    /**************************************************************************************
     * 게시물
     **************************************************************************************/

    /**
     * 게시물 리스트
     *
     * @param searchDto
     * @param result
     * @return
     */
    @GetMapping("/contents")
    public String contentList(@ModelAttribute @Valid SearchDto searchDto,
                              BindingResult result) {

        if (result.hasErrors()) {
            return displayError(result);
        }

        // 게시물 리스트 조회
        JSONObject data = communityService.getContentList(searchDto);

        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 게시물 등록
     *
     * @param contentDto
     * @return
     */
    @PostMapping("/contents")
    public String registerContent(@ModelAttribute CommunityContentDto contentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        contentDto.setMemberIdx(memberIdx);

        // 게시물 등록
        communityService.registerContent(contentDto);

        String message = super.langMessage("lang.common.success.regist"); // 등록 완료하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 게시물 삭제
     *
     * @param idx
     * @return
     */
    @DeleteMapping("contents/{idx}")
    public String deleteContent(@PathVariable(name = "idx") Long idx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        CommunityContentDto contentDto = CommunityContentDto.builder()
                .idx(idx)
                .memberIdx(memberIdx).build();
        communityService.deleteContent(contentDto);
        String message = super.langMessage("lang.common.success.delete"); // 삭제 완료하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 게시물 상세
     *
     * @param idx
     * @return
     */
    @GetMapping("/contents/{idx}")
    public String getContentInfo(@PathVariable(name = "idx") Long idx) {

        JSONObject data = communityService.getContentInfo(idx);
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 게시물 좋아요
     *
     * @param idx
     * @return
     */
    @PostMapping("/contents/{idx}/like")
    public String likeContent(@PathVariable(name = "idx") Long idx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // comment dto set
        CommunityContentDto contentDto = CommunityContentDto.builder()
                .idx(idx)                           // 게시물 idx
                .memberIdx(memberIdx)               // 회원 idx
                .build();

        // 게시물 좋아요
        String likeType = communityService.updateContentLike(contentDto);

        String message = super.langMessage("lang.community.success.content.like");     // 좋아요 완료.

        if (likeType.equals("likeCancel")) {
            message = super.langMessage("lang.community.success.content.like.cancel"); // 좋아요 취소.
        }

        return displayJson(true, "1000", message);
    }

    /**
     * 게시물 신고
     *
     * @param idx : 커뮤니티 게시물 idx
     * @return
     */
    @PostMapping("/contents/{idx}/report")
    public String reportContent(@PathVariable(name = "idx") Long idx) {
        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        CommunityContentDto contentDto = CommunityContentDto.builder()
                .idx(idx)
                .memberIdx(memberIdx)
                .build();

        // 커뮤니티 게시물 신고
        communityService.reportContent(contentDto);

        String message = super.langMessage("lang.community.success.content.report"); // 댓글을 신고하였습니다.

        return displayJson(true, "1000", message);
    }


    /**************************************************************************************
     * 댓글
     **************************************************************************************/

    /**
     * 댓글 리스트
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param searchDto    : page(페이지 번호) , recordSize
     * @return
     */
    @GetMapping("/contents/{idx}/comments")
    public String commentList(@PathVariable(name = "idx") Long communityIdx,
                              @ModelAttribute @Valid SearchDto searchDto,
                              BindingResult result) {

        // 에피소드 idx set
        searchDto.setCommunityIdx(communityIdx);

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
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param commentIdx   : 댓글 idx
     * @param searchDto    : page, recordSize
     * @param result
     * @return
     */
    @GetMapping("/contents/{idx}/comments/{commentIdx}")
    public String replyCommentList(@PathVariable(name = "idx") Long communityIdx,
                                   @PathVariable Long commentIdx,
                                   @ModelAttribute @Valid SearchDto searchDto,
                                   BindingResult result) {

        if (result.hasErrors()) {
            return displayError(result);
        }

        searchDto.setCommunityIdx(communityIdx); // 커뮤니티 컨텐츠 idx
        searchDto.setIdx(commentIdx);            // 부모 댓글 번호

        // 댓글 리스트 조회
        JSONObject data = commentService.getReplyCommentList(searchDto);
        String message = super.langMessage(SEARCH_SUCCESS); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 댓글 좋아요 & 좋아요 취소
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param commentIdx   : 댓글 idx
     * @return
     */
    @PostMapping("/contents/{idx}/comments/{commentIdx}/like")
    public String likeComment(@PathVariable(name = "idx") Long communityIdx,
                              @PathVariable Long commentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // comment dto set
        CommunityCommentDto commentDto = CommunityCommentDto.builder()
                .idx(commentIdx)                    // 댓글 idx
                .communityContentsIdx(communityIdx) // 커뮤니티 컨텐츠 idx
                .memberIdx(memberIdx)               // 회원 idx
                .build();

        // 댓글 좋아요
        String likeType = commentService.updateCommentLike(commentDto);

        String message = super.langMessage("lang.community.success.comment.like");     // 좋아요 완료.

        if (likeType.equals("likeCancel")) {
            message = super.langMessage("lang.community.success.comment.like.cancel"); // 좋아요 취소.
        }

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 등록
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param commentDto   : content(댓글 내용)
     * @return
     */
    @PostMapping("/contents/{idx}/comments")
    public String registComment(@PathVariable(name = "idx") Long communityIdx,
                                @RequestBody CommunityCommentDto commentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        commentDto.setMemberIdx(memberIdx);   // 회원 idx set
        commentDto.setCommunityContentsIdx(communityIdx); // 커뮤니티 컨텐츠 idx

        // 댓글 등록
        commentService.registComment(commentDto);
        String message = super.langMessage("lang.community.success.comment.regist"); // 댓글을 등록하였습니다

        return displayJson(true, "1000", message);
    }

    /**
     * 대댓글 등록
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param commentIdx   : 댓글 idx
     * @param commentDto   : content(댓글 내용)
     * @return
     */
    @PostMapping("/contents/{idx}/comments/{commentIdx}")
    public String registReplyComment(@PathVariable(name = "idx") Long communityIdx,
                                     @PathVariable Long commentIdx,
                                     @RequestBody CommunityCommentDto commentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        commentDto.setMemberIdx(memberIdx);               // 회원 idx set
        commentDto.setCommunityContentsIdx(communityIdx); // 커뮤니티 컨텐츠 idx set
        commentDto.setIdx(commentIdx);                    // 부모 댓글 번호 set
        commentDto.setParentIdx(commentIdx);              // 부모 댓글 번호 set

        // 대댓글 등록
        commentService.registReplyComment(commentDto);
        String message = super.langMessage("lang.community.success.comment.regist"); // 댓글을 등록하였습니다

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 삭제
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @return
     */
    @DeleteMapping("/contents/{idx}/comments/{commentIdx}")
    public String deleteComment(@PathVariable(name = "idx") Long communityIdx,
                                @PathVariable Long commentIdx) {
        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        CommunityCommentDto commentDto = CommunityCommentDto.builder()
                .idx(commentIdx)
                .communityContentsIdx(communityIdx)
                .memberIdx(memberIdx)
                .build();

        // 댓글 삭제
        commentService.deleteComment(commentDto);

        String message = super.langMessage("lang.community.success.comment.delete"); // 댓글을 삭제하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 신고
     *
     * @param communityIdx : 커뮤니티 게시물 idx
     * @param commentIdx   : 댓글 idx
     * @return
     */
    @PostMapping("/contents/{idx}/comments/{commentIdx}/report")
    public String reportComment(@PathVariable(name = "idx") Long communityIdx,
                                @PathVariable Long commentIdx) {
        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        CommunityCommentDto commentDto = CommunityCommentDto.builder()
                .idx(commentIdx)
                .communityContentsIdx(communityIdx)
                .memberIdx(memberIdx)
                .build();

        // 댓글 신고
        commentService.reportComment(commentDto);

        String message = super.langMessage("lang.community.success.comment.report"); // 댓글을 신고하였습니다.

        return displayJson(true, "1000", message);
    }
}
