package com.architecture.admin.services.episode;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.episode.EpisodeDao;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeCommentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.daosub.purchase.PurchaseDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.episode.EpisodeImgDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.architecture.admin.config.SessionConfig.IDX;
import static com.architecture.admin.config.SessionConfig.MEMBER_INFO;
import static com.architecture.admin.libraries.utils.ContentUtils.*;
import static com.architecture.admin.libraries.utils.EventUtils.*;

@Service
@RequiredArgsConstructor
public class EpisodeService extends BaseService {

    private final EpisodeDao episodeDao;
    private final EpisodeDaoSub episodeDaoSub;
    private final EpisodeCommentDaoSub commentDaoSub;
    private final ContentDaoSub contentDaoSub;
    private final PurchaseDaoSub purchaseDaoSub;
    private final MemberDaoSub memberDaoSub;
    private final S3Library s3Library;

    /**
     * 회차 뷰어
     *
     * @param episodeDto : idx(회차 idx), device(요청 기기 정보), route(뷰어 진입 경로)
     * @return
     */
    @Transactional
    public JSONObject getViewer(EpisodeDto episodeDto, HttpServletRequest request) {

        Long idx = episodeDto.getIdx();

        /** 에피소드 IDX 유효성 검사 **/
        episodeIdxValidate(idx);

        // return value
        int episodeType = 0; // 1: 무료 회차, 2 : 무료 이벤트 회차, 3 : 구매회차

        // 해당 회차 뷰어에서 필요한 정보 조회
        // (contentsTitle, title, episodeIdx, checkLogin, contentIdx, adult, sort, episodeNumber)
        EpisodeDto episodeInfo = episodeDaoSub.getEpisodeInfo(idx);
        episodeInfo.setDevice(episodeDto.getDevice());     // 디바이스 set
        episodeInfo.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set (pubDate 비교 시 사용)
        episodeInfo.setRoute(episodeDto.getRoute()); // 진입 경로 구분값 set

        // 컨텐츠 idx set
        Integer contentIdx = episodeInfo.getContentIdx();
        episodeDto.setContentIdx(contentIdx);

        // 해당 회차 순서(sort) 조회
        int episodeSort = episodeInfo.getSort();

        // 무료 회차 범위 조회
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

        // 이벤트 무료 회차 번호 조회
        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        /** 비성인 컨텐츠 + 첫번째 회차 + 무료 OR 이벤트 무료 회차일 경우 로그인 여부 상관없이 뷰어 실행 **/
        if (episodeInfo.getAdult() == 0 && episodeSort == 1) {
            if ((episodeSort <= freeCnt) || (episodeSort <= eventFreeCnt)) {
                // 뷰어 실행 후 리턴
                return viewer(episodeInfo, episodeType);
            }
        }
        /** ///비성인 컨텐츠 + 첫번째 회차 + 무료 OR 이벤트 무료 회차일 경우 로그인 여부 상관없이 뷰어 실행 **/

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(MEMBER_INFO);

        // 로그인이 필요한 회차인데 로그인하지 않음 -> 정책 변경 : 모든 회차는 로그인해야 열람 가능
        if (memberInfo == null) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해 주세요.
        }

        // 해당 컨텐츠 성인 컨텐츠인지 체크
        int adult = episodeInfo.getAdult();

