package com.architecture.admin.services.content;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.content.ContentDao;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.episode.EpisodeDaoSub;
import com.architecture.admin.models.daosub.gift.GiftDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.daosub.purchase.PurchaseDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.*;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.episode.EpisodeImgDto;
import com.architecture.admin.models.dto.episode.EpisodeLastViewDto;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.architecture.admin.libraries.utils.BadgeUtils.*;
import static com.architecture.admin.libraries.utils.ContentUtils.*;
import static com.architecture.admin.libraries.utils.EventUtils.*;


@RequiredArgsConstructor
@Service
@Transactional
public class ContentService extends BaseService {

    private final ContentDaoSub contentDaoSub;
    private final EpisodeDaoSub episodeDaoSub;
    private final PurchaseDaoSub purchaseDaoSub;
    private final MemberDaoSub memberDaoSub;
    private final GiftDaoSub giftDaoSub;
    private final ContentDao contentDao;
    private final S3Library s3Library;
    private String searchSuccessMsg = "lang.common.success.search"; // 조회를 완료하였습니다.

    /**************************************************************************************
     * 작품 큐레이션 리스트
     *
     * 작품 노출 정책
     * (1) 비로그인 OR 비성인 회원 -> 비성인 작품만 노출
     * (2) 성인 회원 -> 비성인 + 성인 작품 전체 노출
     * (예외) 비로그인 상태이나, OTT에서 접속한 성인 회원 -> 비성인 + 성인 작품 전체 노출
     **************************************************************************************/

    /**
     * 작품 큐레이션 리스트
     * (1) 레디스 키가 유효한 경우 -> 기존 레디스 키 반환
     * (2) 레디스 키가 만료된 경우 -> 새로 생성한 레디스 키 반환
     *
     * @param searchDto
     */
    @Transactional(readOnly = true)
    public String getCurationList(SearchDto searchDto, HttpServletRequest request) {

        // 큐레이션 유효성 검사
        curationValidate(searchDto);

        // 현재 시간 set - (1) 큐레이션 예약 시간 확인 (2) 작품 발행일 확인
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> curationList = null;

        // 로그인한 회원 정보
        Integer memberAdult = null;
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            // 회원 성인 여부 set
            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT);
            memberAdult = Integer.valueOf(stringMemberAdult);

            if (memberAdult == 0) { // 미성년자
                jsonData.put("isMemberAdult", false);

                // 성인관 작품을 열람할 경우
                if (searchDto.getPavilionIdx() > 0) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }
            
        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                memberAdult = 1;
                searchDto.setPavilionIdx(1);

