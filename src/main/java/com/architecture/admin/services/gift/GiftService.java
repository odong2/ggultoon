package com.architecture.admin.services.gift;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.gift.GiftDao;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.gift.GiftDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.BadgeDto;
import com.architecture.admin.models.dto.content.ContentDto;
import com.architecture.admin.models.dto.content.ContentImgDto;
import com.architecture.admin.models.dto.gift.GiftDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.architecture.admin.libraries.utils.BadgeUtils.*;

@Service
@RequiredArgsConstructor
@Transactional
public class GiftService extends BaseService {
    private final S3Library s3Library;
    private final GiftDao giftDao;
    private final GiftDaoSub giftDaoSub;
    private final ContentDaoSub contentDaoSub;
    private final MemberDaoSub memberDaoSub;

    /**
     * 선물함
     * 회원이 지급 받은 선물 리스트 조회 -> 미지급된 선물 확인 후 지급
     *
     * [관련 정책]
     * 로그인 시점에 지급
     * 오늘 받을 수 있는 선물 리스트 + 내일 받을 수 있는 선물 리스트 -> 구분해서 반환
     * 오늘 받을 수 있는 선물과 내일 받을 수 있는 선물이 중복될 경우 내일 받을 수 있는 선물 리스트에서 제거
     * 이미 지급 받은 작품 이용권(동일한 idx)은 지급 제외(1회 제한)
     *
     * @param searchDto : memberIdx(회원 idx), adult(회원 성인 여부), contentsIdx(작품 idx), device(기기 정보)
     * @return
     */
    @Transactional
    public JSONObject getMemberGiftList(SearchDto searchDto) {

        // return value
        JSONObject jsonData = new JSONObject();
        JSONObject todayGift = new JSONObject();
        JSONObject tomorrowGift = new JSONObject();
        List<GiftDto> todayGiftList = null;
        List<GiftDto> tomorrowGiftList = null;

        // 작품 발행일 체크 -> 현재 시간 set
        searchDto.setNowDate(dateLibrary.getDatetime());
        
        // 총 지급된 개수 체크
        searchDto.setCount(0); // 리스트 개수

        // 작품 idx 유효성 체크
        contentIdxValidate(searchDto);

        /**************** 회원이 오늘 받을 수 있는 선물 리스트 ***************/
        searchDto.setSearchDateType("today");
        searchDto.setStartDate(dateLibrary.setDateTime(searchDto, "start")); // 오늘 00시 00분 00초
        searchDto.setEndDate(dateLibrary.setDateTime(searchDto, "end")); // 오늘 23시 59분 59초

        // 회원이 오늘 받을 수 있는 선물 개수 카운트
        int todayCnt = giftDaoSub.getAvailableGiftTotalCnt(searchDto);

        // 회원이 오늘 받을 수 있는 선물이 있는 경우
        if (todayCnt > 0) {

            // paging
            PaginationLibray pagination = new PaginationLibray(todayCnt, searchDto);
            searchDto.setPagination(pagination);

            // 회원이 오늘 받을 수 있는 선물 리스트 조회
            todayGiftList = giftDaoSub.getAvailableGiftList(searchDto);

            /** 꿀툰 서비스 종료 -> 선물 지급 중지(주석 처리) **/
            // 선물 지급 -> 이미 받은 선물 제외
            //insertMemberGift(searchDto, todayGiftList);

            // 이미지 fulUrl 세팅
            setImgFullUrl(todayGiftList);

            // 배지 세팅
            setBadgeCode(todayGiftList);

            // 사용 가능 기간 종료일 텍스트 변환
            convertDateToText(searchDto, todayGiftList);

            // paging set
            todayGift.put("params", new JSONObject(searchDto));
        }
        // list set
        todayGift.put("list", todayGiftList);

        /**************** 회원이 내일 받을 수 있는 선물 리스트 ***************/
        searchDto.setSearchDateType("tomorrow");
        searchDto.setStartDate(dateLibrary.setDateTime(searchDto, "start")); // 내일 00시 00분 00초
        searchDto.setEndDate(dateLibrary.setDateTime(searchDto, "end")); // 내일 23시 59분 59초

        // 회원이 내일 받을 수 있는 선물 개수 카운트
        int tomorrowCnt = giftDaoSub.getAvailableGiftTotalCnt(searchDto);

        // 회원이 내일 받을 수 있는 선물이 있는 경우
        if (tomorrowCnt > 0) {

            // 리스트 조회용 paging
            PaginationLibray pagination = new PaginationLibray(tomorrowCnt, searchDto);
            searchDto.setPagination(pagination);

            // 회원이 내일 받을 수 있는 선물 리스트
            tomorrowGiftList = giftDaoSub.getAvailableGiftList(searchDto);

            // 중복 제거 -> 오늘 받을 수 있는 선물과 중복 시 제거
            removeDuplicateTicket(todayGiftList, tomorrowGiftList);

            // 중복 제거로 변경된 개수 반영 -> paging 재설정
            tomorrowCnt = tomorrowGiftList.size();
            pagination = new PaginationLibray(tomorrowCnt, searchDto);
            searchDto.setPagination(pagination);

            // 이미지 fulUrl 세팅
            setImgFullUrl(tomorrowGiftList);

            // 배지 세팅
            setBadgeCode(todayGiftList);

            // 사용 가능 기간 시작일 텍스트 변환
            convertDateToText(searchDto, tomorrowGiftList);

            // paging set
            tomorrowGift.put("params", new JSONObject(searchDto));
        }
        // list set
        tomorrowGift.put("list", tomorrowGiftList);

        // 회원별 오늘 & 내일 받을 수 있는 작품 이용권 리스트 set
        jsonData.put("totalGiftCount", todayCnt + tomorrowCnt);
        jsonData.put("giftIconCount", searchDto.getCount());
        jsonData.put("todayGiftList", todayGift);
        jsonData.put("tomorrowGiftList", tomorrowGift);
        return jsonData;
    }