        /** 해당 회차 성인 작품 **/
        if (adult == 1) {
            // 비로그인
            if (memberInfo == null) {
                throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해 주세요.
                // 로그인 했지만 성인 아님
            } else if (Long.valueOf(getMemberInfo(ADULT)) == 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        /** 전작품 무료 감상 이벤트 진행중일 경우 **/
        if (EVENT_STATE && dateLibrary.checkEventState(START_FREE_VIEW, END_FREE_VIEW)) {

            // 세션에서 가져온 회원 idx
            Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

            // OTT 접속 토큰이 있는 경우
            if (!super.getOttVisitToken(request).equals("")) {
                // 이벤트 무료 회차 set
                episodeType = EPISODE_TYPE_EVENT_FREE;
                // 뷰어 실행 후 리턴
                return viewer(episodeInfo, episodeType);

                // OTT 유입을 통해 꿀툰 가입한 회원인 경우
            } else if (!memberDaoSub.getMemberSite(memberIdx).equals("ggultoon")) {
                // 이벤트 무료 회차 set
                episodeType = EPISODE_TYPE_EVENT_FREE;
                // 뷰어 실행 후 리턴
                return viewer(episodeInfo, episodeType);
            }
        }

        /******************** 구분선 이후 부터는 구매 여부를 판단하므로 회원만 가능 *************************/

        if (memberInfo == null) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해 주세요.
        }

        // 회차 이벤트 대여 코인 조회
        Integer eventRentCoin = episodeDaoSub.getEpisodeEventRentCoin(episodeDto);

        /** 회차 이벤트 대여 무료인 경우 **/
        if (eventRentCoin != null) {
            if (eventRentCoin == 0) {
                // 무료 회차 set
                episodeType = EPISODE_TYPE_EVENT;
                // 뷰어 실행 후 리턴
                return viewer(episodeInfo, episodeType);
            }
        }

        /** 무료 회차 범위에 해당 회차 번호가 포함될 경우 **/
        if (episodeSort <= freeCnt) {
            // 무료 회차 set
            episodeType = EPISODE_TYPE_FREE;
            // 뷰어 실행 후 리턴
            return viewer(episodeInfo, episodeType);

            // 해당 회차가 이벤트 무료 회차일 경우
        } else if (episodeSort <= eventFreeCnt) {
            // 이벤트 무료 회차 set
            episodeType = EPISODE_TYPE_EVENT_FREE;
            // 뷰어 실행 후 리턴
            return viewer(episodeInfo, episodeType);
        }

        // purchase 테이블에서 구매 여부 판단
        Long memberIdx = Long.valueOf(getMemberInfo(IDX)); // 회원 idx
        PurchaseDto purchaseDto = PurchaseDto.builder()
                .memberIdx(memberIdx)                      // 회원 idx
                .contentsIdx(contentIdx)                   // 컨텐츠 idx
                .episodeIdx(idx)                           // 에피소드 idx
                .nowDate(dateLibrary.getDatetime())        // 현재 시간
                .build();

        /** 회원 구매 내역에서 해당 회차가 유효한지 조회 **/
        int purchaseCnt = purchaseDaoSub.getPurchaseCnt(purchaseDto);

        // 회원이 해당 회차를 보유하고 있지 않을 경우
        if (purchaseCnt < 1) {
            throw new CustomException(CustomError.EPISODE_VIEWER_NOT_PURCHASE); // 구매 후 이용이 가능합니다.
        }

        // 구매 회차 set
        episodeType = EPISODE_TYPE_PURCHASE;

        // 뷰어 실행 후 리턴
        return viewer(episodeInfo, episodeType);
    }

    /**
     * 웹툰, 만화, 소설 판단하여 뷰어 실행
     *
     * @param episodeDto  : idx(회차 번호), contentIdx(컨텐츠 idx), category(1:웹툰,2:만화,3:소설),
     *                    sort(회차 순서), device(요청 기기), route(진입 경로 구분값)
     * @param episodeType
     * @return
     */
    private JSONObject viewer(EpisodeDto episodeDto, int episodeType) {

        // return value
        JSONObject jsonData = null;
        // 카테고리(1: 웹툰, 2: 만화, 3: 소설)
        int category = episodeDto.getCategoryIdx();

        /** 1. 뷰어 실행 **/
        // 소설 뷰어
        if (category == CATEGORY_NOVEL) {
            jsonData = novelViewer(episodeDto, episodeType);

            // 웹툰, 만화
        } else {
            jsonData = webToonAndComicViewer(episodeDto, episodeType);
        }

        if (jsonData.length() > 0) {
            /** 2. 뷰 카운트 증가 **/
            episodeViewCntPlus(episodeDto.getIdx());
            Object memberInfo = session.getAttribute(MEMBER_INFO);
            // 로그인 상태
            if (memberInfo != null) {
                Long memberIdx = Long.valueOf(getMemberInfo(IDX)); // 회원 idx
                episodeDto.setMemberIdx(memberIdx);
                /** 3. 최근 본 회차 업데이트 (해당 회차가 휴재 OR 공지가 아닐 때만) **/
                if (episodeDto.getEpisodeNumber() != 0) {
                    memberLastViewUpdate(episodeDto);
                }
            }
        }
        return jsonData;
    }

    /**
     * 회차 뷰 카운트 증가
     *
     * @param idx : 회차 idx
     */
    private void episodeViewCntPlus(Long idx) {

        // 뷰 카운트 조회
        int viewCnt = episodeDaoSub.getEpisodeViewCnt(idx);
        viewCnt = viewCnt + 1; // 뷰 1 증가

        EpisodeDto episodeDto = EpisodeDto.builder()
                .idx(idx)
                .view(viewCnt).build();

        // 에피소드 뷰 카운트 증가
        episodeDao.updateViewCnt(episodeDto);
    }