                // 성인관 작품을 열람할 경우
            } else if (searchDto.getPavilionIdx() > 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }
        // 큐레이션 노출 영역 세팅
        setCurationAreaInfo(memberInfo, searchDto);

        /******************** 속도 개선을 위해 Redis 사용 ********************/

        // 레디스 키
        String redisKey ="getCurationList" + searchDto;

        // 레디스 키 조회
        String curationListKey = getRedis(redisKey);

        // 레디스가 만료된 경우
        if (curationListKey == null || curationListKey.isEmpty()) {

            // 큐레이션 개수 카운트
            int curationCnt = contentDaoSub.getCurationTotalCnt(searchDto);

            // 큐레이션이 있는 경우
            if (curationCnt > 0) {

                // 큐레이션 리스트 조회
                curationList = contentDaoSub.getCurationList(searchDto);

                for (ContentDto curationDto : curationList) {

                    // 현재 시간 set (pubDate 비교 시 사용)
                    curationDto.setNowDate(dateLibrary.getDatetime());

                    // 회원이 로그인 여부 & 성인 여부 체크값 set
                    curationDto.setIsMemberAdult(memberAdult);

                    // 회원이 선택한 토글 옵션값 set
                    curationDto.setPavilionIdx(searchDto.getPavilionIdx());

                    // 큐레이션에 들어간 작품 리스트 set
                    List<ContentDto> contentList = contentDaoSub.getCurationContentList(curationDto);

                    if (contentList != null && !contentList.isEmpty() && contentList.size() > 0) {

                        // 이미지 fullUrl set
                        setImgFullUrl(contentList);

                        // 작가 & 태그 set
                        setAuthorAndTag(contentList);

                        // 배지 코드 set
                        setBadgeCode(contentList, CURATION);

                        // 큐레이션에 들어간 작품 리스트 set
                        curationDto.setContentList(contentList);

                        // 앞단에서 필요없는 정보 null 처리
                        curationDto.setNowDate(null);
                        curationDto.setIsMemberAdult(null);
                        curationDto.setDevice(null);
                    }
                }
                // 큐레이션에 작품이 배정되지 않은 경우 -> 해당 dto 제거
                curationList.removeIf(dto -> dto.getContentList() == null);
            }
            // 큐레이션 리스트 담기
            jsonData.put("curationList", curationList);
            
            // 레디스 키 생성
            String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.
            curationListKey = displayJson(true, "1000", message, jsonData);
            setRedis(redisKey, curationListKey,300);
        }
        // 레디스 키 반환
        return curationListKey;
    }

    /*********************************************************************
     * 작품 카테고리 리스트
     *
     * 작품 노출 정책
     * (1) 비로그인 OR 비성인 회원 -> 비성인 작품만 노출
     * (2) 성인 회원 -> 비성인 + 성인 작품 전체 노출
     * (예외) 비로그인 상태이나, OTT에서 접속한 성인 회원 -> 비성인 + 성인 작품 전체 노출
     *********************************************************************/

    /**
     * 카테고리별/장르별 컨텐츠 리스트 (+랭킹순/최신순/인기순 정렬)
     *
     * @param searchDto (디바이스 정보 & 선택한 카테고리 idx & 장르 idx & 정렬 타입)
     * @return JSONObject
     */
    @Transactional(readOnly = true)
    public JSONObject getCategoryContentsList(SearchDto searchDto, String type, HttpServletRequest request) {

        // 선택한 이용관 idx & 카테고리 idx & 장르 idx & 정렬 타입 유효성 체크
        selectInfoValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> contentsList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태
            JSONObject jsonMemberInfo = new JSONObject(memberInfo.toString());

            // 회원 idx set
            Long memberIdx = jsonMemberInfo.getLong("idx");
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회원 성인 여부 set
            Integer memberAdult = jsonMemberInfo.getInt("adult");
            searchDto.setAdult(memberAdult); // 성인 여부 set

            if (memberAdult == 0) { // 미성년자
                jsonData.put("isMemberAdult", false);

                // 성인관 작품을 열람할 경우
                if (searchDto.getPavilionIdx() > 0) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }
        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);

                // 성인관 작품을 열람할 경우
            } else if (searchDto.getPavilionIdx() > 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // rankingType 설정 - rankingType = (genreIdx * 100) + (categoryIdx * 10) + adultPavilion;
        Integer rankingType = searchDto.getPavilionIdx();
        if (searchDto.getCategoryIdx() != null) {
            rankingType += searchDto.getCategoryIdx() * 10;
        }
        if (searchDto.getGenreIdx() != null) {
            rankingType += searchDto.getGenreIdx() * 100;
        }
        if (rankingType != null) {
            searchDto.setRankingType(rankingType);
        }

        // 카테고리 리스트 카운트
        int totalCount = contentDaoSub.getCategoryContentsTotalCnt(searchDto);

        // 카테고리 리스트가 있을 경우
        if (totalCount > 0) {

            // 메인 랭킹 더보기에서 호출 시
            if (type.equals(RANK)) {
                searchDto.setRecordSize(9); // LIMIT 9

                // 메인 최신작 더보기에서 호출 시
            } else if (type.equals(NEW)) {
                searchDto.setRecordSize(20); // LIMIT 20
                searchDto.setSortType(2);

                // 카테고리 페이지에서 호출 시
            } else if (type.equals(CATEGORY)) {
                PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
                searchDto.setPagination(pagination); // 페이징 처리
            }

            // 카테고리 리스트 조회
            contentsList = contentDaoSub.getContentsList(searchDto);

            // 이미지 fullUrl 세팅
            setImgFullUrl(contentsList);

            // 작가 & 태그 set
            setAuthorAndTag(contentsList);

            // 배지 코드 set
            setBadgeCode(contentsList, CATEGORY_LIST);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        if (type.equals(RANK)) { // 메인 랭킹 더보기
            jsonData.put("rankContentsList", contentsList);
        } else if (type.equals(NEW)) { // 메인 최신작 더보기
            jsonData.put("newContentsList", contentsList);
        } else if (type.equals(CATEGORY)) { // 카테고리 페이지
            jsonData.put("categoryContentsList", contentsList);
        }
        return jsonData;
    }

    /**
     * 랭킹작 리스트
     *
     * @param searchDto
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getRankingContentsList(SearchDto searchDto, String type, HttpServletRequest request) {

        // 선택한 이용관 idx & 카테고리 idx & 장르 idx & 정렬 타입 유효성 체크
        selectInfoValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> contentsList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);
        if (memberInfo != null) { // 로그인 상태
            JSONObject jsonMemberInfo = new JSONObject(memberInfo.toString());

            // 회원 idx set
            Long memberIdx = jsonMemberInfo.getLong("idx");
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회원 성인 여부 set
            Integer memberAdult = jsonMemberInfo.getInt("adult");
            searchDto.setAdult(memberAdult); // 성인 여부 set

            if (memberAdult == 0) { // 미성년자
                jsonData.put("isMemberAdult", false);

                // 성인관 작품을 열람할 경우
                if (searchDto.getPavilionIdx() > 0) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }
        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);

                // 성인관 작품을 열람할 경우
            } else if (searchDto.getPavilionIdx() > 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // rankingType 설정 - rankingType = (genreIdx * 100) + (categoryIdx * 10) + adultPavilion;
        Integer rankingType = searchDto.getPavilionIdx();
        if (searchDto.getCategoryIdx() != null) {
            rankingType += searchDto.getCategoryIdx() * 10;
        }
        if (searchDto.getGenreIdx() != null) {
            rankingType += searchDto.getGenreIdx() * 100;
        }
        if (rankingType != null) {
            searchDto.setRankingType(rankingType);
        }

        // 랭크 개수 카운트
        Integer totalCount = contentDaoSub.getRankingContentsListTotalCnt(searchDto);

        PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
        searchDto.setPagination(pagination); // 페이징 처리

        // paging 담기
        jsonData.put("params", new JSONObject(searchDto));

        // 랭킹 리스트 조회, 조회 개수 제한
        contentsList = contentDaoSub.getRankingContentsList(searchDto);

        // 이미지 fullUrl 세팅
        setImgFullUrl(contentsList);

        // 작가 & 태그 set
        setAuthorAndTag(contentsList);

        // 배지 코드 set
        setBadgeCode(contentsList, RANKING_LIST);

        // ranking
        for (ContentDto dto : contentsList) {
            // 구매 있는 작품만 랭킹 등록
            if (dto.getEpisodeCount() != null && dto.getEpisodeCount() > 0) {
                // ranking badge
                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(CODE_TOP); // top 코드 set
                dto.getBadgeList().add(badgeDto);
            }

            if (dto.getRankingPrev() != null) {
                dto.setVariance(dto.getRankingPrev() - dto.getRanking());
            } else {
                dto.setVariance(101);
            }
        }
        jsonData.put("categoryContentsList", contentsList);

        // 랭킹 기준 시간
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
        sdf.setLenient(false);
        Calendar day = Calendar.getInstance();
        String effectiveDate = sdf.format(day.getTime());

        try {
            if (searchDto.getPeriod() > 1 && searchDto.getPeriod() <= 3) {
                effectiveDate = effectiveDate.substring(0, effectiveDate.indexOf(" ")).concat(" 00:00:00");
            } else if (!contentsList.isEmpty()) {
                effectiveDate = sdf.format(sdf.parse(contentsList.get(0).getEffectiveDate()));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        jsonData.put("effectiveDate", effectiveDate);
        return jsonData;
    }


    /*********************************************************************
     * 작품 상세
     *
     * 작품 노출 정책
     * (1) 비로그인 OR 비성인 회원 -> 비성인 작품만 노출
     * (2) 성인 회원 -> 비성인 + 성인 작품 전체 노출
     * (예외) 비로그인 상태이나, OTT에서 접속한 성인 회원 -> 비성인 + 성인 작품 전체 노출
     *********************************************************************/

    /**
     * 상단 컨텐츠 상세 정보
     *
     * @param searchDto (선택한 컨텐츠 idx & 디바이스 정보)
     * @return JSONObject
     */
    public JSONObject getContent(SearchDto searchDto, HttpServletRequest request) {

        // 컨트롤러로 리턴할 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> content = null;

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(searchDto.getContentsIdx());

        // 해당 작품이 성인 작품 또는 성인관 작품인지 체크
        ContentDto isContentAdult = contentDaoSub.checkIsContentAdult(searchDto.getContentsIdx());

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            // 회원 idx set
            Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회원 성인 여부 set
            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT);
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 성인 여부 set

            if (memberAdult == 0) { // 미성년자
                jsonData.put("isMemberAdult", false);

                // 성인 작품을 열람할 경우
                if (isContentAdult.getAdult() == 1 || isContentAdult.getAdultPavilion() == 1) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);

                // 성인 작품을 열람할 경우
            } else if (isContentAdult.getAdult() == 1 || isContentAdult.getAdultPavilion() == 1) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 컨텐츠 상세 정보 유무 조회
        int contentInfoCnt = contentDaoSub.getContentInfoCnt(searchDto);

        // 컨텐츠 상세 정보 있을 경우
        if (contentInfoCnt > 0) {

            // 컨텐츠 상세 정보 조회
            content = contentDaoSub.getContent(searchDto);

            // 해당 작품의 카테고리 정보 가져오기 (회차 번호 세팅용)
            int categoryIdx = contentDaoSub.getCategoryIdx(searchDto.getContentsIdx());

            /** 회원이 마지막으로 본 회차 IDX + 회차 번호 세팅 **/
            EpisodeLastViewDto lastView = episodeDaoSub.getMemberLastViewNumber(searchDto);
            if (lastView != null) {

                // 회원이 마지막으로 본 회차 번호 문자변환
                String lastViewEpisodeNumber;

                if (categoryIdx == CATEGORY_COMIC) { // 해당 작품의 카테고리가 만화일 경우
                    lastViewEpisodeNumber = lastView.getEpisodeNumber() + "권";

                } else { // 해당 작품의 카테고리가 웹툰 또는 소설일 경우
                    lastViewEpisodeNumber = lastView.getEpisodeNumber() + "화";
                }
                jsonData.put("lastViewEpisodeIdx", lastView.getEpisodeIdx()); // 회원이 마지막으로 본 회차 idx set
                jsonData.put("lastViewEpisodeNumber", lastViewEpisodeNumber); // 회원이 마지막으로 본 회차 번호 set

            } else { // 회원이 마지막으로 본 회차 정보가 없을 경우
                jsonData.put("lastViewEpisodeIdx", ""); // 회원이 마지막으로 본 회차 idx 빈값으로 set
                jsonData.put("lastViewEpisodeNumber", ""); // 회원이 마지막으로 본 회차 번호 빈값으로 set
            }

            /** 해당 작품의 첫번째 회차 IDX + 회차 번호 세팅 **/
            Map<String, Object> firstEpisodeInfo = episodeDaoSub.getFirstEpisodeInfo(searchDto);
            if (firstEpisodeInfo != null) {

                // 해당 작품의 첫번째 회차 번호 문자변환
                String firstEpisodeNumber;

                if (categoryIdx == CATEGORY_COMIC) { // 해당 작품의 카테고리가 만화일 경우
                    firstEpisodeNumber = firstEpisodeInfo.get("firstEpisodeNumber").toString() + "권";

                } else { // 해당 작품의 카테고리가 웹툰 또는 소설일 경우
                    firstEpisodeNumber = firstEpisodeInfo.get("firstEpisodeNumber").toString() + "화";
                }
                jsonData.put("firstEpisodeIdx", firstEpisodeInfo.get("firstEpisodeIdx")); // 작품의 첫번째 회차 idx set
                jsonData.put("firstEpisodeNumber", firstEpisodeNumber); // 작품의 첫번째 회차 번호 set

                // 첫화 구매 여부
                if (searchDto.getMemberIdx() != null) {
                    Map<String, Object> memberFirstEpisodeSearch = new HashMap<>();
                    memberFirstEpisodeSearch.put("nowDate", dateLibrary.getDatetime());
                    memberFirstEpisodeSearch.put("firstEpisodeIdx", firstEpisodeInfo.get("firstEpisodeIdx"));
                    memberFirstEpisodeSearch.put("memberIdx", Long.valueOf(getMemberInfo(SessionConfig.IDX)));
                    PurchaseDto memberFirstEpisode = purchaseDaoSub.getMemberFirstEpisode(memberFirstEpisodeSearch);
                    if (memberFirstEpisode != null) {
                        jsonData.put("memberFirstEpisodeType", memberFirstEpisode.getType()); // 첫화 구매 여부
                    }
                }

            }

            // 작품 조회수 +1 처리
            contentViewCntPlus(searchDto.getContentsIdx());

            // 이미지 fullUrl 세팅
            setImgFullUrl(content);

            // 배지 코드 set
            setBadgeCode(content, CONTENT_DETAIL);

            // 내가 찜한 작품 여부 세팅
            if (memberInfo != null) { // 로그인 상태
                setLoginContentLikeInfo(searchDto, content);
            }
        }

        /** 무료 회차 조회 **/
        int freeCnt = episodeDaoSub.getFreeEpisodeCnt(searchDto.getContentsIdx());

        /** 이벤트 무료 회차 조회 **/
        EpisodeDto episodeDto = EpisodeDto.builder()
                .contentIdx(searchDto.getContentsIdx())
                .nowDate(dateLibrary.getDatetime())
                .build();

        int eventFreeCnt = episodeDaoSub.getEventFreeEpisodeCnt(episodeDto);

        // 무료 회차 최대값
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 첫화 무료 유무
        if (epFreeMaxSort >= 1) {
            // 무료
            jsonData.put("firstEpisodeFree", true);
        } else {
            // 유료
            jsonData.put("firstEpisodeFree", false);
        }
        // data 담기
        jsonData.put("contentDetailsList", content);
        return jsonData;
    }

    /**
     * 작품 조회수 +1
     *
     * @param idx : 작품 idx
     */
    private void contentViewCntPlus(Integer idx) {

        // 작품 조회수 조회
        int viewCnt = contentDaoSub.getContentViewCnt(idx);
        viewCnt = viewCnt + 1; // 조회수 +1 set

        ContentDto contentDto = ContentDto.builder()
                .idx(idx)
                .view(viewCnt).build();

        // 작품 조회수 +1 증가
        contentDao.updateViewCnt(contentDto);
    }

    /**
     * 컨텐츠 제목 정보
     * 메타 데이터 세팅용
     *
     * @param searchDto (선택한 컨텐츠 idx)
     * @return JSONObject
     */
    public JSONObject getContentTitle(SearchDto searchDto) {

        // 컨트롤러로 리턴할 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(searchDto.getContentsIdx());

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 컨텐츠 제목 정보 조회
        String title = contentDaoSub.getContentTitle(searchDto);

        // data 담기
        jsonData.put("title", title);
        return jsonData;
    }

    /**************************************************************************************
     * 작품 회차 리스트
     *
     * (1) 작품 상세 하단 회차 리스트
     * (2) 뷰어 내 회차 리스트
     **************************************************************************************/

    /**
     * 작품 상세 하단 회차 리스트
     *
     * 작품 노출 정책
     * (1) 비로그인 OR 비성인 회원 -> 비성인 작품만 노출
     * (2) 성인 회원 -> 비성인 + 성인 작품 전체 노출
     * (예외) 비로그인 상태이나, OTT에서 접속한 성인 회원 -> 비성인 + 성인 작품 전체 노출
     *
     * @param searchDto (선택한 컨텐츠 idx & 호출 위치 & 디바이스 정보 & 검색 타입(진입 경로 구분값) & 정렬 타입)
     * @return JSONObject
     */
    @Transactional(readOnly = true)
    public JSONObject getContentEpisodeList(SearchDto searchDto, HttpServletRequest request) {

        // 컨트롤러로 리턴할 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<EpisodeDto> contentEpisodeList = null;

        // 작품 회차 리스트 기본 유효성 검사
        episodeListValidate(searchDto);

        // 해당 작품이 성인 작품 또는 성인관 작품인지 체크
        ContentDto contentInfo = contentDaoSub.checkIsContentAdult(searchDto.getContentsIdx());

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            // 회원 idx set
            Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회원 성인 여부 set
            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT);
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 성인 여부 set

            if (memberAdult == 0) { // 미성년자

                jsonData.put("isMemberAdult", false);

                // 성인 작품일 경우
                if (contentInfo.getAdult() == 1 || contentInfo.getAdultPavilion() == 1) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);

                // 성인 작품을 열람할 경우
            } else if (contentInfo.getAdult() == 1 || contentInfo.getAdultPavilion() == 1) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 작품 카테고리 idx set
        searchDto.setCategoryIdx(contentInfo.getCategoryIdx());

        // 회차 리스트 개수 카운트
        int totalCount = episodeDaoSub.getContentEpisodesTotalCnt(searchDto);

        // 선택한 작품의 회차 리스트가 있는 경우
        if (totalCount > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 작품 회차 리스트
            contentEpisodeList = episodeDaoSub.getContentEpisodeList(searchDto);

            // 작품 회차 썸네일 fullUrl 세팅
            setEpisodeImgFullUrl(contentEpisodeList);

            // 작품 회차 리스트 표시 정보 세팅
            setContentEpisodeInfo(searchDto, contentEpisodeList, request);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // data 담기
        jsonData.put("contentEpisodeList", contentEpisodeList);
        return jsonData;
    }

    /**
     * 뷰어 내 회차 리스트
     *
     * @param searchDto (선택한 컨텐츠 idx & 호출 위치 & 검색 타입(진입 경로 구분값))
     * @return JSONObject
     */
    @Transactional(readOnly = true)
    public JSONObject getViewerEpisodeList(SearchDto searchDto, HttpServletRequest request) {

        // 컨트롤러로 리턴할 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<EpisodeDto> viewerEpisodeList = null;

        // 선택한 컨텐츠 조회 및 유효성 검사
        ContentDto contentInfo = checkContentInfo(searchDto.getContentsIdx());

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            // 회원 idx set
            Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
            searchDto.setMemberIdx(memberIdx); // 회원 idx set

            // 회원 성인 여부 set
            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT);
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 성인 여부 set

            if (memberAdult == 0) { // 미성년자

                jsonData.put("isMemberAdult", false);

                // 성인 작품일 경우
                if (contentInfo.getAdult() == 1 || contentInfo.getAdultPavilion() == 1) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }

            } else { // 성인
                jsonData.put("isMemberAdult", true);
            }
        } else { // 비로그인 상태

            // 성인 작품일 경우
            if (contentInfo.getAdult() == 1 || contentInfo.getAdultPavilion() == 1) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // 작품 카테고리 idx set
        searchDto.setCategoryIdx(contentInfo.getCategoryIdx());

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 뷰어 내 회차 리스트 개수 카운트
        int totalCount = episodeDaoSub.getContentEpisodesTotalCnt(searchDto);

        // 선택한 작품의 뷰어 내 회차 리스트가 있는 경우
        if (totalCount > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 뷰어 내 회차 리스트
            viewerEpisodeList = episodeDaoSub.getContentEpisodeList(searchDto);

            // 뷰어 내 회차 리스트 표시 정보 세팅
            setViewerEpisodeInfo(searchDto, viewerEpisodeList, request);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // data 담기
        jsonData.put("viewerEpisodeList", viewerEpisodeList);
        return jsonData;
    }

    /**************************************************************************************
     * 작품 / 작가 / 태그 검색
     *
     * 작품 노출 정책
     * (1) 비로그인 OR 비성인 회원 -> 비성인 작품만 노출
     * (2) 성인 회원 -> 비성인 + 성인 작품 전체 노출
     * (예외) 비로그인 상태이나, OTT에서 접속한 성인 회원 -> 비성인 + 성인 작품 전체 노출
     **************************************************************************************/

    /**
     * 작품 검색 결과 미리보기
     * 페이징 처리 X
     *
     * @param searchDto
     */
    public JSONObject getContentSearchPreview(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> contentSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 카테고리별 검색 결과 개수 set
        Map<String, Integer> categoryCnt = setCategoryResultCnt(searchDto);
        jsonData.put("categoryCnt", categoryCnt);

        // 작품 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getContentSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 검색 결과 개수 set
            jsonData.put("totalCount", totalCount);

            // 작품 검색 결과 미리보기 리스트 조회
            contentSearchList = contentDaoSub.getContentSearchList(searchDto);

            // 작품 검색 결과 노출할 카테고리 기본값 세팅
            String categoryTab = setCategoryTab(contentSearchList);
            jsonData.put("categoryTab", categoryTab);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(contentSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(contentSearchList);

            // 배지 코드 set
            setBadgeCode(contentSearchList, SEARCH_CONTENT);
        }
        // list 담기
        jsonData.put("contentSearchList", contentSearchList);
        return jsonData;
    }

    /**
     * 작품 검색 결과 전체보기
     * 페이징 처리 O
     *
     * @param searchDto
     */
    public JSONObject getContentSearchAll(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> contentSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 카테고리별 검색 결과 개수 set
        Map<String, Integer> categoryCnt = setCategoryResultCnt(searchDto);
        jsonData.put("categoryCnt", categoryCnt);

        // 작품 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getContentSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 작품 검색 결과 전체보기 리스트 조회
            contentSearchList = contentDaoSub.getContentSearchList(searchDto);

            // 작품 검색 결과 노출할 카테고리 기본값 세팅
            String categoryTab = setCategoryTab(contentSearchList);
            jsonData.put("categoryTab", categoryTab);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(contentSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(contentSearchList);

            // 배지 코드 set
            setBadgeCode(contentSearchList, SEARCH_CONTENT);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        jsonData.put("contentSearchList", contentSearchList);
        return jsonData;
    }

    /**
     * 작가 검색 결과 미리보기
     * 페이징 처리 X
     *
     * @param searchDto
     */
    public JSONObject getAuthorSearchPreview(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> authorSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 작가 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getAuthorSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 검색 결과 개수 set
            jsonData.put("totalCount", totalCount);

            // 작가 검색 결과 미리보기 리스트 조회
            authorSearchList = contentDaoSub.getAuthorSearchList(searchDto);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(authorSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(authorSearchList);

            // 배지 코드 set
            setBadgeCode(authorSearchList, SEARCH_AUTHOR);
        }
        // list 담기
        jsonData.put("authorSearchList", authorSearchList);
        return jsonData;
    }

    /**
     * 작가 검색 결과 전체보기
     * 페이징 처리 O
     *
     * @param searchDto
     */
    public JSONObject getAuthorSearchAll(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> authorSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 작가 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getAuthorSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 작가 검색 결과 전체보기 리스트 조회
            authorSearchList = contentDaoSub.getAuthorSearchList(searchDto);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(authorSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(authorSearchList);

            // 배지 코드 set
            setBadgeCode(authorSearchList, SEARCH_AUTHOR);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        jsonData.put("authorSearchList", authorSearchList);
        return jsonData;
    }

    /**
     * 태그 검색 결과 미리보기
     * 페이징 처리 X
     *
     * @param searchDto
     */
    public JSONObject getTagSearchPreview(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> tagSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 태그 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getTagSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 검색 결과 개수 set
            jsonData.put("totalCount", totalCount);

            // 태그 검색 결과 미리보기 리스트 조회
            tagSearchList = contentDaoSub.getTagSearchList(searchDto);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(tagSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(tagSearchList);

            // 배지 코드 set
            setBadgeCode(tagSearchList, SEARCH_TAG);

        }
        // list 담기
        jsonData.put("tagSearchList", tagSearchList);
        return jsonData;
    }

    /**
     * 태그 검색 결과 전체보기
     * 페이징 처리 O
     *
     * @param searchDto
     */
    public JSONObject getTagSearchAll(SearchDto searchDto, HttpServletRequest request) {

        // 검색 유효성 검사
        searchWordValidate(searchDto);

        // 현재 시간 set (pubDate 비교 시 사용)
        searchDto.setNowDate(dateLibrary.getDatetime());

        // 데이터 담을 객체 생성
        JSONObject jsonData = new JSONObject();
        List<ContentDto> tagSearchList = null;

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태

            String stringMemberAdult = super.getMemberInfo(SessionConfig.ADULT); // 회원 성인 여부 정보
            Integer memberAdult = Integer.valueOf(stringMemberAdult); // Integer 형변환
            searchDto.setAdult(memberAdult); // 회원 성인 여부 set

            if (memberAdult == 0) {
                jsonData.put("isMemberAdult", false);
            } else {
                jsonData.put("isMemberAdult", true);
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 작품 전체 노출
                searchDto.setAdult(1);
                searchDto.setPavilionIdx(1);
            }
        }

        // 태그 검색 결과 개수 카운트
        int totalCount = contentDaoSub.getTagSearchTotalCnt(searchDto);

        // 검색 결과가 있으면
        if (totalCount > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 태그 검색 결과 리스트 조회
            tagSearchList = contentDaoSub.getTagSearchList(searchDto);

            // 검색 결과 이미지 fullUrl 세팅
            setImgFullUrl(tagSearchList);

            // 작가 & 태그 set
            setAuthorAndTag(tagSearchList);

            // 배지 코드 set
            setBadgeCode(tagSearchList, SEARCH_TAG);

            // paging 담기
            jsonData.put("params", new JSONObject(searchDto));
        }
        // list 담기
        jsonData.put("tagSearchList", tagSearchList);
        return jsonData;
    }

    /**************************************************************************************
     * 내 서재 - 관심 작품
     **************************************************************************************/

    /**
     * 관심 작품 리스트 (내 서재)
     *
     * @param searchDto
     * @return
     */
    @Transactional(readOnly = true)
    public JSONObject getFavoriteContentsList(SearchDto searchDto) {

        // 회차 소장 or 대여 리스트 조회
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        // 구매내역 개수 조회
        int totalCnt = contentDaoSub.getFavoriteContentsTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<ContentDto> favoriteList = null;

        if (totalCnt > 0) {
            // 보유중인 에피소드 리스트(이미지 포함) - 내서재
            favoriteList = contentDaoSub.getFavoriteContentsList(searchDto);
            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set
            // 타입 지정 및 문자변환(내 서재 형식 맞추기 위해 사용)
            typeText(favoriteList);
            // 이미지 도메인 세팅
            setImgFullUrl(favoriteList);
            // 작가 & 태그 set
            setAuthorAndTag(favoriteList);
            // 배지 코드 set
            setBadgeCode(favoriteList, LIBRARY);
        }

        jsonData.put("list", favoriteList);

        return jsonData;
    }

    /**
     * 관심 작품 리스트 삭제
     *
     * @param searchDto : idxList, memberIdx
     */
    public void deleteMemberFavoriteList(SearchDto searchDto) {
        /** 유효성 검사 **/
        deleteMemberFavoriteValidate(searchDto);

        // 관심 작품 삭제
        int deleteResult = contentDao.deleteMemberFavoriteList(searchDto);

        if (deleteResult < 1) {
            throw new CustomException(CustomError.FAVORITE_DELETE_FAIL);  // 최근 본 작품 삭제에 실패하였습니다.
        }
    }


    /********************************************************************************
     * 작품 찜하기
     ********************************************************************************/

    /**
     * 작품 찜하기
     *
     * @param contentDto
     * @return
     */
    public void favoriteContent(ContentDto contentDto) {

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(contentDto.getContentsIdx());

        // 찜 기록 DB 조회
        ContentDto favoriteInfo = contentDaoSub.getContentFavorite(contentDto);

        // 이미 찜한 작품일 경우
        if (favoriteInfo != null) {
            throw new CustomException(CustomError.CONTENTS_FAVORITE_DUPLE_ERROR);  // 이미 찜한 작품입니다.
        }

        // 찜한 날짜 현재 시간으로 set
        contentDto.setRegdate(dateLibrary.getDatetime());

        // 작품 찜하기
        int result = contentDao.favoriteContent(contentDto);

        // 작품 찜하기 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_FAVORITE_ERROR);  // 관심 작품 목록에 추가할 수 없습니다.
        }

        // 현재 작품의 찜 개수 가져오기
        int favoriteCnt = contentDaoSub.getFavoriteCnt(contentDto);

        // 작품 찜 개수 +1 set
        contentDto.setFavorite(favoriteCnt + 1);

        // 작품 찜 개수 업데이트
        contentDao.updateFavoriteCnt(contentDto);
    }

    /**
     * 작품 찜하기 취소
     *
     * @param contentDto
     * @return
     */
    public void deleteFavoriteContent(ContentDto contentDto) {

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(contentDto.getContentsIdx());

        // 찜 기록 DB 조회
        ContentDto favoriteInfo = contentDaoSub.getContentFavorite(contentDto);

        // 찜하지 않은 작품일 경우
        if (favoriteInfo == null) {
            throw new CustomException(CustomError.CONTENTS_FAVORITE_NOT_EXIST);  // 찜하지 않은 작품입니다.
        }

        // 작품 찜하기 취소
        int result = contentDao.deleteFavoriteContent(contentDto);

        // 작품 찜하기 취소 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_FAVORITE_CANCEL_ERROR);  // 관심 작품 목록에서 삭제할 수 없습니다.
        }

        // 현재 작품의 찜 개수 가져오기
        int favoriteCnt = contentDaoSub.getFavoriteCnt(contentDto);

        if (favoriteCnt > 0) {

            // 작품 찜 개수 -1 set
            contentDto.setFavorite(favoriteCnt - 1);

            // 작품 찜 개수 업데이트
            contentDao.updateFavoriteCnt(contentDto);
        }
    }

    /********************************************************************************
     * 작품 신고하기
     ********************************************************************************/

    /**
     * 작품 신고하기
     *
     * @param contentDto
     * @return
     */
    public void reportContent(ContentDto contentDto) {

        // 신고할 컨텐츠 idx 유효성 체크
        contentIdxValidate(contentDto.getContentsIdx());

        // 신고 기록 DB 조회
        ContentDto contentReport = contentDaoSub.getContentReport(contentDto);

        // 이미 신고한 작품일 경우
        if (contentReport != null) {
            throw new CustomException(CustomError.CONTENTS_REPORT_DUPLE_ERROR);  // 이미 신고한 작품입니다.
        }

        // 신고 상태 set
        contentDto.setState(1);

        // 신고한 날짜 현재 시간으로 set
        contentDto.setRegdate(dateLibrary.getDatetime());

        // 작품 신고하기
        int result = contentDao.insertContentReport(contentDto);

        // 작품 신고 실패 시
        if (result < 1) {
            throw new CustomException(CustomError.CONTENTS_REPORT_ERROR);  // 작품을 신고할 수 없습니다.
        }
    }

    /********************************************************************************
     * 내 서재 - 컨텐츠 상세보기
     ********************************************************************************/

    /**
     * 회차 정보 관련 조회 (내 서재)
     * 팝업 3가지
     * 대여, 소장, 대여 & 소장
     *
     * @param searchDto
     */
    @Transactional(readOnly = true)
    public JSONObject getContentsInfoFromLibrary(SearchDto searchDto) {

        /** episodeIdx 유효성 검사 **/
        episodeIdxValidate(searchDto.getEpisodeIdx());

        /** 1. episodeIdx 해당하는 정보 조회 **/
        // contentIdx, sort(현재 순서), episodeNumber(현재 회차번호) 등
        EpisodeDto episodeDto = episodeDaoSub.getEpisodeInfo(searchDto.getEpisodeIdx());

        Integer contentsIdx = episodeDto.getContentIdx();
        searchDto.setContentsIdx(contentsIdx); // 컨텐츠 idx set

        /** 2. 해당 작품 정보 조회 **/
        // idx, title, lastEpisodeNumber, pubDate, category, genre (컨텐츠 정보)
        ContentDto contentDto = contentDaoSub.getContentsInfoFromLibrary(searchDto);
        contentDto.setEpisodeNumber(episodeDto.getSort());     // 현재 회차 번호 set

        // 회차 번호 텍스트 변환
        if (episodeDto.getCategoryIdx() == CATEGORY_COMIC) {
            contentDto.setEpisodeNumTitle(episodeDto.getEpisodeNumber() + "권");
        } else {
            contentDto.setEpisodeNumTitle(episodeDto.getEpisodeNumber() + "화");
        }

        contentDto.setEpisodeIdx(searchDto.getEpisodeIdx());   // 해당 에피소드 idx set

        PurchaseDto purchaseDto = PurchaseDto.builder()
                .memberIdx(searchDto.getMemberIdx())
                .contentsIdx(contentDto.getContentsIdx())
                .episodeIdx(searchDto.getEpisodeIdx())
                .nowDate(dateLibrary.getDatetime())
                .build();

        /** 3. 현재 회차 보유 여부 조회 **/
        int purchaseCnt = purchaseDaoSub.getPurchaseCnt(purchaseDto);

        // 보유하고 있음
        if (purchaseCnt > 0) {
            contentDto.setIsNowEpPurchase(true);     // 보유
            // 보유하고 있지 않음
        } else {
            contentDto.setIsNowEpPurchase(false);    // 미보유
        }

        /** 4. 현재 회차 무료 OR 이벤트 무료 여부 조회 **/

        // 무료 회차 & 이벤트 무료 회차 & 이벤트 상태 DB 조회
        PurchaseDto freeInfo = purchaseDaoSub.getEpisodeFreeInfo(contentsIdx);

        int freeCnt = freeInfo.getFreeEpisodeCnt(); // 기본 무료 sort
        int eventFreeCnt = 0;                       // 이벤트 무료 sort

        // 이벤트 진행 중
        if (freeInfo != null && freeInfo.getEventFreeUsed() == 1) {
            String startDate = freeInfo.getStartDate(); // 이벤트 시작일
            String endDate = freeInfo.getEndDate();     // 이벤트 종료일

            // 현재 날짜 기준 이벤트 진행중인 경우
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                if (dateLibrary.checkEventState(startDate, endDate)) {
                    // 이벤트 무료 회차 초기화
                    eventFreeCnt = freeInfo.getEventFreeEpisodeCnt();
                }
            } // end of if
        } // end of 이벤트 진행 중
        
        // 최종 무료 회차 sort
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 현재 회차 무료일 경우
        if (episodeDto.getSort() <= epFreeMaxSort) {
            contentDto.setIsNowEpFree(true);  // 현재 회차 무료 set

            // 현재 회차 무료 아닌 경우
        } else {
            contentDto.setIsNowEpFree(false); // 현재 회차 유료 set
        }

        /** 개별 회차 이벤트(0코인) 조회 **/
        EpisodeDto episodeEventDto = episodeDaoSub.getEpisodeEventByIdx(episodeDto);

        contentDto.setIsNowEpRentFree(false); // 기본 유료 set [현재 회차 대여 무료 여부]
        contentDto.setIsNowEpHaveFree(false); // 기본 유료 set [현재 회차 소장 무료 여부]

        if (episodeEventDto != null) {
            int haveCoin = episodeEventDto.getEventCoin();
            int rentCoin = episodeEventDto.getEventCoinRent();

            /** 소장 무료 **/
            if (haveCoin == 0) {
                contentDto.setIsNowEpHaveFree(true); // 무료로 초기화
            }

            /** 대여 무료 **/
            if (rentCoin == 0) {
                contentDto.setIsNowEpHaveFree(true); // 무료로 초기화
            }
        }

        /** 5. 다음 회차 유무 조회 (다음 회차 없을 경우 0 return) **/
        long nextEpisodeIdx = getNextEpisodeIdx(episodeDto); // 다음회차 idx

        // 다음 회차 존재
        if (nextEpisodeIdx > 0) {
            episodeDto.setNowDate(dateLibrary.getDatetime());
            episodeDto.setIdx(nextEpisodeIdx); /** 다음 회차 idx set **/
            // 다음 회차 무료, 유료 여부 조회(true: 무료, false: 유료)
            Map<String, Boolean> freeMap = getIsNextEpisodeFree(episodeDto);
            /** return value set **/
            System.out.println("freeMap = " + freeMap.toString());
            contentDto.setIsNextEpFree(freeMap.get("isNextEpFree"));         // 다음 회차 무료 여부
            contentDto.setIsNextEpRentFree(freeMap.get("isNextEpRentFree")); // 다음 회차 대여 무료 여부
            contentDto.setIsNextEpHaveFree(freeMap.get("isNextEpHaveFree")); // 다음 회차 소장 무료 여부

            contentDto.setIsNextEpisode(true);            // 다음 회차 존재
            contentDto.setNextEpisodeIdx(nextEpisodeIdx); // 다음 회차 idx

            /** 6. 다음 회차 대여 여부 조회 **/
            purchaseDto.setEpisodeIdx(nextEpisodeIdx); // 다음 회차 idx set
            purchaseDto.setType(EPISODE_RENT);         // 대여 SET
            // 대여 여부 조회
            int rentCnt = purchaseDaoSub.getPurchaseCnt(purchaseDto);

            if (rentCnt > 0) {
                contentDto.setIsNextEpRent(true);    // 대여중
            } else {
                contentDto.setIsNextEpRent(false);   // 미대여중
            }

            purchaseDto.setType(EPISODE_HAVE);           // 소장 SET
            // 소장 여부 조회
            int haveCnt = purchaseDaoSub.getPurchaseCnt(purchaseDto);

            if (haveCnt > 0) {
                contentDto.setIsNextEpHave(true);    // 소장중
            } else {
                contentDto.setIsNextEpHave(false);   // 미소장중
            }

        // 다음 회차 미존재
        } else if(nextEpisodeIdx == 0){
            /** 다음 회차 미존재 데이터 set **/
            contentDto.setIsNextEpisode(false);          // 다음 회차 미존재
            contentDto.setIsNextEpRent(false);           // 다음 회차 미대여
            contentDto.setNextEpisodeIdx(0L);            // 다음 회차 idx 없으면 0으로 set
            contentDto.setIsNextEpFree(false);
            contentDto.setIsNextEpRentFree(false);
            contentDto.setIsNextEpHaveFree(false);
        }
        // 앞단에 필요없는 정보 null set
        contentDto.setNextEpisodeNumber(null);
        contentDto.setMemberIdx(null);

        // 이미지 url set
        setContentImgFulUrl(contentDto.getContentHeightImgList());
        // 작가 & 태그 set
        setAuthorAndTag(contentDto);
        // 배지 set
        setBadgeCode(contentDto, LIBRARY);

        //return value
        JSONObject jsonData = new JSONObject();
        jsonData.put("content", new JSONObject(contentDto));

        return jsonData;
    }

    /********************************************************************************
     * SUB - 배지 코드 세팅
     ********************************************************************************/

    /**
     * 컨텐츠 배지 코드 세팅
     * 완결 배지 세팅된 경우 -> 신작 배지 세팅 X
     *
     * @param contentDto
     * @param type
     */
    private void setBadgeCode(ContentDto contentDto, String type) {

        // nowDate set
        String nowDate = dateLibrary.getDatetimeToSeoul();

        // 이벤트 무료 회차 idx 리스트 전체 조회
        List<Integer> freeIdxList = contentDaoSub.getEventFreeEpisodeInfo(nowDate);

        // 이벤트 할인 회차 개수 리스트 조회
        List<ContentDto> discountEpisodeCntList = contentDaoSub.getEventEpisodeCount(nowDate);
        List<Integer> discountIdxList = new ArrayList<>();
        if (!discountEpisodeCntList.isEmpty()) {
            for (int index = 0; index < discountEpisodeCntList.size(); index++) {
                discountIdxList.add(discountEpisodeCntList.get(index).getIdx());
            }
        }

        // 태그 이름 리스트
        List<String> tagNameList = new ArrayList<>();
        if (!contentDto.getTagList().isEmpty()) {
            for (int index = 0; index < contentDto.getTagList().size(); index++) {
                tagNameList.add(contentDto.getTagList().get(index).getName());
            }
        }

        /** 배지 코드 세팅 시작 **/
        // 배지 리스트 생성
        contentDto.setBadgeList(new ArrayList<>());
        List<BadgeDto> badgeList = contentDto.getBadgeList();

        /** 작품에 이벤트 무료 회차가 존재할 경우 free 배지 세팅 **/
        if (!freeIdxList.isEmpty()) {
            if (freeIdxList.contains(contentDto.getContentsIdx())) {
                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(CODE_FREE); // free 코드 set
                badgeList.add(badgeDto);
            }
        }

        /** 작품에 이벤트 할인 회차가 존재할 경우 discount 배지 세팅 **/
        if (!discountIdxList.isEmpty()) {
            if (discountIdxList.contains(contentDto.getContentsIdx())) {

                // 이벤트 할인 회차 idx 리스트의 인덱스 구하기
                int discountIdx = discountIdxList.indexOf(contentDto.getContentsIdx());

                // 이벤트 할인 회차가 1개라도 있는 경우
                if (discountEpisodeCntList.get(discountIdx).getDiscountEpisodeCnt() > 0) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_DISCOUNT); // discount 코드 set
                    badgeList.add(badgeDto);
                }
            }
        }

        /** 완결 작품일 경우 complete 배지 세팅 **/
        Integer progress = contentDto.getProgress(); // 작품 완결 여부
        contentDto.setProgress(null); // 앞단에서 안쓰므로 null 처리

        if (progress != null && progress == 3) {
            BadgeDto badgeDto = new BadgeDto();
            badgeDto.setCode(CODE_COMPLETE); // complete 코드 set
            badgeList.add(badgeDto);

        } else {

            /** 완결 배지 세팅 X 경우에만 -> 작품 발행 후 30일이 지나지 않았다면 new 배지 세팅 **/
            String contentsPubDate = contentDto.getContentsPubdate(); // 작품 발행일
            contentDto.setContentsPubdate(null); // 앞단에서 안쓰므로 null 처리

            if (contentsPubDate != null && !contentsPubDate.isEmpty()) {
                if (dateLibrary.isNotAfterDate(contentsPubDate, dateLibrary.ONE_MONTH)) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_NEW); // new 코드 set
                    badgeList.add(badgeDto);
                }
            }
        }

        /** 마지막 회차 업데이트 후 24시간이 지나지 않았다면 up 배지 세팅 **/
        String episodePubDate = contentDto.getEpisodePubdate(); // 회차 발행일
        contentDto.setEpisodePubdate(null); // 앞단에서 안쓰므로 null 처리

        if (episodePubDate != null && !episodePubDate.isEmpty()) {
            if (dateLibrary.isNotAfterDate(episodePubDate, dateLibrary.ONE_DAY)) {
                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(CODE_UP); // up 코드 set
                badgeList.add(badgeDto);
            }
        }

        /** 독점 작품일 경우 only 배지 세팅 **/
        Integer exclusive = contentDto.getExclusive(); // 작품 독점 여부
        contentDto.setExclusive(null); // 앞단에서 안쓰므로 null 처리

        if (exclusive != null && exclusive == 1) {
            BadgeDto badgeDto = new BadgeDto();
            badgeDto.setCode(CODE_ONLY); // only 코드 set
            badgeList.add(badgeDto);
        }

        /** 성인 작품일 경우 19 배지 세팅 **/
        Integer adult = contentDto.getAdult(); // 성인 작품 여부
        contentDto.setAdult(null); // 앞단에서 안쓰므로 null 처리

        if (adult != null && adult == 1) {
            BadgeDto badgeDto = new BadgeDto();
            badgeDto.setCode(CODE_ADULT_19); // adult_19 코드 sets
            badgeList.add(badgeDto);
        }

        /** 작품이 단행본일 경우 book 배지 세팅 **/
        Integer publication = contentDto.getPublication(); // 작품 단행본 여부
        contentDto.setPublication(null); // 앞단에서 안쓰므로 null 처리

        if (publication != null && publication == 1) {
            BadgeDto badgeDto = new BadgeDto();
            badgeDto.setCode(CODE_BOOK); // book 코드 set
            badgeList.add(badgeDto);
        }

        /** 작품이 개정판일 경우 revised 배지 세팅 **/
        Integer revision = contentDto.getRevision(); // 작품 개정판 여부
        contentDto.setRevision(null); // 앞단에서 안쓰므로 null 처리

        if (revision != null && revision == 1) {
            BadgeDto badgeDto = new BadgeDto();
            badgeDto.setCode(CODE_REVISED); // revised 코드 set
            badgeList.add(badgeDto);
        }

        /** 소설 원작 작품일 경우 original 배지 세팅 **/
        // 태그 이름 리스트
        if (!contentDto.getTagList().isEmpty()) {
            for (int index = 0; index < contentDto.getTagList().size(); index++) {
                if (contentDto.getTagList().get(index).getName().equals(CODE_ORIGINAL_TEXT)) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_ORIGINAL); // original 코드 set
                    badgeList.add(badgeDto);
                }
            }
        }

        /** 랭킹 1위 ~ 100위 사이의 작품일 경우 top 배지 세팅 **/
        List<Integer> rankIdxList = contentDaoSub.getRankContentsIdxList();
        if (!rankIdxList.isEmpty()) {
            // 랭킹 리스트에 포함된 작품일 경우
            if (rankIdxList.contains(contentDto.getContentsIdx()) && contentDto.getEpisodeCount()!= null && contentDto.getEpisodeCount() > 0) {
                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(CODE_TOP); // top 코드 set
                badgeList.add(badgeDto);
            }
        }

        /** 내 서재 페이지 한정 - webtoon, comic, novel, adult_pavilion 배지 세팅 **/
        if (type.equals(LIBRARY)) {

            String code = "";
            String category = contentDto.getCategory(); // 작품의 카테고리 이름

            if (category != null && !category.isEmpty()) {

                if (category.equals(WEBTOON_TEXT)) { // 카테고리가 웹툰일 때
                    code = CODE_WEBTOON; // webtoon 코드 set

                } else if (category.equals(COMIC_TEXT)) { // 카테고리가 만화일 때
                    code = CODE_COMIC; // comic 코드 set

                } else if (category.equals(NOVEL_TEXT)) { // 카테고리가 소설일 때
                    code = CODE_NOVEL; // novel 코드 set

                } else if (category.equals(ADULT_TEXT)) { // 카테고리가 성인일 때
                    code = CODE_ADULT_PAVILION; // adult_pavilion 코드 set
                }
                BadgeDto badgeDto = new BadgeDto();
                badgeDto.setCode(code); // 카테고리 배지 set
                badgeList.add(badgeDto);
            }
        }
    }

    /**
     * 컨텐츠 리스트 배지 코드 세팅
     * 완결 배지 세팅된 경우 -> 신작 배지 세팅 X
     *
     * @param contentDtoList
     * @param type
     */
    private void setBadgeCode(List<ContentDto> contentDtoList, String type) {

        // nowDate set
        String nowDate = dateLibrary.getDatetimeToSeoul();

        // 이벤트 무료 회차 idx 리스트 전체 조회
        List<Integer> freeIdxList = contentDaoSub.getEventFreeEpisodeInfo(nowDate);

        // 컨텐츠 idx 리스트 set
        List<Integer> idxList = new ArrayList<>();
        for (ContentDto contentDto : contentDtoList) {
            if (contentDto != null) {
                idxList.add(contentDto.getContentsIdx());
            }
        }

        // 이벤트 할인 회차 개수 리스트 조회
        List<ContentDto> discountEpisodeCntList = contentDaoSub.getEventEpisodeCount(nowDate);
        List<Integer> discountIdxList = new ArrayList<>();
        if (!discountEpisodeCntList.isEmpty()) {
            for (int index = 0; index < discountEpisodeCntList.size(); index++) {
                discountIdxList.add(discountEpisodeCntList.get(index).getIdx());
            }
        }

        // 랭킹 리스트에 포함된 작품 idx 리스트
        List<Integer> rankIdxList = contentDaoSub.getRankContentsIdxList();

        /** 배지 코드 세팅 시작 **/
        for (ContentDto contentDto : contentDtoList) {
            if (contentDto != null) {

                // 배지 리스트 생성
                contentDto.setBadgeList(new ArrayList<>());
                List<BadgeDto> badgeList = contentDto.getBadgeList();

                /** 작품에 이벤트 무료 회차가 존재할 경우 free 배지 세팅 **/
                if (!freeIdxList.isEmpty()) {
                    if (freeIdxList.contains(contentDto.getContentsIdx())) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_FREE); // free 코드 set
                        badgeList.add(badgeDto);
                    }
                }

                /** 작품에 이벤트 할인 회차가 존재할 경우 discount 배지 세팅 **/
                if (!discountIdxList.isEmpty()) {
                    if (discountIdxList.contains(contentDto.getContentsIdx())) {

                        // 이벤트 할인 회차 idx 리스트의 인덱스 구하기
                        int discountIdx = discountIdxList.indexOf(contentDto.getContentsIdx());

                        // 이벤트 할인 회차가 1개라도 있는 경우
                        if (discountEpisodeCntList.get(discountIdx).getDiscountEpisodeCnt() > 0) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_DISCOUNT); // discount 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 완결 작품일 경우 complete 배지 세팅 **/
                Integer progress = contentDto.getProgress(); // 작품 완결 여부
                contentDto.setProgress(null); // 앞단에서 안쓰므로 null 처리

                if (progress != null && progress == 3) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_COMPLETE); // complete 코드 set
                    badgeList.add(badgeDto);

                } else {

                    /** 완결 배지 세팅 X 경우에만 -> 작품 발행 후 30일이 지나지 않았다면 new 배지 세팅 **/
                    String contentsPubDate = contentDto.getContentsPubdate(); // 작품 발행일
                    contentDto.setContentsPubdate(null); // 앞단에서 안쓰므로 null 처리

                    if (contentsPubDate != null && !contentsPubDate.isEmpty()) {
                        if (dateLibrary.isNotAfterDate(contentsPubDate, dateLibrary.ONE_MONTH)) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_NEW); // new 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 마지막 회차 업데이트 후 24시간이 지나지 않았다면 up 배지 세팅 **/
                String episodePubDate = contentDto.getEpisodePubdate(); // 회차 발행일
                contentDto.setEpisodePubdate(null); // 앞단에서 안쓰므로 null 처리

                if (episodePubDate != null && !episodePubDate.isEmpty()) {
                    if (dateLibrary.isNotAfterDate(episodePubDate, dateLibrary.ONE_DAY)) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_UP); // up 코드 set
                        badgeList.add(badgeDto);
                    }
                }

                /** 독점 작품일 경우 only 배지 세팅 **/
                Integer exclusive = contentDto.getExclusive(); // 작품 독점 여부
                contentDto.setExclusive(null); // 앞단에서 안쓰므로 null 처리

                if (exclusive != null && exclusive == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_ONLY); // only 코드 set
                    badgeList.add(badgeDto);
                }

                /** 성인 작품일 경우 19 배지 세팅 **/
                Integer adult = contentDto.getAdult(); // 성인 작품 여부
                contentDto.setAdult(null); // 앞단에서 안쓰므로 null 처리

                if (adult != null && adult == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_ADULT_19); // adult_19 코드 set
                    badgeList.add(badgeDto);
                }

                /** 작품이 단행본일 경우 book 배지 세팅 **/
                Integer publication = contentDto.getPublication(); // 작품 단행본 여부
                contentDto.setPublication(null); // 앞단에서 안쓰므로 null 처리

                if (publication != null && publication == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_BOOK); // book 코드 set
                    badgeList.add(badgeDto);
                }

                /** 작품이 개정판일 경우 revised 배지 세팅 **/
                Integer revision = contentDto.getRevision(); // 작품 개정판 여부
                contentDto.setRevision(null); // 앞단에서 안쓰므로 null 처리

                if (revision != null && revision == 1) {
                    BadgeDto badgeDto = new BadgeDto();
                    badgeDto.setCode(CODE_REVISED); // revised 코드 set
                    badgeList.add(badgeDto);
                }

                /** 소설 원작 작품일 경우 original 배지 세팅 **/
                // 태그 이름 리스트
                if (!contentDto.getTagList().isEmpty()) {
                    for (int index = 0; index < contentDto.getTagList().size(); index++) {
                        if (contentDto.getTagList().get(index).getName().equals(CODE_ORIGINAL_TEXT)) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_ORIGINAL); // original 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                if (!type.equals(RANKING_LIST)) {
                    /** 랭킹 1위 ~ 100위 사이의 작품일 경우 top 배지 세팅 **/
                    if (!rankIdxList.isEmpty()) {
                        // 랭킹 리스트에 포함된 작품일 경우
                        if (rankIdxList.contains(contentDto.getContentsIdx())) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_TOP); // top 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }

                /** 내 서재 페이지 한정 - webtoon, comic, novel, adult_pavilion 배지 세팅 **/
                if (type.equals(LIBRARY)) {

                    String code = "";
                    String category = contentDto.getCategory(); // 작품의 카테고리 이름
                    Integer adultPavilion = contentDto.getAdultPavilion(); // 작품 성인관 여부

                    if (category != null && !category.isEmpty()) {

                        if (category.equals(WEBTOON_TEXT)) { // 카테고리가 웹툰일 때
                            code = CODE_WEBTOON; // webtoon 코드 set

                        } else if (category.equals(COMIC_TEXT)) { // 카테고리가 만화일 때
                            code = CODE_COMIC; // comic 코드 set

                        } else if (category.equals(NOVEL_TEXT)) { // 카테고리가 소설일 때
                            code = CODE_NOVEL; // novel 코드 set

                        } else if (adultPavilion == 1) { // 성인관 작품일 때
                            code = CODE_ADULT_PAVILION; // adult_pavilion 코드 set
                        }
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(code); // 카테고리 배지 set
                        badgeList.add(badgeDto);
                    }
                }
            }
        }
    }

    /********************************************************************************
     * SUB - 작품 검색 결과 노출할 카테고리 기본값 세팅
     ********************************************************************************/

    /**
     * 작품 검색 결과 노출할 카테고리 기본값 세팅
     *
     * @param contentSearchList : 카테고리 idx 순으로 정렬된 리스트
     */
    private String setCategoryTab(List<ContentDto> contentSearchList) {

        // 노출 기본 탭
        String categoryTab = null;

        for (ContentDto contentDto : contentSearchList) {
            if (contentDto != null) {

                // 작품 검색 결과에 웹툰이 있는 경우
                if (contentDto.getCategoryIdx() == 1) {
                    categoryTab = WEBTOON; // 노출 기본 탭을 웹툰으로 설정
                    break;
                }
                // 작품 검색 결과에 만화가 있는 경우
                if (contentDto.getCategoryIdx() == 2) {
                    categoryTab = COMIC; // 노출 기본 탭을 만화로 설정
                    break;
                }
                // 작품 검색 결과에 소설이 있는 경우
                if (contentDto.getCategoryIdx() == 3) {
                    categoryTab = NOVEL; // 노출 기본 탭을 소설로 설정
                    break;
                }
            }
        }
        return categoryTab;
    }

    /**
     * 작품 검색 결과 - 카테고리별 개수 세팅
     */
    private Map<String, Integer> setCategoryResultCnt(SearchDto searchDto) {

        // 기존 searchDto 카테고리 idx 조회
        int categoryIdx = searchDto.getCategoryIdx();

        // set value
        Map<String, Integer> data = new HashMap<>();

        // 웹툰
        searchDto.setCategoryIdx(1);
        int webtoonCnt = contentDaoSub.getContentSearchTotalCnt(searchDto);

        // 만화
        searchDto.setCategoryIdx(2);
        int comicCnt = contentDaoSub.getContentSearchTotalCnt(searchDto);

        // 소설
        searchDto.setCategoryIdx(3);
        int novelCnt = contentDaoSub.getContentSearchTotalCnt(searchDto);

        // map set
        data.put("webtoon", webtoonCnt);
        data.put("comic", comicCnt);
        data.put("novel", novelCnt);

        // searchDto 카테고리 idx 원복
        searchDto.setCategoryIdx(categoryIdx);

        // return value
        return data;
    }

    /********************************************************************************
     * SUB - 문자 변환
     ********************************************************************************/

    /**
     * 내 서재(관심 작품) 타입 문자변환
     *
     * @param contentDtoList
     */
    private void typeText(List<ContentDto> contentDtoList) {
        for (ContentDto contentDto : contentDtoList) {
            if (contentDto != null) {
                contentDto.setEpisodeIdx(0L);   // 다른 API와 형식 맞추기 위해 0 setting
                contentDto.setEpisodeNumber(0); // 다른 API와 형식 맞추기 위해 0 setting
                contentDto.setType(4);          // 관심(더미) - json 데이터 형식 맞추기 위한 용도
                contentDto.setTypeText(super.langMessage("lang.contents.favorite")); // 관심
                contentDto.setEpisodeNumTitle("0");
            }
        }
    }

    /********************************************************************************
     * SUB - 이미지 리사이징
     ********************************************************************************/

    /**
     * 컨텐츠 리스트 이미지 url setting
     *
     * @param contentList
     * @return
     */
    private void setImgFullUrl(List<ContentDto> contentList) {

        for (ContentDto contentDto : contentList) {

            // 컨텐츠 세로 이미지 리스트 url setting
            setContentImgFulUrl(contentDto.getContentHeightImgList());

            // 컨텐츠 가로 이미지 리스트 url setting
            setContentImgFulUrl(contentDto.getContentWidthImgList());
        }
    }

    /**
     * 컨텐츠 이미지 fulUrl 세팅
     * 이미지 리사이징 필요 - s3Library.getThumborFullUrl
     *
     * @param contentImgDtoList
     */
    private void setContentImgFulUrl(List<ContentImgDto> contentImgDtoList) {

        if (contentImgDtoList != null && !contentImgDtoList.isEmpty()) {

            for (ContentImgDto contentImgDto : contentImgDtoList) {

                if (contentImgDto.getUrl() != null) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("fileUrl", contentImgDto.getUrl());     // 컨텐츠 이미지 url
                    map.put("width", contentImgDto.getWidth());     // 컨텐츠 이미지 가로 사이즈
                    map.put("height", contentImgDto.getHeight());   // 컨텐츠 이미지 세로 사이즈

                    String fullUrl = s3Library.getThumborFullUrl(map);
                    contentImgDto.setUrl(fullUrl);
                }
            }
        }
    }

    /**
     * 회차 이미지 리스트 url 세팅
     *
     * @param episodeDtoList
     */
    private void setEpisodeImgFullUrl(List<EpisodeDto> episodeDtoList) {

        for (EpisodeDto episodeDto : episodeDtoList) {

            // dto set
            EpisodeImgDto dto = new EpisodeImgDto();
            dto.setUrl(episodeDto.getUrl());        // 회차 썸네일 url
            dto.setWidth(episodeDto.getWidth());    // 회차 썸네일 가로 길이
            dto.setHeight(episodeDto.getHeight());  // 회차 썸네일 세로 길이

            // 회차 썸네일 리스트 생성
            episodeDto.setEpisodeWidthImgList(new ArrayList<>());
            List<EpisodeImgDto> episodeWidthImgList = episodeDto.getEpisodeWidthImgList();

            // 회차 썸네일 리스트에 dto 담기
            episodeWidthImgList.add(dto);

            // 회차 썸네일 thumbor domain 세팅
            setEpisodeImgFulUrl(episodeWidthImgList);

            // 앞단에서 필요없는 데이터 null 처리
            episodeDto.setDevice(null);
            episodeDto.setUrl(null);
            episodeDto.setWidth(null);
            episodeDto.setHeight(null);
        }
    }

    /**
     * 회차 이미지 fulUrl 세팅
     * 이미지 리사이징 필요 - s3Library.getThumborFullUrl
     *
     * @param episodeWidthImgList
     */
    private void setEpisodeImgFulUrl(List<EpisodeImgDto> episodeWidthImgList) {

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

    /********************************************************************************
     * SUB - 작가 & 태그 정보 세팅
     ********************************************************************************/

    /**
     * 작품 작가 & 태그 정보 세팅
     *
     * @param contentList
     * @return
     */
    private void setAuthorAndTag(List<ContentDto> contentList) {

        for (ContentDto contentDto : contentList) {
            if (contentDto != null) {

                // 작품 작가 & 태그 정보 세팅
                setAuthorAndTag(contentDto);
            }
        }
    }

    /**
     * 작품 작가 & 태그 정보 세팅
     *
     * @param contentDto
     * @return
     */
    private void setAuthorAndTag(ContentDto contentDto) {

        // 세팅용 리스트 생성
        List<AuthorDto> writerList = new ArrayList<>();  // 글작가 리스트
        List<AuthorDto> painterList = new ArrayList<>(); // 그림작가 리스트
        List<TagDto> tagList = new ArrayList<>();        // 태그 리스트

        // 글작가 set
        if (contentDto.getWriterList() != null && !contentDto.getWriterList().isEmpty() && contentDto.getWriterList().size() > 0) {
            String[] writerStringList = contentDto.getWriterList().get(0).getName().split(",");

            for (String name : writerStringList) {
                AuthorDto writerDto = new AuthorDto();
                writerDto.setName(name);
                writerList.add(writerDto);
            }
            contentDto.setWriterList(writerList);
        }

        // 그림작가 set
        if (contentDto.getPainterList() != null && !contentDto.getPainterList().isEmpty() && contentDto.getPainterList().size() > 0) {
            String[] painterStringList = contentDto.getPainterList().get(0).getName().split(",");
            for (String name : painterStringList) {
                AuthorDto painterDto = new AuthorDto();
                painterDto.setName(name);
                painterList.add(painterDto);
            }
            contentDto.setPainterList(painterList);
        }

        // 태그 set
        if (contentDto.getTagList() != null && !contentDto.getTagList().isEmpty() && contentDto.getTagList().size() > 0) {
            String[] tagStringList = contentDto.getTagList().get(0).getName().split(",");
            for (String name : tagStringList) {
                TagDto tagDto = new TagDto();
                tagDto.setName(name);
                tagList.add(tagDto);
            }
            contentDto.setTagList(tagList);
        }
    }

    /********************************************************************************
     * SUB - 내 서재 다음 회차 조회
     ********************************************************************************/

    /**
     * 다음 회차 idx 리턴
     *
     * @param episodeDto : sort(현재 회차 순서), contentsIdx(컨텐츠 idx)
     * @return
     */
    private long getNextEpisodeIdx(EpisodeDto episodeDto) {

        int episodeSort = episodeDto.getSort();
        int nextEpSort = episodeSort + 1;

        // 다음 회차 번호
        EpisodeDto nextEpisodeDto = EpisodeDto.builder()
                .sort(nextEpSort)
                .contentIdx(episodeDto.getContentIdx())
                .build();

        // 다음 회차 idx 조회
        Long nextEpisodeIdx = episodeDaoSub.getIdxBySort(nextEpisodeDto);

        return nextEpisodeIdx;
    }

    /********************************************************************************
     * SUB - 작품 상세 내가 찜한 작품 여부 세팅
     ********************************************************************************/

    /**
     * 작품 상세에 표시할 정보 세팅(로그인)
     * 회원이 찜한 작품 여부
     *
     * @param searchDto
     * @param contentList
     */
    private void setLoginContentLikeInfo(SearchDto searchDto, List<ContentDto> contentList) {

        // 회원이 찜한 작품 idx 리스트 DB 조회
        List<Integer> memberLikeList = contentDaoSub.getMemberLikeContentList(searchDto);

        for (ContentDto dto : contentList) {

            if (!memberLikeList.isEmpty()) {

                // 회원이 찜한 작품일 경우
                if (memberLikeList.contains(dto.getContentsIdx())) {
                    dto.setIsMemberLike(true); // 찜한 작품으로 표시
                }
            }
        }
    }

    /********************************************************************************
     * SUB - 회차리스트 표시 정보 세팅
     ********************************************************************************/

    /**
     * 작품 회차 리스트에 표시할 정보 세팅
     * 회원이 최근 본 회차 배지 & 회원이 대여한 회차 & 회원이 소장한 회차
     * 무료 회차 & 이벤트 무료 회차 & 이벤트 할인 회차 & 이용권 사용 가능 회차
     *
     * @param searchDto : memberIdx(회원 idx), contentsIdx(작품 idx), searchType(회차리스트 탭 유형 - 대여/소장)
     * @param contentEpisodeList
     */
    @SneakyThrows
    private void setContentEpisodeInfo(SearchDto searchDto,
                                       List<EpisodeDto> contentEpisodeList,
                                       HttpServletRequest request) {

        // 회차리스트 null check
        if (!contentEpisodeList.isEmpty()) {

            // 해당 작품의 최종화 회차 번호 조회(완결작이 아니면 null)
            Integer lastEpisodeNumber = contentDaoSub.getLastEpisodeNumber(searchDto.getContentsIdx());

            // 해당 작품의 전체 회차 idx 리스트 조회(최신 회차 기준 내림차순)
            List<Long> epIdxList = episodeDaoSub.getEpisodeIdxList(searchDto);

            // 회원이 소장한 회차 idx 리스트
            List<Long> memberHaveList = null;      
            // 회원이 대여한 회차 리스트
            List<EpisodeDto> memberRentList = null;
            // 회원이 대여한 회차 idx 리스트
            List<Long> rentIdxList = new ArrayList<>();
            // 회원이 마지막으로 본 회차 번호
            Integer lastViewNumber = null;
            // 동일한 작품으로 지급 받은 작품 이용권 중 최신 보호 회차 개수의 최소값
            Integer minExceptCnt = null;
            // 현재 작품에서 사용 가능한 이용권 잔여 개수
            Integer restTicketCnt = null;

            // 로그인 상태 -> for문 돌리기 전에 필요한 DB 데이터 우선 조회
            if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0L) {

                // 회원이 소장한 회차 idx 리스트 DB 조회
                memberHaveList = episodeDaoSub.getMemberHaveList(searchDto);

                // 회원이 대여한 회차 리스트 DB 조회 + 회원이 대여한 회차 idx 리스트 생성
                memberRentList = episodeDaoSub.getMemberRentList(searchDto);
                if (!memberRentList.isEmpty()) {
                    for (int index = 0; index < memberRentList.size(); index++) {
                        rentIdxList.add(memberRentList.get(index).getIdx());
                    }
                }

                // 회원이 마지막으로 본 회차 번호 DB 조회
                EpisodeLastViewDto lastView = episodeDaoSub.getMemberLastViewNumber(searchDto);
                if (lastView != null) {
                    lastViewNumber = lastView.getEpisodeNumber();
                }

                // 동일한 작품으로 지급 받은 작품 이용권 중 제외할 최신 회차 개수의 최소값
                searchDto.setNowDate(dateLibrary.getDatetime()); // 오늘 00시 00분 00초
                minExceptCnt = giftDaoSub.getMemberMinExceptCnt(searchDto);

                // 현재 작품에서 사용 가능한 이용권 잔여 개수
                restTicketCnt = giftDaoSub.getMemberGiftCnt(searchDto);
            }

            // 무료 회차 & 이벤트 무료 회차 & 이벤트 상태 DB 조회
            EpisodeDto freeInfo = episodeDaoSub.episodeFreeAndEventInfo(searchDto);

            for (EpisodeDto dto : contentEpisodeList) {

                // 배지 리스트 set
                dto.setBadgeList(new ArrayList<>());

                /**  회차 번호 [권/화] 텍스트 변환 **/
                if (dto.getCategoryIdx() == CATEGORY_COMIC) {
                    dto.setEpisodeNumTitle(dto.getEpisodeNumber() + "권");
                } else {
                    dto.setEpisodeNumTitle(dto.getEpisodeNumber() + "화");
                }
                dto.setCategoryIdx(null); // 앞단에서 필요없으므로 null 처리

                /** 완결작일 경우, 마지막 회차에 최종화 텍스트 표시 & 완결 배지 세팅 **/
                if (lastEpisodeNumber != null && lastEpisodeNumber > 0) { // 완결작일 경우
                    if (dto.getEpisodeNumber().equals(lastEpisodeNumber)) { // 현재 순회 중인 dto가 최종화일 경우

                        // 완결 배지 set
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_COMPLETE); // complete 코드 set
                        dto.getBadgeList().add(badgeDto);
                        
                        // 최종화 텍스트 set
                        dto.setLastEpisodeText(LAST_EPISODE);
                    }
                }

                /** 실제 회차 가격이 무료인 경우 : 0코인 -> 무료로 표시 **/
                if (searchDto.getSearchType().equals(RENT)) { // 대여 카테고리에서
                    if (dto.getCoinRent() == 0) { // 대여 가격이 0코인일 경우
                        dto.setIsEpisodeFree(true); // 무료 회차 표시
                    }
                }
                if (searchDto.getSearchType().equals(HAVE)) { // 소장 카테고리에서
                    if (dto.getCoin() == 0) { // 소장 가격이 0코인일 경우
                        dto.setIsEpisodeFree(true); // 무료 회차 표시
                    }
                }

                /** 무료 회차 & 이벤트 무료 회차 & 이벤트 진행 상태 표시 **/
                if (freeInfo != null) {

                    // 무료로 제공하는 회차일 경우
                    if (dto.getSort() <= freeInfo.getFreeEpisodeCnt()) {
                        dto.setIsEpisodeFree(true); // 무료 회차 표시
                    }

                    // 이벤트 무료로 제공하는 회차일 경우
                    if (freeInfo.getEventFreeUsed() == 1) { // 이벤트가 진행중일 때

                        String startdate = freeInfo.getStartdate(); // 이벤트 시작일
                        String enddate = freeInfo.getEnddate();     // 이벤트 종료일

                        // 현재 날짜 기준 이벤트 진행중인 경우
                        if (startdate != null && !startdate.isEmpty() && enddate != null && !enddate.isEmpty()) {
                            if (dateLibrary.checkEventState(startdate, enddate)) {

                                int min = freeInfo.getFreeEpisodeCnt();
                                int max = freeInfo.getFreeEpisodeCnt() + (freeInfo.getEventFreeEpisodeCnt() - freeInfo.getFreeEpisodeCnt());

                                if (min < dto.getSort() && dto.getSort() <= max) {
                                    dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                }
                            }
                        }
                    }
                }

                /** 전작품 무료 감상 이벤트 회차 표시 (이벤트 기간일 때만 세팅) **/
                if (EVENT_STATE && dateLibrary.checkEventState(START_FREE_VIEW, END_FREE_VIEW)) {
                    
                    // 로그인 상태인 경우
                    if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0L) {

                        // 회원의 가입 경로 사이트 조회
                        String memberSite = memberDaoSub.getMemberSite(searchDto.getMemberIdx());

                        // OTT 유입을 통해 꿀툰 가입한 회원인 경우
                        if (!memberSite.equals("") && !memberSite.equals("ggultoon")) {
                            // 대여 탭의 회차리스트일 경우
                            if (searchDto.getSearchType().equals(RENT)) {
                                // 무료 회차가 아닌 회차에 한해
                                if (dto.getIsEpisodeFree() == null) {
                                    // 이벤트 무료 회차 표시
                                    dto.setIsEpisodeEventFree(true);
                                }
                            }
                        }
                        
                        // 비로그인 상태인 경우
                    } else {

                        // OTT 접속 토큰이 있는 경우
                        if (!super.getOttVisitToken(request).equals("")) {
                            // 대여 탭의 회차리스트일 경우
                            if (searchDto.getSearchType().equals(RENT)) {
                                // 무료 회차가 아닌 회차에 한해
                                if (dto.getIsEpisodeFree() == null) {
                                    // 이벤트 무료 회차 표시
                                    dto.setIsEpisodeEventFree(true);
                                }
                            }
                        }
                    }
                }

                /** 이벤트 할인 회차 & 이벤트 진행 상태 표시 **/
                if (dto.getIsEpisodeFree() == null && dto.getIsEpisodeEventFree() == null) {

                    String discountStartdate = dto.getStartdate(); // 할인 이벤트 시작일
                    String discountEnddate = dto.getEnddate();     // 할인 이벤트 종료일

                    // 현재 날짜 기준 이벤트 진행중인 경우
                    if (discountStartdate != null && !discountStartdate.isEmpty() && discountEnddate != null && !discountEnddate.isEmpty()) {
                        if (dateLibrary.checkEventState(discountStartdate, discountEnddate)) {

                            // 대여 탭의 회차리스트일 경우
                            if (searchDto.getSearchType().equals(RENT)) {
                                if (dto.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                                    dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                } else {
                                    dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                }
                                // 소장 탭의 회차리스트일 경우
                            } else if (searchDto.getSearchType().equals(HAVE)) {
                                if (dto.getEventCoin() == 0) { // 이벤트 할인 소장 가격이 0일 경우
                                    dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                } else {
                                    dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                }
                            }
                        }
                    }
                }
                // 앞단에서 필요없는 데이터 null 처리
                dto.setStartdate(null);
                dto.setEnddate(null);

                /** 로그인 상태일 때만 추가 세팅 - 회원이 최근 본 회차 & 회원이 대여한 회차 & 회원이 소장한 회차 **/
                if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0L) {

                    /** 회원이 대여한 회차 정보 표시 (대여 카테고리 선택 시에만) **/
                    if (searchDto.getSearchType().equals(RENT)) {

                        if (!rentIdxList.isEmpty()) {

                            // 회원이 대여한 회차일 경우
                            if (rentIdxList.contains(dto.getIdx())) {
                                dto.setIsMemberRent(true); // 대여 회차 표시

                                // 대여 후 잔여 사용 가능 기간 계산
                                int rentIdx = rentIdxList.indexOf(dto.getIdx()); // 대여한 회차 idx 리스트의 인덱스 구하기
                                String expireDate = memberRentList.get(rentIdx).getExpiredate(); // 대여한 회차의 만료일
                                String convertExpireDate = dateLibrary.getConvertRentExpiredate(expireDate);
                                dto.setConvertExpireDate(convertExpireDate); // 대여 회차 잔여 사용 기간 표시
                            }
                        }
                    }

                    /** 회원이 소장한 회차 정보 표시 (대여 후 소장했다면 대여 표시 제거) **/
                    if (!memberHaveList.isEmpty()) {

                        // 회원이 소장한 회차일 경우
                        if (memberHaveList.contains(dto.getIdx())) {
                            dto.setIsMemberHave(true); // 소장 회차 표시
                            dto.setIsMemberRent(null); // 대여 회차 표시 제거
                            dto.setConvertExpireDate(null); // 대여 회차 잔여 사용 기간 표시 제거
                        }
                    }

                    /** 회원이 최근 본 회차 정보 배지 세팅 **/
                    if (lastViewNumber != null) {

                        // 회원이 마지막으로 본 회차 번호일 경우
                        if (dto.getEpisodeNumber() == lastViewNumber) {

                            // 내가 보던 배지 set
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_VIEW); // view 코드 set
                            dto.getBadgeList().add(badgeDto);
                        }
                    }

                    /** 작품 무료 이용권 사용 가능 회차 표시 (대여 카테고리 선택 시에만) **/
                    if (searchDto.getSearchType().equals(RENT)) {
                        if (dto.getIsMemberRent() == null && dto.getIsMemberHave() == null) { // 대여 or 소장하지 않은 회차이면서
                            if (dto.getIsEpisodeFree() == null && dto.getIsEpisodeEventFree() == null) { // 무료 or 이벤트 무료 회차가 아닌 경우
                                if (minExceptCnt != null && minExceptCnt > 0 && minExceptCnt < epIdxList.size()) {

                                    // 이용권 사용이 가능한 회차 idx 최대값
                                    Long episodeIdx = epIdxList.get(minExceptCnt);
                                    if (dto.getIdx() <= episodeIdx) {

                                        // 이용권 사용 가능 배지 set
                                        BadgeDto badgeDto = new BadgeDto();
                                        badgeDto.setCode(CODE_FREE_TICKET); // free_ticket 코드 set
                                        dto.getBadgeList().add(badgeDto);

                                        // 무료 이용권 사용 가능 회차 set
                                        dto.setIsEpisodeTicketFree(true); // 무료 이용권 사용 가능 회차 표시

                                        // 사용 가능한 잔여 이용권 개수 set
                                        dto.setRestTicketCnt(restTicketCnt);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 뷰어 내 회차 리스트에 표시할 정보 세팅
     * 회원이 대여한 회차 & 회원이 소장한 회차
     * 무료 회차 & 이벤트 무료 회차 & 이벤트 할인 회차
     *
     * @param searchDto
     * @param viewerEpisodeList
     */
    @SneakyThrows
    private void setViewerEpisodeInfo(SearchDto searchDto, List<EpisodeDto> viewerEpisodeList, HttpServletRequest request) {

        if (!viewerEpisodeList.isEmpty()) {

            // 회차리스트의 idx 리스트 생성
            List<Long> epIdxList = new ArrayList<>();
            for (int index = 0; index < viewerEpisodeList.size(); index++) {
                epIdxList.add(viewerEpisodeList.get(index).getIdx());
            }

            // 회원이 소장한 회차 idx 리스트
            List<Long> memberHaveList = null;
            // 회원이 대여한 회차 리스트
            List<EpisodeDto> memberRentList = null;
            // 회원이 대여한 회차 idx 리스트
            List<Long> rentIdxList = new ArrayList<>();

            // 로그인 상태
            if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0L) {

                // 회원이 소장한 회차 idx 리스트 DB 조회
                memberHaveList = episodeDaoSub.getMemberHaveList(searchDto);

                // 회원이 대여한 회차 리스트 DB 조회 + 회원이 대여한 회차 idx 리스트 생성
                memberRentList = episodeDaoSub.getMemberRentList(searchDto);
                if (!memberRentList.isEmpty()) {
                    for (int index = 0; index < memberRentList.size(); index++) {
                        rentIdxList.add(memberRentList.get(index).getIdx());
                    }
                }
            }

            // 무료 회차 & 이벤트 무료 회차 & 이벤트 상태 DB 조회
            EpisodeDto freeInfo = episodeDaoSub.episodeFreeAndEventInfo(searchDto);

            for (EpisodeDto dto : viewerEpisodeList) {

                /**  1. 회차 번호 [권/화] 텍스트 변환 **/
                if (dto.getCategoryIdx() == CATEGORY_COMIC) { // 카테고리가 만화일 경우
                    dto.setEpisodeNumTitle(dto.getEpisodeNumber() + "권");
                } else { // 카테고리가 웹툰, 소설일 경우
                    dto.setEpisodeNumTitle(dto.getEpisodeNumber() + "화");
                }
                dto.setCategoryIdx(null); // 앞단에서 필요없으므로 null 처리

                /** 로그인 상태일 때만 추가 세팅 - 회원이 최근 본 회차 & 회원이 대여한 회차 & 회원이 소장한 회차 **/
                if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0) {

                    /** (1) 회원이 대여한 회차 정보 표시 **/
                    if (!rentIdxList.isEmpty()) {

                        // 회원이 대여한 회차일 경우
                        if (rentIdxList.contains(dto.getIdx())) {
                            dto.setIsMemberRent(true); // 대여 회차 표시
                        }
                    }

                    /** (2) 회원이 소장한 회차 정보 표시 **/
                    if (!memberHaveList.isEmpty()) {

                        // 회원이 소장한 회차일 경우
                        if (memberHaveList.contains(dto.getIdx())) {
                            dto.setIsMemberHave(true); // 소장 회차 표시
                        }
                    }
                }

                /** 2. 실제 회차 가격이 무료인 경우 : 0코인 -> 무료로 표시 **/
                if (searchDto.getSearchType() != null && !searchDto.getSearchType().isEmpty()) {
                    if (searchDto.getSearchType().equals(RENT)) { // 대여 카테고리에서
                        if (dto.getCoinRent() == 0) { // 대여 가격이 0코인일 경우
                            dto.setIsEpisodeFree(true); // 무료 회차 표시
                        }
                    }
                    if (searchDto.getSearchType().equals(HAVE)) { // 소장 카테고리에서
                        if (dto.getCoin() == 0) { // 소장 가격이 0코인일 경우
                            dto.setIsEpisodeFree(true); // 무료 회차 표시
                        }
                    }
                }

                /** 3. 무료 회차 & 이벤트 무료 회차 & 이벤트 진행 상태 표시 **/
                if (freeInfo != null) {

                    // 무료로 제공하는 회차일 경우
                    if (dto.getSort() <= freeInfo.getFreeEpisodeCnt()) {
                        dto.setIsEpisodeFree(true); // 무료 회차 표시
                    }

                    // 이벤트 무료로 제공하는 회차일 경우
                    if (freeInfo.getEventFreeUsed() == 1) { // 이벤트가 진행중일 때

                        String startdate = freeInfo.getStartdate(); // 이벤트 시작일
                        String enddate = freeInfo.getEnddate();     // 이벤트 종료일

                        // 현재 날짜 기준 이벤트 진행중인 경우
                        if (startdate != null && !startdate.isEmpty() && enddate != null && !enddate.isEmpty()) {
                            if (dateLibrary.checkEventState(startdate, enddate)) {

                                int min = freeInfo.getFreeEpisodeCnt();
                                int max = freeInfo.getFreeEpisodeCnt() + (freeInfo.getEventFreeEpisodeCnt() - freeInfo.getFreeEpisodeCnt());

                                if (min < dto.getSort() && dto.getSort() <= max) {
                                    dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                }
                            }
                        }
                    }
                }

                /** 전작품 무료 감상 이벤트 회차 표시 (이벤트 기간일 때만 세팅) **/
                if (EVENT_STATE && dateLibrary.checkEventState(START_FREE_VIEW, END_FREE_VIEW)) {

                    // 로그인 상태인 경우
                    if (searchDto.getMemberIdx() != null && searchDto.getMemberIdx() > 0L) {

                        // 회원의 가입 경로 사이트 조회
                        String memberSite = memberDaoSub.getMemberSite(searchDto.getMemberIdx());

                        // OTT 유입을 통해 꿀툰 가입한 회원인 경우
                        if (!memberSite.equals("") && !memberSite.equals("ggultoon")) {
                            // 대여 탭의 회차리스트일 경우
                            if (searchDto.getSearchType().equals(RENT)) {
                                // 무료 회차가 아닌 회차에 한해
                                if (dto.getIsEpisodeFree() == null) {
                                    // 이벤트 무료 회차 표시
                                    dto.setIsEpisodeEventFree(true);
                                }
                            }
                        }

                        // 비로그인 상태인 경우
                    } else {

                        // OTT 접속 토큰이 있는 경우
                        if (!super.getOttVisitToken(request).equals("")) {
                            // 대여 탭의 회차리스트일 경우
                            if (searchDto.getSearchType().equals(RENT)) {
                                // 무료 회차가 아닌 회차에 한해
                                if (dto.getIsEpisodeFree() == null) {
                                    // 이벤트 무료 회차 표시
                                    dto.setIsEpisodeEventFree(true);
                                }
                            }
                        }
                    }
                }

                /** 4. 이벤트 할인 회차 & 이벤트 진행 상태 표시 **/
                if (dto.getIsEpisodeFree() == null && dto.getIsEpisodeEventFree() == null) {

                    String discountStartdate = dto.getStartdate(); // 할인 이벤트 시작일
                    String discountEnddate = dto.getEnddate();     // 할인 이벤트 종료일

                    // 현재 날짜 기준 이벤트 진행중인 경우
                    if (discountStartdate != null && !discountStartdate.isEmpty() && discountEnddate != null && !discountEnddate.isEmpty()) {
                        if (dateLibrary.checkEventState(discountStartdate, discountEnddate)) {

                            // 진입 경로 구분값이 있을 경우
                            if (searchDto.getSearchType() != null && !searchDto.getSearchType().isEmpty()) {

                                // 진입 경로 : 대여일 경우
                                if (searchDto.getSearchType().equals(RENT)) {
                                    if (dto.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                                        dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                    } else {
                                        dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                    }

                                    // 진입 경로 : 소장일 경우
                                } else if (searchDto.getSearchType().equals(HAVE)) {
                                    if (dto.getEventCoin() == 0) { // 이벤트 할인 소장 가격이 0일 경우
                                        dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                    } else {
                                        dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                    }
                                }

                                // 진입 경로를 알 수 없는 경우 -> 해당 작품의 sell_type 값 조회
                            } else {

                                // 대여 & 소장으로 판매 시
                                if (dto.getSellType() == 1) {
                                    dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시

                                    // 소장으로만 판매 시
                                } else if (dto.getSellType() == 2) {
                                    if (dto.getEventCoin() == 0) { // 이벤트 할인 소장 가격이 0일 경우
                                        dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                    } else {
                                        dto.setRoute(HAVE); // 진입경로 set
                                        dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                    }

                                    // 대여로만 판매 시
                                } else if (dto.getSellType() == 3) {
                                    if (dto.getEventCoinRent() == 0) { // 이벤트 할인 대여 가격이 0일 경우
                                        dto.setIsEpisodeEventFree(true); // 이벤트 무료 회차 표시
                                    } else {
                                        dto.setRoute(RENT); // 진입경로 set
                                        dto.setIsEpisodeEventDiscount(true); // 이벤트 할인 회차 표시
                                    }
                                }
                            }
                        }
                    }
                }
                // 앞단에서 필요없는 데이터 null 처리
                dto.setStartdate(null);
                dto.setEnddate(null);
            }
        }
    }

    /**************************************************************************
     * Validation
     **************************************************************************/

    /**
     * 회차 idx 유효성 검사(공통)
     */
    private void episodeIdxValidate(Long episodeIdx) {
        // idx 기본 유효성 검사
        if (episodeIdx == null || episodeIdx < 1L) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }

        EpisodeDto episodeDto = EpisodeDto.builder()
                .nowDate(dateLibrary.getDatetime())// 현재 시간
                .idx(episodeIdx) // 에피소드 idx
                .build();

        // idx db 조회 후 검사
        int episodeCnt = episodeDaoSub.getEpisodeCnt(episodeDto);

        if (episodeCnt < 1) {
            throw new CustomException(CustomError.EPISODE_IDX_ERROR); // 유효하지 않은 회차입니다.
        }
    }

    /**
     * 작품 카테고리 리스트 유효성 검사
     * 선택한 이용관 idx & 카테고리 idx & 장르 idx & 정렬 타입 유효성 체크
     *
     * @param searchDto
     */
    private void selectInfoValidate(SearchDto searchDto) {

        // 선택한 이용관 idx 값이 유효하지 않은 경우
        if (searchDto.getPavilionIdx() == null || searchDto.getPavilionIdx() < 0 || searchDto.getPavilionIdx() > 1) {
            searchDto.setPavilionIdx(0); // 일반관으로 set(기본값)
        }

        // 선택한 카테고리 idx 값이 없는 경우
        if (searchDto.getCategoryIdx() == null || searchDto.getCategoryIdx() < 1) {
            throw new CustomException(CustomError.CATEGORY_IDX_EMPTY); // 요청하신 카테고리 정보를 찾을 수 없습니다.
        }

        // 유효한 카테고리 idx 값인지 DB 조회
        int categoryCnt = contentDaoSub.getCategoryCountByIdx(searchDto.getCategoryIdx());
        if (categoryCnt < 1) {
            throw new CustomException(CustomError.CATEGORY_NOT_EXIST); // 요청하신 카테고리 정보를 찾을 수 없습니다.
        }

        // 선택한 장르 idx 값이 있을 때
        if (searchDto.getGenreIdx() != null) {

            // 유효한 장르 idx 값인지 DB 조회
            int genreCnt = contentDaoSub.getGenreCountByIdx(searchDto.getGenreIdx());
            if (genreCnt < 1) {
                throw new CustomException(CustomError.GENRE_NOT_EXIST); // 요청하신 장르 정보를 찾을 수 없습니다.
            }
        }

        // 선택한 정렬 타입이 있을 때
        if (searchDto.getSortType() != null) {

            // 유효한 정렬 타입인지 체크 (1:랭킹순 / 2:최신순 / 3:인기순)
            if (searchDto.getSortType() < 1 || searchDto.getSortType() > 3) {
                throw new CustomException(CustomError.SORT_NOT_EXIST); // 요청하신 정렬 정보를 찾을 수 없습니다.
            }
        }
    }

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
        int contentCnt = contentDaoSub.getContentCountByIdx(searchDto);

        // 유효한 컨텐츠가 아닐 경우
        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
        }
    }


    /**
     * 큐레이션 기본 유효성 체크
     *
     * @param searchDto (큐레이션 idx)
     */
    private void curationValidate(SearchDto searchDto) {

        // 선택한 이용관 idx 값이 유효하지 않은 경우
        if (searchDto.getPavilionIdx() == null || searchDto.getPavilionIdx() < 0 || searchDto.getPavilionIdx() > 1) {
            searchDto.setPavilionIdx(0); // 일반관으로 set(기본값)
        }

        // 노출 영역 값이 없는 경우
        if (searchDto.getType() == null || searchDto.getType() < 1) {
            throw new CustomException(CustomError.CONTENTS_CURATION_EMPTY); // 큐레이션 노출 영역을 선택해주세요.
        }

        // 노출 영역 값이 유효하지 않은 경우
        if (searchDto.getType() > 10) {
            throw new CustomException(CustomError.CONTENTS_CURATION_NOT_EXIST); // 요청하신 노출 영역을 찾을 수 없습니다.
        }
    }

    /**
     * 회차 리스트 기본 유효성 검사
     *
     * @param searchDto - type(API 호출 위치 : 작품 하단 / 뷰어 내), searchType(진입 경로 구분값 : 대여 / 소장 / 알 수 없음), sortType(정렬 타입 : 회차순 / 최신순)
     */
    private void episodeListValidate(SearchDto searchDto) {

        // 컨텐츠 idx 유효성 체크
        contentIdxValidate(searchDto.getContentsIdx());

        // 검색 타입 유효성 체크
        if (searchDto.getSearchType() == null || searchDto.getSearchType().isEmpty()) {
            throw new CustomException(CustomError.SEARCH_TYPE_EMPTY); // 검색 유형을 선택해주세요.
        }

        if (!(searchDto.getSearchType().equals(RENT) || searchDto.getSearchType().equals(HAVE))) {
            throw new CustomException(CustomError.SEARCH_TYPE_ERROR); // 요청하신 검색 유형을 찾을 수 없습니다.
        }

        // 정렬 타입 유효성 체크
        if (searchDto.getSortType() != null) {

            // 유효한 정렬 타입인지 체크 (1:회차순 / 2:최신순)
            if (searchDto.getSortType() < 1 || searchDto.getSortType() > 2) {
                throw new CustomException(CustomError.SORT_NOT_EXIST); // 요청하신 정렬 정보를 찾을 수 없습니다.
            }
        }
    }

    /**
     * 관심 작품 리스트 삭제 유효성 검사
     *
     * @param searchDto
     */
    private void deleteMemberFavoriteValidate(SearchDto searchDto) {

        // 회원 idx 빈값
        if (searchDto.getMemberIdx() == null || searchDto.getMemberIdx() < 1L) {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR);          // 로그인 후 이용해주세요.
        }
        // idx 리스트가 빈값
        if (searchDto.getIdxList() == null || searchDto.getIdxList().isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_DELETE_IDX_EMPTY); // 삭제할 작품이 없습니다.
        }
        // 유효한 idx 인지 조회
        int idxCnt = searchDto.getIdxList().size();                           // 앞단에서 넘겨받은 idx 리스트 길이
        int viewContentCnt = contentDaoSub.getFavoriteIdxListCnt(searchDto);  // 조회한 idx 카운트

        // 넘겨 받은 idx 유효하지 않음
        if (idxCnt != viewContentCnt) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);       // 요청하신 작품정보를 찾을 수 없습니다.
        }

    }

    /**
     * 다음 회차 무료 여부 (isNextEpFree)
     * 다음 회차 소장 무료 여부 (isNextEpRentFree)
     * 다음 회차 대여 무료 여부 (isNextEpHaveFree)
     *
     * @param episodeDto
     * @return
     */
    private Map<String, Boolean> getIsNextEpisodeFree(EpisodeDto episodeDto) {

        Integer contentIdx = episodeDto.getContentIdx();

        // 다음 회차 번호
        int nextEpSort = episodeDto.getSort() + 1;

        Map<String, Boolean> map = new HashMap<>(); // 리턴할 value
        map.put("isNextEpFree", false);       // 기본 유료 set(기본 무료)
        map.put("isNextEpRentFree", false); // 기본 유료 set(대여 무료)
        map.put("isNextEpHaveFree", false); // 기본 유료 set(소장 무료)


        // 무료 회차 & 이벤트 무료 회차 & 이벤트 상태 DB 조회
        PurchaseDto freeInfo = purchaseDaoSub.getEpisodeFreeInfo(contentIdx);

        int freeCnt = freeInfo.getFreeEpisodeCnt(); // 기본 무료 sort
        int eventFreeCnt = 0;                       // 이벤트 무료 sort

        // 이벤트 진행 중
        if (freeInfo != null && freeInfo.getEventFreeUsed() == 1) {
            String startDate = freeInfo.getStartDate(); // 이벤트 시작일
            String endDate = freeInfo.getEndDate();     // 이벤트 종료일

            // 현재 날짜 기준 이벤트 진행중인 경우
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                if (dateLibrary.checkEventState(startDate, endDate)) {
                    // 이벤트 무료 회차 초기화
                    eventFreeCnt = freeInfo.getEventFreeEpisodeCnt();
                }
            } // end of if
        } // end of if

        // 최종 무료 회차 sort
        int epFreeMaxSort = (freeCnt > eventFreeCnt ? freeCnt : eventFreeCnt);

        // 현재 회차 무료일 경우
        if (nextEpSort <= epFreeMaxSort) {
            map.put("isNextEpFree", true);  // 현재 회차 무료 set
        }

        /** 개별 회차 이벤트(0코인) 조회 **/
        EpisodeDto episodeEventDto = episodeDaoSub.getEpisodeEventByIdx(episodeDto);

        // 개별 회차 이벤트 중
        if (episodeEventDto != null) {
            int haveCoin = episodeEventDto.getEventCoin();
            int rentCoin = episodeEventDto.getEventCoinRent();

            /** 소장 무료 **/
            if (haveCoin == 0) {
                map.put("isNextEpHaveFree", true);
            }
            /** 대여 무료 **/
            if (rentCoin == 0) {
                map.put("isNextEpRentFree", true);
            }
        }

        return map;
    }

    /**
     * 검색 기본 유효성 체크
     * 검색어 & 이용관 정보
     *
     * @param searchDto (검색어)
     */
    private void searchWordValidate(SearchDto searchDto) {

        // 검색어
        String searchWord = searchDto.getSearchWord();

        // 검색어가 없는 경우
        if (searchWord == null || searchWord.isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_SEARCH_WORD_EMPTY); // 검색어를 입력해주세요.
        }

        // 검색어 띄어쓰기 및 공백 제거
        searchDto.setSearchWord(searchDto.getSearchWord().replaceAll("\\s", ""));

        // 유효하지 않은 이용관 idx일 경우
        if (searchDto.getPavilionIdx() == null || searchDto.getPavilionIdx() < 0 || searchDto.getPavilionIdx() > 1) {
            searchDto.setPavilionIdx(0); // 기본값 set
        }
    }

    /**
     * 작품 정보 유효성 체크
     *
     * @param contentsIdx (컨텐츠 idx)
     */
    private ContentDto checkContentInfo(Integer contentsIdx) {

        // 선택한 컨텐츠 idx 값이 없는 경우
        if (contentsIdx == null || contentsIdx < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_EMPTY); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 유효한 컨텐츠인지 DB 조회하기 위해 set
        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(contentsIdx); // 컨텐츠 idx set
        searchDto.setNowDate(dateLibrary.getDatetime()); // 현재 시간 set (pubdate 비교용)

        // 유효한 컨텐츠인지 DB 조회
        ContentDto content = contentDaoSub.checkContentInfo(searchDto);

        // 유효한 컨텐츠가 아닐 경우
        if (content == null) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
        }
        return content;
    }

    /**************************************************************************
     * SUB
     **************************************************************************/

    /**
     * 큐레이션 노출 영역 세팅
     *
     * @param memberInfo (회원 세션 정보)
     * @param searchDto (큐레이션 idx)
     */
    private Integer setCurationAreaInfo(Object memberInfo, SearchDto searchDto) {

        // 현재 노출 영역
        Integer type = searchDto.getType();

        // 성인 회원인 경우
        if (memberInfo != null && searchDto.getAdult() != null && searchDto.getAdult() == 1 && searchDto.getPavilionIdx() == 1) {

            // 노출 영역 세팅
            switch (type) {

                // 메인 영역 호출 시
                case 1 : type = 2; // 메인 성인 영역
                    break;

                // 검색 영역 호출 시
                case 3 : type = 4; // 검색 성인 영역
                    break;

                // 회차 리스트 영역 호출 시
                case 4 : type = 6; // 회차 리스트 성인 영역
                    break;

                // 회차 뷰어 영역 호출 시
                case 6 : type = 8; // 회차 뷰어 성인 영역
                    break;
            }

            // 비로그인 OR 미성년자 회원 OR 성인 회원+일반관 토글 선택한 경우
        } else {

            // 노출 영역 세팅
            switch (type) {

                // 메인 영역 호출 시
                case 1 : type = 1; // 메인 일반 영역
                    break;

                // 검색 영역 호출 시
                case 3 : type = 3; // 검색 일반 영역
                    break;

                // 회차 리스트 영역 호출 시
                case 4 : type = 5; // 회차 리스트 일반 영역
                    break;

                // 회차 뷰어 영역 호출 시
                case 6 : type = 7; // 회차 뷰어 일반 영역
                    break;
            }
        }
        // 변경된 노출 영역 값으로 세팅
        searchDto.setType(type);

        // 노출 영역 값 리턴
        return type;
    }
}