    /**
     * 회원 선물 지급
     * 오늘 받을 수 있는 선물만 지급
     * 이미 받은 선물 제외(회원의 전체 계정 확인)
     * 실제 지급 개수 통계 등록
     *
     * @param searchDto
     * @param memberGiftList (회원이 받을 수 있는 선물 리스트)
     * @return
     */
    @Transactional
    public void insertMemberGift(SearchDto searchDto, List<GiftDto> memberGiftList) {

        if (memberGiftList != null && !memberGiftList.isEmpty()) {

            // insert용 리스트 생성
            List<GiftDto> insertGiftList = new ArrayList<>();
            for (GiftDto dto : memberGiftList) {
                insertGiftList.add(dto);
            }

            // 회원 ci 정보 조회
            String memberCi = memberDaoSub.getMemberCi(searchDto);

            // 중복 체크용 dto set
            GiftDto gift = GiftDto.builder()
                    .memberCi(memberCi) // 회원 CI 정보로 체크 -> 다계정 중복 지급 방지
                    .nowDate(searchDto.getNowDate())
                    .searchDateType(searchDto.getSearchDateType())
                    .startDate(searchDto.getStartDate())
                    .endDate(searchDto.getEndDate())
                    .build();

            /** 이미 받은 선물 중복 제거 **/
            List<Long> dupleGiftIdxList = giftDaoSub.getMemberGiftIdxList(gift);
            if (dupleGiftIdxList != null && !dupleGiftIdxList.isEmpty()) {
                for (Long idx : dupleGiftIdxList) {
                    insertGiftList.removeIf(item -> item.getIdx().equals(idx));
                }
            }

            // 중복 제거 후에도 지급 받을 선물이 남아있다면
            if (insertGiftList != null && !insertGiftList.isEmpty()) {

                /** 회원이 받을 수 있는 선물 지급 **/
                // dto set
                for (GiftDto dto : insertGiftList) {
                    dto.setMemberIdx(searchDto.getMemberIdx()); // 회원 idx
                    dto.setPaymentIdx(0L); // 결제 idx
                    dto.setRegdate(dateLibrary.getDatetime()); // 지급일

                    // 통계 반영
                    giftDao.insertGiftGiveCnt(dto);
                }
                // DB insert
                giftDao.insertMemberGiftSave(insertGiftList);    // save 테이블
                giftDao.insertMemberGiftSaveLog(insertGiftList); // save_log 테이블
                giftDao.insertMemberGiftUsed(insertGiftList);    // used 테이블
            }

            /** 다른 계정으로 이미 받은 선물 중복 제거 **/
            gift.setMemberIdx(searchDto.getMemberIdx());
            List<Long> dupleCiGiftIdxList = giftDaoSub.getMemberGiftIdxList(gift);
            if (dupleCiGiftIdxList != null && !dupleCiGiftIdxList.isEmpty()) {
                for (Long idx : dupleCiGiftIdxList) {
                    memberGiftList.removeIf(item -> item.getIdx().equals(idx));
                }
            }
        }
    }