    /**
     * 최근 본 회차 업데이트
     *
     * @param episodeDto : contentIdx(컨텐츠 idx), memberIdx(회원 idx)
     */
    private void memberLastViewUpdate(EpisodeDto episodeDto) {

        // 회원이 해당 컨텐츠 본적 있는 지 조회(본 적 업으면 0 리턴)
        int lastViewCnt = episodeDaoSub.getMemberLastViewCnt(episodeDto);
        episodeDto.setRegdate(dateLibrary.getDatetime());

        // 해당 컨텐츠 본적 있음
        if (lastViewCnt > 0) {
            // 업데이트
            episodeDao.updateMemberLastView(episodeDto);
            // 해당 컨텐츠 본적 없음
        } else {
            // 등록
            episodeDao.insertMemberLastView(episodeDto);
        }
    }

    /**
     * 웹툰, 만화 뷰어
     *
     * @param episodeDto
     * @return
     */
    private JSONObject webToonAndComicViewer(EpisodeDto episodeDto, int episodeType) {

        // return value
        JSONObject jsonData = new JSONObject();

        Long idx = episodeDto.getIdx(); // 에피소드 idx
        int categoryIdx = episodeDto.getCategoryIdx(); // 카테고리 idx
        String category = ""; // 카테고리 텍스트

        // 뷰어 이미지 리스트
        List<EpisodeImgDto> viewerImgList = null;

        // 웹툰
        if (categoryIdx == CATEGORY_WEBTOON) {
            category = super.langMessage("lang.episode.webtoon");
            // 웹툰 회차 뷰어 이미지 리스트 조회
            viewerImgList = episodeDaoSub.getEpisodeWebtoonImgList(episodeDto);
            // 만화
        } else if (categoryIdx == CATEGORY_COMIC) {
            category = super.langMessage("lang.episode.comic");
            // 만화 회차 뷰어 이미지 리스트 조회
            viewerImgList = episodeDaoSub.getEpisodeComicImgList(episodeDto);
        }

        // 뷰어 이미지 full url 세팅
        setEpisodeImgFulUrl(viewerImgList);

        // 2. 회차 댓글 개수
        SearchDto searchDto = new SearchDto();
        searchDto.setEpisodeIdx(idx); // 회차 idx set

        // 세션 정보 가져오기
        Object memberInfo = session.getAttribute(MEMBER_INFO);

        // 로그인 상태일 경우
        if (memberInfo != null) {
            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
            searchDto.setMemberIdx(memberIdx); // 회원 idx set
            episodeDto.setMemberIdx(memberIdx); // 회원 idx set
        }
        // 댓글 개수
        int commentCnt = commentDaoSub.getCommentsTotalCnt(searchDto);

        // 에피소드 타입 텍스트 변환 후 리턴
        String episodeTypeText = episodeTypeText(episodeType);

        // 3. 이전 회차 & 다음 회차 idx 조회
        int sort = episodeDto.getSort();
        int contentIdx = episodeDto.getContentIdx();

        // 회차 조회용
        EpisodeDto searchIdxDto = EpisodeDto.builder()
                .sort(sort - 1).contentIdx(contentIdx).build();

        // 이전 회차 조회
        JSONObject prevData = new JSONObject();
        Long beforeIdx = episodeDaoSub.getIdxBySort(searchIdxDto);

        // 이전 회차 -> 뷰어 실행 OR 구매 팝업 노출 판단을 위한 구분값 반환
        if (beforeIdx != null && beforeIdx > 0L) {
            prevData = checkEpisodeInfo(episodeDto, beforeIdx);
        }

        // 순서 초기화
        searchIdxDto.setSort(null);
        searchIdxDto.setSort(sort + 1); // 다음 회차

        // 다음 회차 조회
        JSONObject nextData = new JSONObject();
        Long nextIdx = episodeDaoSub.getIdxBySort(searchIdxDto);

        // 다음 회차 -> 뷰어 실행 OR 구매 팝업 노출 판단을 위한 구분값 반환
        if (nextIdx != null && nextIdx > 0L) {
            nextData = checkEpisodeInfo(episodeDto, nextIdx);
        }

        jsonData.put("category", category);                          // 카테고리
        jsonData.put("categoryCode", categoryIdx);                   // 카테고리 코드
        jsonData.put("contentIdx", episodeDto.getContentIdx());     // 컨텐츠 idx
        jsonData.put("contentTitle", episodeDto.getContentsTitle()); // 컨텐츠 제목
        jsonData.put("episodeTitle", episodeDto.getTitle());         // 회차 제목
        jsonData.put("checkLogin", episodeDto.getCheckLogin());      // 로그인 필요 여부
        jsonData.put("checkArrow", episodeDto.getCheckArrow());      // 체제 방식
        jsonData.put("sellType", episodeDto.getSellType());          // 판매 종류
        jsonData.put("sort", episodeDto.getSort());                  // 회차 순서
        jsonData.put("number", episodeDto.getEpisodeNumber());       // 현재 회차 번호
        jsonData.put("type", episodeTypeText);     // 회차 타입(무료 회차, 무료 이벤트 회차, 구매 회차)
        jsonData.put("commentCnt", commentCnt);    // 댓글 개수
        jsonData.put("coverImg", "");              // 커버 이미지는 소설만 존재
        jsonData.put("imgList", viewerImgList);    // 회차 이미지 리스트
        jsonData.put("prevEpisodeInfo", prevData); // 이전 회차 정보
        jsonData.put("nextEpisodeInfo", nextData); // 다음 회차 정보
        return jsonData;
    }

