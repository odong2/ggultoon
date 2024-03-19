package com.architecture.admin.controllers.v1.content;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.ContentCommentDto;
import com.architecture.admin.models.dto.content.ContentDto;
import com.architecture.admin.services.content.ContentCommentService;
import com.architecture.admin.services.content.ContentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;
import static com.architecture.admin.libraries.utils.ContentUtils.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentV1Controller extends BaseController {

    private final ContentService contentService;
    private final ContentCommentService contentCommentService;
    private String searchSuccessMsg = "lang.common.success.search"; // 조회를 완료하였습니다.

    /**************************************************************************************
     * 메인 페이지
     * 최신작 정보 / 랭킹 정보 / 큐레이션
     **************************************************************************************/

    /**
     * 최신 작품 리스트 조회
     *
     * @param searchDto
     */
    @GetMapping("/new")
    public String getNewList(@ModelAttribute @Valid SearchDto searchDto,
                             HttpServletRequest request) {

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 최신작 리스트(카테고리 리스트) 조회
        JSONObject data = contentService.getCategoryContentsList(searchDto, NEW, request);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 작품 랭킹 정보 조회
     * 카테고리별(웹툰/만화/소설)로 1위 ~ 100위
     * 1순위 : 구매 건수 순
     * 2순위 : 최신 순
     *
     * @param searchDto
     */
    @GetMapping("/rank")
    public String getRankList(@ModelAttribute @Valid SearchDto searchDto,
                              HttpServletRequest request,
                              BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return displayError(result);
        }

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 랭킹 리스트(카테고리 리스트) 조회
        JSONObject data = contentService.getCategoryContentsList(searchDto, RANK, request);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 작품 큐레이션 리스트
     *
     * @param searchDto type(노출 영역), pavilionIdx(이용관 정보)
     * @param request
     */
    @GetMapping("/curation")
    public String getCurationList(@ModelAttribute @Valid SearchDto searchDto,
                                  HttpServletRequest request) {

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 큐레이션 리스트 레디스 키 조회
        String curationList = contentService.getCurationList(searchDto, request);

        return curationList;
    }

    /**************************************************************************************
     * 카테고리 리스트
     **************************************************************************************/

    /**
     * 카테고리별/장르별 컨텐츠 리스트 (+랭킹순/최신순/인기순 정렬)
     * 비성인 컨텐츠만 조회(기본값) / 성인 컨텐츠 필터 선택 시 성인 컨텐츠만 조회
     *
     * @param searchDto (회원 idx, 선택한 카테고리 idx, 장르 idx, 정렬 타입, 성인 컨텐츠 필터 선택값)
     * @return
     */
    @GetMapping()
    public String getCategoryContentsList(@ModelAttribute @Valid SearchDto searchDto,
                                  HttpServletRequest request,
                                  BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // period (1: 일간, 2: 주간, 3: 월간)
        Integer period = (request.getParameter("period") != null) ? Integer.valueOf(request.getParameter("period").toString()) : null;

        JSONObject data;
        if (period != null && period > 0) {
            // 랭킹 리스트 조회
            data = contentService.getRankingContentsList(searchDto, RANK, request);
        } else {
            // 카테고리 리스트 조회
            data = contentService.getCategoryContentsList(searchDto, CATEGORY, request);
        }

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);

    }

    /**************************************************************************************
     * 작품 상세
     **************************************************************************************/

    /**
     * 상단 작품 상세 정보
     *
     * @param contentIdx (선택한 컨텐츠 idx)
     * @return
     */
    @GetMapping("/{idx}")
    public String getContentInfo(@PathVariable(name = "idx") Integer contentIdx,
                                 HttpServletRequest request) {

        // 컨텐츠 idx set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentIdx);

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 컨텐츠 상세
        JSONObject data = contentService.getContent(searchDto, request);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 작품 제목 정보
     * 구글 검색 메타 데이터 세팅용
     *
     * @param contentIdx (선택한 컨텐츠 idx)
     * @return
     */
    @GetMapping("/{idx}/title")
    public String getContentTitle(@PathVariable(name = "idx") Integer contentIdx) {

        // 컨텐츠 idx set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentIdx);

        // 컨텐츠 제목 정보
        JSONObject data = contentService.getContentTitle(searchDto);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }
    
    /**************************************************************************************
     * 작품 회차 리스트
     * 
     * (1) 작품 상세 하단 회차 리스트
     * (2) 뷰어 내 회차 리스트
     **************************************************************************************/

    /**
     * 작품 회차 리스트
     *
     * @param contentIdx - 회차 리스트를 조회할 컨텐츠 idx
     * @param searchDto - type(API 호출 위치) / searchType(검색 유형) / sortType(정렬 타입) / page(페이지) / recordSize(한 페이지에 보여줄 회차 개수)
     * @return
     */
    @GetMapping("/{idx}/episodes")
    public String getContentEpisodeList(@PathVariable(name = "idx") Integer contentIdx,
                                        @ModelAttribute @Valid SearchDto searchDto,
                                        HttpServletRequest request,
                                        BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 컨텐츠 idx set
        searchDto.setContentsIdx(contentIdx);

        // return value
        JSONObject data;

        // 작품 상세 하단 회차 리스트
        if (searchDto.getType() == null || searchDto.getType() < 1 || searchDto.getType() == 1) {

            // 디바이스 정보 set
            String device = request.getHeader("User-Agent");

            if (isMobile(device)) {  // 모바일
                searchDto.setDevice(ORIGIN);
            } else if (isTablet(device)) {  // 태블릿
                searchDto.setDevice(ORIGIN);
            } else { // pc
                searchDto.setDevice(ORIGIN);
            }
            data = contentService.getContentEpisodeList(searchDto, request);

            // 뷰어 내 회차 리스트
        } else if (searchDto.getType() == 2) {
            data = contentService.getViewerEpisodeList(searchDto, request);

        } else {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR); // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 작품 회차 리스트
     *
     * @param contentIdx - 회차 리스트를 조회할 컨텐츠 idx
     * @param searchDto - searchType(검색 유형)
     * @param episodeIdx - 회차 idx
     * @return
     */
    @GetMapping("/{idx}/episode/{episodeIdx}")
    public String getContentEpisodeInfo(@PathVariable(name = "idx") Integer contentIdx,
                                        @PathVariable(name = "episodeIdx") Long episodeIdx,
                                        @ModelAttribute SearchDto searchDto,
                                        HttpServletRequest request,
                                        BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 컨텐츠 idx set
        searchDto.setContentsIdx(contentIdx);

        // 회차 idx set
        searchDto.setEpisodeIdx(episodeIdx);

        // return value
        JSONObject data;

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");
        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 작품 상세 하단 회차 리스트
        data = contentService.getContentEpisodeList(searchDto, request);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**************************************************************************************
     * 작품 / 작가 / 태그 검색
     **************************************************************************************/

    /**
     * 작품 검색
     *
     * @param searchDto : page / recordSize / searchWord(검색어) / searchType(검색유형 - 미리보기, 전체보기) / pavilionIdx(이용관 정보)
     * @return
     */
    @GetMapping("/search/content")
    public String getContentSearchList(@ModelAttribute @Valid SearchDto searchDto,
                                       HttpServletRequest request,
                                       BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 검색 유형 유효성 검사
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.

        } else if(!searchDto.getSearchType().equals(PREVIEW) && !searchDto.getSearchType().equals(ALL)) {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR); // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 데이터 담을 객체 생성
        JSONObject data = new JSONObject();

        // 검색 유형이 미리보기인 경우
        if (searchDto.getSearchType().equals(PREVIEW)) {

            // 작품 검색 결과 미리보기 리스트
            data = contentService.getContentSearchPreview(searchDto, request);

            // 검색 유형이 전체보기인 경우
        } else if (searchDto.getSearchType().equals(ALL)) {

            // 작품 검색 결과 전체보기 리스트
            data = contentService.getContentSearchAll(searchDto, request);
        }

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 작가 검색
     *
     * @param searchDto : page / recordSize / searchWord(검색어) / searchType(검색유형 - 미리보기, 전체보기) / pavilionIdx(이용관 정보)
     * @return
     */
    @GetMapping("/search/author")
    public String getAuthorSearchList(@ModelAttribute @Valid SearchDto searchDto,
                                      HttpServletRequest request,
                                      BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 검색 유형 유효성 검사
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.

        } else if(!searchDto.getSearchType().equals(PREVIEW) && !searchDto.getSearchType().equals(ALL)) {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR); // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 데이터 담을 객체 생성
        JSONObject data = new JSONObject();

        // 검색 유형이 미리보기인 경우
        if (searchDto.getSearchType().equals(PREVIEW)) {

            // 작가 검색 결과 미리보기 리스트
            data = contentService.getAuthorSearchPreview(searchDto, request);

            // 검색 유형이 전체보기인 경우
        } else if (searchDto.getSearchType().equals(ALL)) {

            // 작가 검색 결과 전체보기 리스트
            data = contentService.getAuthorSearchAll(searchDto, request);
        }

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 태그 검색
     *
     * @param searchDto : page / recordSize / searchWord(검색어) / searchType(검색유형 - 미리보기, 전체보기) / pavilionIdx(이용관 정보)
     * @return
     */
    @GetMapping("/search/tag")
    public String getTagSearchList(@ModelAttribute @Valid SearchDto searchDto,
                                   HttpServletRequest request,
                                   BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 디바이스 정보 set
        String device = request.getHeader("User-Agent");

        if (isMobile(device)) {  // 모바일
            searchDto.setDevice(ORIGIN);
        } else if (isTablet(device)) {  // 태블릿
            searchDto.setDevice(ORIGIN);
        } else { // pc
            searchDto.setDevice(ORIGIN);
        }

        // 검색 유형 유효성 검사
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.

        } else if(!searchDto.getSearchType().equals(PREVIEW) && !searchDto.getSearchType().equals(ALL)) {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR); // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 데이터 담을 객체 생성
        JSONObject data = new JSONObject();

        // 검색 유형이 미리보기인 경우
        if (searchDto.getSearchType().equals(PREVIEW)) {

            // 태그 검색 결과 미리보기 리스트
            data = contentService.getTagSearchPreview(searchDto, request);

            // 검색 유형이 전체보기인 경우
        } else if (searchDto.getSearchType().equals(ALL)) {

            // 태그 검색 결과 전체보기 리스트
            data = contentService.getTagSearchAll(searchDto, request);
        }

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**************************************************************************************
     * 작품 찜하기
     **************************************************************************************/

    /**
     * 작품 찜하기
     *
     * @param contentsIdx (작품 idx)
     * @return
     */
    @PostMapping("/{idx}/favorite")
    public String favoriteContent(@PathVariable(name = "idx") Integer contentsIdx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 IDX
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        ContentDto contentDto = new ContentDto();
        contentDto.setMemberIdx(memberIdx);            // 회원 idx set
        contentDto.setContentsIdx(contentsIdx);        // 컨텐츠 idx set

        // 작품 찜하기
        contentService.favoriteContent(contentDto);

        String message = super.langMessage("lang.contents.success.favorite"); // 관심 작품 목록에 추가하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 작품 찜하기 취소
     *
     * @param contentsIdx (작품 idx)
     * @return
     */
    @DeleteMapping("/{idx}/favorite")
    public String deleteFavoriteContent(@PathVariable(name = "idx") Integer contentsIdx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 IDX
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        ContentDto contentDto = new ContentDto();
        contentDto.setMemberIdx(memberIdx);            // 회원 idx set
        contentDto.setContentsIdx(contentsIdx);        // 컨텐츠 idx set

        // 작품 찜하기 취소
        contentService.deleteFavoriteContent(contentDto);

        String message = super.langMessage("lang.contents.success.favorite.cancel"); // 관심 작품 목록에서 삭제하였습니다.

        return displayJson(true, "1000", message);
    }

    /**************************************************************************************
     * 작품 신고하기
     **************************************************************************************/

    /**
     * 작품 신고하기
     *
     * @param contentsIdx (작품 idx)
     * @return
     */
    @PostMapping("/{idx}/report")
    public String reportContent(@PathVariable(name = "idx") Integer contentsIdx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 IDX
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        ContentDto contentDto = new ContentDto();
        contentDto.setMemberIdx(memberIdx);            // 회원 idx set
        contentDto.setContentsIdx(contentsIdx);        // 컨텐츠 idx set

        // 작품 신고하기
        contentService.reportContent(contentDto);

        String message = super.langMessage("lang.contents.success.report"); // 작품을 신고하였습니다.

        return displayJson(true, "1000", message);
    }

    /**************************************************************************************
     * 작품 댓글 & 대댓글
     *
     * <정책>
     * 베스트 댓글 3개는 상단 고정 ex. 1페이지 13개, 2페이지 10개, 3페이지 10개...
     * 로그인한 회원에 한해서 회원이 신고한 베스트 댓글, 댓글, 대댓글 숨김
     * 로그인한 회원에 한해서 회원이 좋아요한 베스트 댓글, 댓글, 대댓글 표시
     **************************************************************************************/

    /**
     * 베스트 댓글 리스트(페이징 X)
     *
     * @param contentIdx : 컨텐츠 idx
     * @return
     */
    @GetMapping("/{idx}/comments/preview")
    public String getPreviewContentCommentList(@PathVariable(name = "idx") Integer contentIdx) {

        // 컨텐츠 idx set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentIdx);

        // 컨텐츠 베스트 댓글 리스트
        JSONObject data = contentCommentService.getBestContentCommentList(searchDto);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 전체 댓글 리스트(페이징 O)
     *
     * @param contentIdx : 컨텐츠 idx
     * @param searchDto  : 페이지 정보, 정렬 타입
     * @return
     */
    @GetMapping("/{idx}/comments/all")
    public String getAllContentCommentList(@PathVariable(name = "idx") Integer contentIdx,
                                           @ModelAttribute @Valid SearchDto searchDto,
                                           BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return displayError(result);
        }

        // 컨텐츠 idx set
        searchDto.setContentsIdx(contentIdx);

        // 컨텐츠 댓글 리스트(전체보기)
        JSONObject data = contentCommentService.getAllContentCommentList(searchDto);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 전체 대댓글 리스트(페이징 O)
     *
     * @param contentIdx : 컨텐츠 idx
     * @param commentIdx : 댓글 idx
     * @param searchDto  : 페이지 정보
     * @return
     */
    @GetMapping("/{idx}/comments/{commentIdx}")
    public String replyCommentList(@PathVariable(name = "idx") Integer contentIdx,
                                   @PathVariable(name = "commentIdx") Long commentIdx,
                                   @ModelAttribute @Valid SearchDto searchDto,
                                   BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return displayError(result);
        }

        searchDto.setIdx(commentIdx);         // 부모 댓글 idx set
        searchDto.setContentsIdx(contentIdx); // 컨텐츠 idx set

        // 컨텐츠 대댓글 리스트
        JSONObject data = contentCommentService.getReplyCommentList(searchDto);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 댓글 등록
     *
     * @param contentsIdx       : 컨텐츠 idx
     * @param contentCommentDto : content(댓글 내용)
     * @return
     */
    @PostMapping("/{idx}/comments")
    public String registerComment(@PathVariable(name = "idx") Integer contentsIdx,
                                  @RequestBody ContentCommentDto contentCommentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        contentCommentDto.setMemberIdx(memberIdx);      // 회원 idx set
        contentCommentDto.setContentsIdx(contentsIdx);  // 컨텐츠 idx set

        // 댓글 등록
        contentCommentService.registerContentComment(contentCommentDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.contents.success.comment.register"); // 댓글을 등록하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 대댓글 등록
     *
     * @param contentsIdx       : 컨텐츠 idx
     * @param commentIdx        : 댓글 idx
     * @param contentCommentDto : content(댓글 내용)
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}")
    public String registerReplyComment(@PathVariable(name = "idx") Integer contentsIdx,
                                       @PathVariable Long commentIdx,
                                       @RequestBody ContentCommentDto contentCommentDto) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        contentCommentDto.setMemberIdx(memberIdx);      // 회원 idx set
        contentCommentDto.setContentsIdx(contentsIdx);  // 컨텐츠 idx set
        contentCommentDto.setParentIdx(commentIdx);     // 부모 댓글 번호 set

        // 대댓글 등록
        contentCommentService.registerContentReply(contentCommentDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.contents.success.comment.register"); // 댓글을 등록하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 OR 대댓글 삭제
     *
     * @param contentsIdx : 컨텐츠 idx
     * @return
     */
    @DeleteMapping("/{idx}/comments/{commentIdx}")
    public String deleteCommentOrReply(@PathVariable(name = "idx") Integer contentsIdx,
                                       @PathVariable Long commentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // contentCommentDto dto set
        ContentCommentDto contentCommentDto = ContentCommentDto.builder()
                .idx(commentIdx)            // 댓글 idx
                .contentsIdx(contentsIdx)   // 컨텐츠 idx
                .memberIdx(memberIdx)       // 회원 idx
                .build();

        // 댓글 or 대댓글 삭제
        contentCommentService.deleteCommentOrReply(contentCommentDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.contents.success.comment.delete"); // 댓글을 삭제하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 좋아요 OR 좋아요 취소
     *
     * @param contentsIdx : 컨텐츠 idx
     * @param commentIdx  : 댓글 idx
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}/like")
    public String likeComment(@PathVariable(name = "idx") Integer contentsIdx,
                              @PathVariable Long commentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        // 회원 idx
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // contentCommentDto dto set
        ContentCommentDto contentCommentDto = ContentCommentDto.builder()
                .idx(commentIdx)            // 댓글 idx
                .contentsIdx(contentsIdx)   // 컨텐츠 idx
                .memberIdx(memberIdx)       // 회원 idx
                .build();

        // 댓글 좋아요 등록
        String likeType = contentCommentService.updateCommentLike(contentCommentDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.contents.success.comment.like");     // 좋아요를 누르셨습니다.

        if (likeType.equals("likeCancel")) {
            message = super.langMessage("lang.contents.success.comment.like.cancel"); // 좋아요를 취소하셨습니다.
        }

        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 OR 대댓글 신고하기
     *
     * @param contentsIdx : 컨텐츠 idx
     * @param commentIdx  : 댓글 idx
     * @return
     */
    @PostMapping("/{idx}/comments/{commentIdx}/report")
    public String reportContentCommentOrReply(@PathVariable(name = "idx") Integer contentsIdx,
                                              @PathVariable Long commentIdx) {

        // 세션 체크
        super.checkSession();

        // 세션에서 가져온 회원 IDX
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        // contentCommentDto dto set
        ContentCommentDto contentCommentDto = ContentCommentDto.builder()
                .idx(commentIdx)            // 댓글 idx
                .contentsIdx(contentsIdx)   // 컨텐츠 idx
                .memberIdx(memberIdx)       // 회원 idx
                .build();

        // 댓글 or 대댓글 신고하기
        contentCommentService.reportContentCommentOrReply(contentCommentDto);

        // 결과 메세지 처리
        String message = super.langMessage("lang.contents.success.comment.report"); // 댓글을 신고하였습니다.

        return displayJson(true, "1000", message);
    }

}