    /********************************************************************************
     * SUB
     ********************************************************************************/

    /**
     * 컨텐츠 리스트 이미지 url setting
     *
     * @param memberGiftList
     * @return
     */
    private void setImgFullUrl(List<GiftDto> memberGiftList) {

        if (memberGiftList != null && !memberGiftList.isEmpty()) {
            for (GiftDto giftDto : memberGiftList) {

                // 컨텐츠 세로 이미지 리스트 url setting
                setContentImgFulUrl(giftDto.getContentHeightImgList());

                // 컨텐츠 가로 이미지 리스트 url setting
                setContentImgFulUrl(giftDto.getContentWidthImgList());
            }
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

                    // 이미 Thumbor FullUrl 세팅된 dto 제외 -> 동일 이미지 누적 세팅 방지
                    if (!contentImgDto.getUrl().contains("https://")) {

                        Map<String, Object> map = new HashMap<>();
                        map.put("fileUrl", contentImgDto.getUrl());   // 컨텐츠 이미지 url
                        map.put("width", contentImgDto.getWidth());   // 컨텐츠 이미지 가로 사이즈
                        map.put("height", contentImgDto.getHeight()); // 컨텐츠 이미지 세로 사이즈

                        String fullUrl = s3Library.getThumborFullUrl(map);
                        contentImgDto.setUrl(fullUrl);
                    }
                }
            }
        }
    }

    /**
     * 컨텐츠 배지 코드 세팅
     * 완결 배지 세팅된 경우 -> 신작 배지 세팅 X
     *
     * @param giftList
     */
    private void setBadgeCode(List<GiftDto> giftList) {

        if (giftList != null && !giftList.isEmpty()) {

            // nowDate set
            String nowDate = dateLibrary.getDatetimeToSeoul();

            // 이벤트 무료 회차 idx 리스트 전체 조회
            List<Integer> freeIdxList = contentDaoSub.getEventFreeEpisodeInfo(nowDate);

            // 컨텐츠 idx 리스트 set
            List<Integer> idxList = new ArrayList<>();
            for (GiftDto giftDto : giftList) {
                if (giftDto != null) {
                    idxList.add(giftDto.getContentsIdx());
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
            for (GiftDto giftDto : giftList) {
                if (giftDto != null) {

                    // 배지 리스트 생성
                    giftDto.setBadgeList(new ArrayList<>());
                    List<BadgeDto> badgeList = giftDto.getBadgeList();

                    /** 작품에 이벤트 무료 회차가 존재할 경우 free 배지 세팅 **/
                    if (!freeIdxList.isEmpty()) {
                        if (freeIdxList.contains(giftDto.getContentsIdx())) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_FREE); // free 코드 set
                            badgeList.add(badgeDto);
                        }
                    }

                    /** 작품에 이벤트 할인 회차가 존재할 경우 discount 배지 세팅 **/
                    if (!discountIdxList.isEmpty()) {
                        if (discountIdxList.contains(giftDto.getContentsIdx())) {

                            // 이벤트 할인 회차 idx 리스트의 인덱스 구하기
                            int discountIdx = discountIdxList.indexOf(giftDto.getContentsIdx());

                            // 이벤트 할인 회차가 1개라도 있는 경우
                            if (discountEpisodeCntList.get(discountIdx).getDiscountEpisodeCnt() > 0) {
                                BadgeDto badgeDto = new BadgeDto();
                                badgeDto.setCode(CODE_DISCOUNT); // discount 코드 set
                                badgeList.add(badgeDto);
                            }
                        }
                    }

                    /** 완결 작품일 경우 complete 배지 세팅 **/
                    Integer progress = giftDto.getProgress(); // 작품 완결 여부
                    giftDto.setProgress(null); // 앞단에서 안쓰므로 null 처리

                    if (progress != null && progress == 3) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_COMPLETE); // complete 코드 set
                        badgeList.add(badgeDto);

                    } else {

                        /** 완결 배지 세팅 X 경우에만 -> 작품 발행 후 30일이 지나지 않았다면 new 배지 세팅 **/
                        String contentsPubDate = giftDto.getContentsPubdate(); // 작품 발행일
                        giftDto.setContentsPubdate(null); // 앞단에서 안쓰므로 null 처리

                        if (contentsPubDate != null && !contentsPubDate.isEmpty()) {
                            if (dateLibrary.isNotAfterDate(contentsPubDate, dateLibrary.ONE_MONTH)) {
                                BadgeDto badgeDto = new BadgeDto();
                                badgeDto.setCode(CODE_NEW); // new 코드 set
                                badgeList.add(badgeDto);
                            }
                        }
                    }

                    /** 마지막 회차 업데이트 후 24시간이 지나지 않았다면 up 배지 세팅 **/
                    String episodePubDate = giftDto.getEpisodePubdate(); // 회차 발행일
                    giftDto.setEpisodePubdate(null); // 앞단에서 안쓰므로 null 처리

                    if (episodePubDate != null && !episodePubDate.isEmpty()) {
                        if (dateLibrary.isNotAfterDate(episodePubDate, dateLibrary.ONE_DAY)) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_UP); // up 코드 set
                            badgeList.add(badgeDto);
                        }
                    }

                    /** 독점 작품일 경우 only 배지 세팅 **/
                    Integer exclusive = giftDto.getExclusive(); // 작품 독점 여부
                    giftDto.setExclusive(null); // 앞단에서 안쓰므로 null 처리

                    if (exclusive != null && exclusive == 1) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_ONLY); // only 코드 set
                        badgeList.add(badgeDto);
                    }

                    /** 성인 작품일 경우 19 배지 세팅 **/
                    Integer adult = giftDto.getContentsAdult(); // 성인 작품 여부
                    giftDto.setContentsAdult(null); // 앞단에서 안쓰므로 null 처리

                    if (adult != null && adult == 1) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_ADULT_19); // adult_19 코드 set
                        badgeList.add(badgeDto);
                    }

                    /** 작품이 단행본일 경우 book 배지 세팅 **/
                    Integer publication = giftDto.getPublication(); // 작품 단행본 여부
                    giftDto.setPublication(null); // 앞단에서 안쓰므로 null 처리

                    if (publication != null && publication == 1) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_BOOK); // book 코드 set
                        badgeList.add(badgeDto);
                    }

                    /** 작품이 개정판일 경우 revised 배지 세팅 **/
                    Integer revision = giftDto.getRevision(); // 작품 개정판 여부
                    giftDto.setRevision(null); // 앞단에서 안쓰므로 null 처리

                    if (revision != null && revision == 1) {
                        BadgeDto badgeDto = new BadgeDto();
                        badgeDto.setCode(CODE_REVISED); // revised 코드 set
                        badgeList.add(badgeDto);
                    }

                    /** 소설 원작 작품일 경우 original 배지 세팅 **/
                    // 태그 이름 리스트
                    if (!giftDto.getTagList().isEmpty()) {
                        for (int index = 0; index < giftDto.getTagList().size(); index++) {
                            if (giftDto.getTagList().get(index).getName().equals(CODE_ORIGINAL_TEXT)) {
                                BadgeDto badgeDto = new BadgeDto();
                                badgeDto.setCode(CODE_ORIGINAL); // original 코드 set
                                badgeList.add(badgeDto);
                            }
                        }
                    }

                    /** 랭킹 1위 ~ 100위 사이의 작품일 경우 top 배지 세팅 **/
                    if (!rankIdxList.isEmpty()) {
                        // 랭킹 리스트에 포함된 작품일 경우
                        if (rankIdxList.contains(giftDto.getContentsIdx())) {
                            BadgeDto badgeDto = new BadgeDto();
                            badgeDto.setCode(CODE_TOP); // top 코드 set
                            badgeList.add(badgeDto);
                        }
                    }
                }
            }
        }
    }

    /**
     * 내일 받을 수 있는 선물 리스트 가공
     * 오늘 받을 수 있는 선물 리스트와 중복되는 이용권이 있을 경우 제거
     *
     * @param todayGiftList (오늘 받을 수 있는 선물 리스트)
     * @param tomorrowGiftList (내일 받을 수 있는 선물 리스트)
     * @return
     */
    private void removeDuplicateTicket(List<GiftDto> todayGiftList, List<GiftDto> tomorrowGiftList) {

        if (todayGiftList != null && !todayGiftList.isEmpty() && tomorrowGiftList != null && !tomorrowGiftList.isEmpty()) {
            for (GiftDto dto : todayGiftList) {
                tomorrowGiftList.removeIf(item -> item.getIdx().equals(dto.getIdx()));
            }
        }
    }

    /**
     * 사용 가능 기간 텍스트 변환 및 선물함 헤더 아이콘 개수 계산
     * 오늘 받을 수 있는 선물 -> 사용 종료까지 남은 시간 표시(종료일 기준) ex. 1일 3시간 남음
     * 내일 받을 수 있는 선물 -> 지급 시작까지 남은 시간 표시(시작일 기준) ex. 9시간 후 선물
     *
     * @param searchDto
     * @param memberGiftList
     * @return
     */
    private void convertDateToText(SearchDto searchDto, List<GiftDto> memberGiftList) {

        // convert value
        String endDate;
        int giftCnt = 0;

        // 받았지만 기간이 만료된 선물 제외
        memberGiftList.removeIf(item -> dateLibrary.checkIsPassed(item.getEndDate()));

        for (GiftDto dto : memberGiftList) {

            // 지금 받을 수 있는 선물인 경우(시작일 ~ 종료일 사이)
            if (dateLibrary.checkEventState(dto.getStartDate(), dto.getEndDate())) {

                // 사용 완료 선물인 경우 예외
                if (dto.getRestCnt() != null && dto.getRestCnt() < 1) {
                    dto.setConvertDateText("사용 완료");
                    dto.setAvailable(false);

                    // 사용 가능 선물인 경우
                } else {
                    searchDto.setSearchDateType("today");
                    endDate = dateLibrary.convertGiftDate(searchDto, dto.getEndDate());
                    dto.setConvertDateText(endDate);
                    dto.setAvailable(true);
                    giftCnt++;
                }

                // 지금은 아니지만 오늘 받을 수 있는 선물 + 내일 받을 수 있는 선물인 경우
            } else {
                searchDto.setSearchDateType("tomorrow");
                endDate = dateLibrary.convertGiftDate(searchDto, dto.getStartDate());
                dto.setConvertDateText(endDate);
                dto.setAvailable(false);
                giftCnt++;
            }
        }
        // 선물함 헤더 카운트 세팅용 반환값 세팅
        searchDto.setCount(searchDto.getCount() + giftCnt);
    }

    /**************************************************************************
     * Validation
     **************************************************************************/

    /**
     * 작품 idx 기본 유효성 체크
     *
     * @param searchDto : contentsIdx(선택한 컨텐츠 idx), nowDate(현재 시간)
     */
    public void contentIdxValidate(SearchDto searchDto) {

        if (searchDto.getContentsIdx() != null && searchDto.getContentsIdx() > 0) {

            // 유효한 컨텐츠 idx 값인지 DB 조회
            int contentCnt = contentDaoSub.getContentCountByIdx(searchDto);

            // 유효한 컨텐츠가 아닐 경우
            if (contentCnt < 1) {
                throw new CustomException(CustomError.CONTENTS_NOT_EXIST);  // 요청하신 작품 정보를 찾을 수 없습니다.
            }
        }
    }
}