    /**
     * 소설 뷰어
     *
     * @param episodeDto
     * @return
     */
    private JSONObject novelViewer(EpisodeDto episodeDto, int episodeType) {

        // return value
        JSONObject jsonData = new JSONObject();

        Long idx = episodeDto.getIdx(); // 에피소드 idx
        int categoryIdx = episodeDto.getCategoryIdx(); // 카테고리 idx

        // 1. 소설 이미지 리스트
        List<EpisodeImgDto> novelImgList = episodeDaoSub.getEpisodeNovelImgList(idx);
        // 소설은 가로, 세로 없으므로 0으로 set
        if (novelImgList != null) {
            for (EpisodeImgDto episodeImgDto : novelImgList) {
                if (episodeImgDto != null) {
                    episodeImgDto.setWidth(0);
                    episodeImgDto.setHeight(0);
                }
            }
        }
        // 2. 회차 댓글 개수
        SearchDto searchDto = new SearchDto();
        searchDto.setEpisodeIdx(idx); // 회차 idx set

        // 세션 정보 가져오기
        Object memberInfo = session.getAttribute(MEMBER_INFO);

        // 로그인 상태일 경우
        if (memberInfo != null) {
            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환
            searchDto.setMemberIdx(memberIdx); // 회원 idx set
            episodeDto.setMemberIdx(memberIdx); // 회원 idx set
        }

        int commentCnt = commentDaoSub.getCommentsTotalCnt(searchDto);

        String category = super.langMessage("lang.episode.novel"); // 소설

        // 4. 에피소드 타입 텍스트 변환 후 리턴
        String episodeTypeText = episodeTypeText(episodeType);

        // 5. 이전 회차 & 다음 회차 idx 조회
        int sort = episodeDto.getSort();
        int contentIdx = episodeDto.getContentIdx();

        // 회차 조회용
        EpisodeDto searchIdxDto = EpisodeDto.builder()
                .sort(sort - 1).contentIdx(contentIdx).build();

        // 이전 회차 조회
        JSONObject prevData = new JSONObject();
        Long beforeIdx = episodeDaoSub.getIdxBySort(searchIdxDto);

        // 이전 회차 -> 뷰어 실행 OR 구매 팝업 노출 판단을 위한 구분값 반환
        if (beforeIdx != null && beforeIdx > 0L) {
            prevData = checkEpisodeInfo(episodeDto, beforeIdx);
        }

        searchIdxDto.setSort(null);     // 순서 초기화
        searchIdxDto.setSort(sort + 1); // 다음 회차

        // 다음 회차 조회
        JSONObject nextData = new JSONObject();
        Long nextIdx = episodeDaoSub.getIdxBySort(searchIdxDto);

        // 다음 회차 -> 뷰어 실행 OR 구매 팝업 노출 판단을 위한 구분값 반환
        if (nextIdx != null && nextIdx > 0L) {
            nextData = checkEpisodeInfo(episodeDto, nextIdx);
        }

        jsonData.put("category", category);                          // 카테고리
        jsonData.put("categoryCode", categoryIdx);                   // 카테고리 idx
        jsonData.put("contentIdx", episodeDto.getContentIdx());      // 컨텐츠 idx
        jsonData.put("contentTitle", episodeDto.getContentsTitle()); // 컨텐츠 제목
        jsonData.put("episodeTitle", episodeDto.getTitle());         // 회차 제목
        jsonData.put("checkLogin", episodeDto.getCheckLogin());      // 로그인 필요 여부
        jsonData.put("checkArrow", episodeDto.getCheckArrow());      // 체제 방식
        jsonData.put("sellType", episodeDto.getSellType());          // 판매 종류
        jsonData.put("sort", episodeDto.getSort());                  // 회차 순서
        jsonData.put("number", episodeDto.getEpisodeNumber());       // 현재 회차 번호
        jsonData.put("type", episodeTypeText);   // 회차 타입(무료 회차, 무료 이벤트 회차, 구매 회차)
        jsonData.put("commentCnt", commentCnt);  // 댓글 개수
        jsonData.put("coverImg", "");   // 소설 커버 이미지
        jsonData.put("imgList", novelImgList);   // 회차 이미지 리스트
        jsonData.put("prevEpisodeInfo", prevData);  // 이전 회차 정보
        jsonData.put("nextEpisodeInfo", nextData);  // 다음 회차 정보
        return jsonData;
    }

    /**
     * 뷰어 회차 리스트
     *
     * @param searchDto : episodeIdx, page, recordSize
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getEpisodeList(SearchDto searchDto) {

        /** 에피소드 IDX 유효성 검사 **/
        episodeIdxValidate(searchDto.getEpisodeIdx());
        Long idx = searchDto.getEpisodeIdx();

        // return value
        JSONObject jsonData = new JSONObject();

        // 컨텐츠 idx 조회
        Integer contentIdx = contentDaoSub.getContentsIdxByEpisodeIdx(idx);
        searchDto.setContentsIdx(contentIdx);
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 회차 리스트 개수 조회
        int totalCnt = episodeDaoSub.getEpisodeTotalCntBySearchDto(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);
        List<EpisodeDto> episodeList = null;

        // 회차 리스트 존재
        if (totalCnt > 0) {
            // 회차 리스트 조회
            episodeList = episodeDaoSub.getEpisodeList(searchDto);
            // 회차 번호 문자변환
            episodeNumberText(episodeList);

            /** 1. 무료 회차 조회 **/
            int freeCnt = episodeDaoSub.getFreeEpisodeCnt(contentIdx);

            /** 2. 이벤트 무료 회차 조회 **/
            EpisodeDto episodeDto = EpisodeDto.builder().
                    contentIdx(contentIdx).nowDate(dateLibrary.getDatetime()).build();

            int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);
            // 무료 회차 최대값
            int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

            // 회차 무료여부 SET
            for (EpisodeDto dto : episodeList) {
                if (dto.getSort() <= epFreeMaxSort) {
                    dto.setIsEpisodeFree(true); // 회차 무료
                } else {
                    dto.setIsEpisodeFree(false); // 유료 회차
                }
            }

            // 로그인한 회원 정보(로그인 or 비로그인)
            Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

            // 로그인
            if (memberInfo != null) {
                String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
                Long memberIdx = Long.valueOf(stringMemberIdx); // Long 형변환

                PurchaseDto purchaseDto = PurchaseDto.builder()
                        .contentsIdx(contentIdx)
                        .memberIdx(memberIdx)
                        .nowDate(dateLibrary.getDatetime()).build();

                // 보유한 회차 idx 리스트 조회
                List<Long> purchaseIdxList = purchaseDaoSub.getEpisodeIdxListFromPurchase(purchaseDto);

                // 보유 회차 존재
                for (EpisodeDto epDto : episodeList) {
                    for (int j = 0; j < purchaseIdxList.size(); j++) { // 보유중 회차 List
                        long epIdx = epDto.getIdx();                   // 회차 idx
                        long purchaseIdx = purchaseIdxList.get(j);     // 회원 보유중 회차 idx
                        // 회차 보유 중 set
                        if (epIdx == purchaseIdx) {
                            epDto.setIsMemberPurchase(true);
                            purchaseIdxList.remove(j);
                            break;
                        }
                    }
                    // 회원 보유회차 List 사이즈가 0이면 break
                    if (purchaseIdxList.size() == 0) {
                        break;
                    }
                } // end of for

                // 미보유 회차
                for (EpisodeDto dto : episodeList) {
                    if (dto.getIsMemberPurchase() == null) {
                        dto.setIsMemberPurchase(false); // 미보유
                    }
                }
                // 비로그인
            } else {
                // 미보유 회차
                for (EpisodeDto dto : episodeList) {
                    dto.setIsMemberPurchase(false); // 미보유
                }
            }
            jsonData.put("params", new JSONObject(searchDto));
        }

        jsonData.put("episodeList", episodeList);

        return jsonData;
    }

    /**************************************************************************************
     * Select
     **************************************************************************************/


    /**************************************************************************************
     * Insert
     **************************************************************************************/


    /**************************************************************************************
     * SUB
     **************************************************************************************/

    /**
     * 회차 뷰어 이미지 fulUrl 세팅
     * 이미지 리사이징 필요 - s3Library.getThumborFullUrl
     *
     * @param episodeWidthImgList
     */
    private void setEpisodeImgFulUrl(List<EpisodeImgDto> episodeWidthImgList) {

        if (episodeWidthImgList != null && !episodeWidthImgList.isEmpty()) {

            for (EpisodeImgDto episodeImgDto : episodeWidthImgList) {
                if (episodeImgDto.getUrl() != null) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("fileUrl", episodeImgDto.getUrl());     // 회차 이미지 url
                    map.put("width", episodeImgDto.getWidth());     // 회차 이미지 가로 사이즈
                    map.put("height", episodeImgDto.getHeight());   // 회차 이미지 세로 사이즈

                    String fullUrl = s3Library.getThumborFullUrl(map);
                    episodeImgDto.setUrl(fullUrl);
                }
            }
        }
    }

    /**
     * 감상하려는 회차 정보 조회
     * 무료로 이용할 수 있는 회차인지 OR 구매 후 이용 가능한 회차인지에 대한 구분값 + 회차 가격 정보 반환
     *
     * @param episodeDto : 현재 뷰어 실행 중인 회차 정보
     * @param episodeIdx : 열람할 회차 idx (이전 회차, 다음 회차)
     */
    private JSONObject checkEpisodeInfo(EpisodeDto episodeDto, Long episodeIdx) {

        // 감상할 회차 정보 조회 (+이벤트 할인 회차 정보 포함)
        EpisodeDto episodeInfo = episodeDaoSub.getEpisodeInfo(episodeIdx);
        episodeInfo.setMemberIdx(episodeDto.getMemberIdx());// 회원 idx set
        episodeInfo.setNowDate(dateLibrary.getDatetime());  // 현재 시간 set
        episodeInfo.setShowViewer(false);                   // 무료로 이용 가능 회차 여부 기본값 set
        episodeInfo.setIsEpisodeEventDiscount(false);       // 이벤트 할인 여부 기본값 set

        // 회차 번호 문자 변환 set
        if (episodeInfo.getCategoryIdx() == 2) { // 카테고리가 만화일 경우
            episodeInfo.setEpisodeNumTitle(episodeInfo.getEpisodeNumber() + "권");
        } else {
            episodeInfo.setEpisodeNumTitle(episodeInfo.getEpisodeNumber() + "화");
        }

        // 진입경로 값이 없을 경우 빈 값으로 set
        if (episodeDto.getRoute() == null || episodeDto.getRoute().isEmpty()) {
            episodeDto.setRoute("");
        }

        /** 0. 감상할 회차가 로그인이 필요한 회차인지 체크 **/
        if (episodeInfo.getCheckLogin() == 1) {

            // 세션 정보 가져오기
            Object memberInfo = session.getAttribute(MEMBER_INFO);
            
            // 비로그인 상태일 때
            if (memberInfo == null) {
                return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
            }
        }

        /** 1. 감상할 회차가 구매한 회차인지 체크 - member_purchase 테이블 조회 **/
        if (episodeDto.getMemberIdx() != null && episodeDto.getMemberIdx() > 0L) {

            // dto set
            PurchaseDto purchaseDto = PurchaseDto.builder()
                    .contentsIdx(episodeInfo.getContentIdx())
                    .memberIdx(episodeInfo.getMemberIdx())
                    .episodeIdx(episodeInfo.getIdx())
                    .nowDate(dateLibrary.getDatetime()).build();

            // 감상할 회차 구매 여부 조회
            int purchaseCnt = purchaseDaoSub.getPurchaseCnt(purchaseDto);

            // 해당 회차를 대여 또는 소장한 경우
            if (purchaseCnt > 0) {
                episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
            }
        }

        /** 2. 감상할 회차가 무료 회차인지 체크 - contents_event_free 테이블 조회 **/
        // 감상할 회차 무료 OR 이벤트 무료 여부 조회
        EpisodeDto freeInfo = episodeDaoSub.getEpisodeFreeInfo(episodeDto.getContentIdx());

        if (episodeInfo.getSort() <= freeInfo.getFreeEpisodeCnt()) {
            episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
            return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
        }

        /** 3. 감상할 회차가 이벤트 무료 회차인지 체크 - contents_event_free 테이블 조회 **/
        if (freeInfo.getEventFreeUsed() == 1) { // 감상할 회차가 이벤트가 진행중일 때

            String startdate = freeInfo.getStartdate(); // 이벤트 시작일
            String enddate = freeInfo.getEnddate();     // 이벤트 종료일

            // 현재 날짜 기준 이벤트 진행중인 경우
            if (startdate != null && !startdate.isEmpty() && enddate != null && !enddate.isEmpty()) {
                if (dateLibrary.checkEventState(startdate, enddate)) {

                    int min = freeInfo.getFreeEpisodeCnt();
                    int max = freeInfo.getFreeEpisodeCnt() + (freeInfo.getEventFreeEpisodeCnt() - freeInfo.getFreeEpisodeCnt());

                    if (min < episodeInfo.getSort() && episodeInfo.getSort() <= max) {
                        episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                        return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                    }
                }
            }
        }

        /** 4. 감상할 회차가 이벤트 0코인 할인 회차인지 체크 - episode_event_coin 테이블 조회 **/
        String discountStartdate = episodeInfo.getStartdate(); // 할인 이벤트 시작일
        String discountEnddate = episodeInfo.getEnddate();     // 할인 이벤트 종료일

        // 현재 날짜 기준 이벤트 진행중인 경우
        if (discountStartdate != null && !discountStartdate.isEmpty() && discountEnddate != null && !discountEnddate.isEmpty()) {
            if (dateLibrary.checkEventState(discountStartdate, discountEnddate)) {

                // 이벤트 할인 회차 set
                episodeInfo.setIsEpisodeEventDiscount(true);

                // 진입 경로 : 대여일 경우 -> 이벤트 할인 대여 가격 조회
                if (episodeDto.getRoute().equals(RENT)) {
                    episodeInfo.setRoute(RENT); // 진입경로 set
                    if (episodeInfo.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                        episodeInfo.setShowViewer(true);// 무료로 이용 가능 회차 set
                        return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                    }

                    // 진입 경로 : 소장일 경우 -> 이벤트 할인 소장 가격 조회
                } else if (episodeDto.getRoute().equals(HAVE)) {
                    episodeInfo.setRoute(HAVE); // 진입경로 set
                    if (episodeInfo.getEventCoin() == 0) { // 이벤트 할인 소장 가격이 0일 경우
                        episodeInfo.setShowViewer(true);// 무료로 이용 가능 회차 set
                        return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                    }

                    // 진입 경로를 알 수 없는 경우
                } else {

                    // 해당 작품의 sell_type 값 조회
                    if (episodeInfo.getSellType() == 1) { // 대여 & 소장 모두 판매 시

                        // 로그인 상태일 경우
                        if (episodeDto.getMemberIdx() != null && episodeDto.getMemberIdx() > 0L) {

                            // dto set
                            PurchaseDto purchaseDto = PurchaseDto.builder()
                                    .contentsIdx(episodeDto.getContentIdx())
                                    .memberIdx(episodeDto.getMemberIdx())
                                    .episodeIdx(episodeDto.getIdx())
                                    .nowDate(dateLibrary.getDatetime()).build();

                            // 현재 감상 중인 회차의 구매 정보 조회
                            List<PurchaseDto> episodePurchaseInfo = purchaseDaoSub.getEpisodePurchaseInfo(purchaseDto);

                            // 현재 감상 중인 회차가 구매(대여 또는 소장)한 회차일 경우
                            if (!episodePurchaseInfo.isEmpty()) {

                                // 현재 감상 중인 회차를 소장했는지 조회 (대여, 소장 둘다 했을 경우 소장이 우선순위)
                                int isEpisodeHave = purchaseDaoSub.getEpisodeHaveInfo(purchaseDto);

                                // 소장한 회차가 맞을 경우
                                if (isEpisodeHave > 0) {
                                    episodeInfo.setRoute(HAVE); // 진입 경로 set
                                    if (episodeInfo.getEventCoin() == 0)  { // 이벤트 할인 소장 가격이 0일 경우
                                        episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                                        return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                                    }

                                    // 대여한 회차일 경우
                                } else {
                                    episodeInfo.setRoute(RENT); // 진입 경로 set
                                    if (episodeInfo.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                                        episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                                        return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                                    }
                                }
                            }
                        }
                    } else if (episodeInfo.getSellType() == 2) { // 소장으로만 판매 시

                        episodeInfo.setRoute(HAVE); // 진입 경로 set
                        if (episodeInfo.getEventCoin() == 0)  { // 이벤트 할인 소장 가격이 0일 경우
                            episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                            return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                        }

                    } else if (episodeInfo.getSellType() == 3) { // 대여로만 판매 시

                        episodeInfo.setRoute(RENT); // 진입 경로 set
                        if (episodeInfo.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                            episodeInfo.setShowViewer(true); // 무료로 이용 가능 회차 set
                            return convertToJson(episodeInfo); // 감상할 회차 정보 리턴
                        }
                    }
                }
            }
        }
        // 감상할 회차 정보 리턴
        return convertToJson(episodeInfo);
    }

    /**
     * 감상하려는 회차 정보 json 변환
     *
     * @param episodeInfo : json으로 변환할 회차 정보
     */
    private JSONObject convertToJson(EpisodeDto episodeInfo) {

        JSONObject data = new JSONObject();

        data.put("idx", episodeInfo.getIdx());
        data.put("episodeNumTitle", episodeInfo.getEpisodeNumTitle());
        data.put("episodeTitle", episodeInfo.getTitle());
        data.put("showViewer", episodeInfo.getShowViewer());
        data.put("eventDiscount", episodeInfo.getIsEpisodeEventDiscount());
        data.put("coin", episodeInfo.getCoin());
        data.put("coinRent", episodeInfo.getCoinRent());
        data.put("eventCoin", episodeInfo.getEventCoin());
        data.put("eventCoinRent", episodeInfo.getEventCoinRent());
        data.put("popupType", episodeInfo.getRoute());

        // 회차 썸네일 리사이징
        Map<String, Object> map = new HashMap<>();
        map.put("fileUrl", episodeInfo.getUrl());     // 이미지 url
        map.put("width", episodeInfo.getWidth());     // 이미지 가로 사이즈
        map.put("height", episodeInfo.getHeight());   // 이미지 세로 사이즈
        String episodeImg = s3Library.getThumborFullUrl(map);
        data.put("episodeImg", episodeImg);

        return data;
    }

    /**************************************************************************************
     * 프로트단 보낼 상태 값 변환
     **************************************************************************************/
    /**
     * 회차 번호 텍슽트 변환
     *
     * @param episodeDtoList
     */
    private void episodeNumberText(List<EpisodeDto> episodeDtoList) {
        for (EpisodeDto episodeDto : episodeDtoList) {
            episodeNumberText(episodeDto);
        }
    }

    /**
     * 회차 번호 텍스트 변환
     *
     * @param episodeDto
     */
    private void episodeNumberText(EpisodeDto episodeDto) {

        if (episodeDto != null) {
            // 회차 번호 텍스트 변환
            if (episodeDto.getCategoryIdx() == CATEGORY_COMIC) {
                episodeDto.setEpisodeNumTitle(episodeDto.getEpisodeNumber() + "권");
            } else {
                episodeDto.setEpisodeNumTitle(episodeDto.getEpisodeNumber() + "화");
            }
            episodeDto.setCategoryIdx(null); // 앞단에서 쓰지 않으므로 카테고리 null 처리
        }
    }

    /**
     * 에피소드 타입 텍스트 변환
     *
     * @param episodeType
     * @return
     */
    private String episodeTypeText(int episodeType) {

        String episodeTypeText = "";

        if (episodeType == EPISODE_TYPE_FREE) {
            episodeTypeText = super.langMessage("lang.episode.type.free");       // 무료 회차
        } else if (episodeType == EPISODE_TYPE_EVENT_FREE) {
            episodeTypeText = super.langMessage("lang.episode.type.event.free"); // 무료 이벤트 회차
        } else if (episodeType == EPISODE_TYPE_PURCHASE) {
            episodeTypeText = super.langMessage("lang.episode.type.purchase");   // 구매 회차
        } else if (episodeType == EPISODE_TYPE_EVENT) {
            episodeTypeText = super.langMessage("lang.episode.type.event");      // 이벤트 할인 회차
        } else if (episodeType == EPISODE_TYPE_TICKET_FREE) {
            episodeTypeText = super.langMessage("lang.episode.type.ticket.free");// 무료 이용권 회차
        }
        return episodeTypeText;
    }

    /**************************************************************************************
     * Validate
     **************************************************************************************/

    /**
     * 회차 idx 유효성 검사(공통)
     */
    private void episodeIdxValidate(Long idx) {
        // idx 기본 유효성 검사
        if (idx == null || idx < 1L) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

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

}
