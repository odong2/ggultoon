package com.architecture.admin.models.daosub.content;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.content.ContentDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Repository
@Mapper
public interface ContentDaoSub {

    /**
     * 유효한 컨텐츠 개수 조회
     *
     * @param contentDto
     * @return
     */
    int getContentCnt(ContentDto contentDto);

    /**
     * 유효한 카테고리인지 조회
     *
     * @param categoryIdx (선택한 카테고리 idx)
     * @return
     */
    int getCategoryCountByIdx(Integer categoryIdx);

    /**
     * 유효한 장르인지 조회
     *
     * @param genreIdx (선택한 장르 idx)
     * @return
     */
    int getGenreCountByIdx(Integer genreIdx);

    /**
     * 유효한 컨텐츠인지 조회
     *
     * @param searchDto
     * @return
     */
    int getContentCountByIdx(SearchDto searchDto);

    /**
     * 유효한 관심 idxList 인지 조회
     *
     * @param searchDto : idxList, memberIdx
     * @return
     */
    int getFavoriteIdxListCnt(SearchDto searchDto);

    /**
     * 컨텐츠 idx 조회
     *
     * @param episodeIdx
     * @return
     */
    int getContentsIdxByEpisodeIdx(Long episodeIdx);

    /**
     * 컨텐츠 카테고리 조회
     *
     * @param idx
     * @return
     */
    int getContentCategory(Integer idx);

    /**
     * 컨텐츠 성인 여부 조회
     *
     * @param idx
     * @return
     */
    int getContentAdult(Integer idx);

    /**
     * 이벤트 무료 회차 전체 조회
     *
     * @param nowDate
     * @return
     */
    List<Integer> getEventFreeEpisodeInfo(String nowDate);

    /**
     * 이벤트 할인 중인 회차 개수 전체 카운트
     *
     * @param nowDate
     * @return
     */
    List<ContentDto> getEventEpisodeCount(String nowDate);

    /**
     * 컨텐츠 할인 정보 조회
     *
     * @param episodeDto
     * @return
     */
    ContentDto getContentFreeInfo(EpisodeDto episodeDto);

    /**
     * 컨텐츠 제목 조회
     *
     * @param idx
     * @return
     */
    String getContentsTitleByIdx(Integer idx);

    /**
     * 큐레이션 리스트 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getCurationTotalCnt(SearchDto searchDto);

    /**
     * 큐레이션 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<ContentDto> getCurationList(SearchDto searchDto);

    /**
     * 큐레이션에 들어간 작품 리스트 조회
     *
     * @param curationDto
     * @return
     */
    List<ContentDto> getCurationContentList(ContentDto curationDto);

    /**
     * 카테고리 리스트 개수 카운트
     *
     * @param searchDto
     * @return int
     */
    int getCategoryContentsTotalCnt(SearchDto searchDto);

    /**
     * 카테고리 리스트 조회
     *
     * @param searchDto
     * @return List<ContentDto>
     */
    List<ContentDto> getContentsList(SearchDto searchDto);

    /**
     * 랭크 리스트 개수 카운트
     * @param searchDto
     * @return
     */
    Integer getRankingContentsListTotalCnt(SearchDto searchDto);

    /**
     * 랭크 리스트 조회
     * @param searchDto
     * @return
     */
    List<ContentDto> getRankingContentsList(SearchDto searchDto);

    /**
     * 컨텐츠 상세 정보 유무 조회
     *
     * @param searchDto
     * @return ContentDto
     */
    int getContentInfoCnt(SearchDto searchDto);

    /**
     * 컨텐츠 상세 정보 조회
     *
     * @param searchDto
     * @return ContentDto
     */
    List<ContentDto> getContent(SearchDto searchDto);

    /**
     * 컨텐츠 조회수 조회
     *
     * @param idx
     * @return
     */
    int getContentViewCnt(Integer idx);

    /**
     * 작품 검색 결과 카운트
     *
     * @param searchDto
     */
    int getContentSearchTotalCnt(SearchDto searchDto);

    /**
     * 작품 검색 결과 리스트
     *
     * @param searchDto
     */
    List<ContentDto> getContentSearchList(SearchDto searchDto);

    /**
     * 작가 검색 결과 카운트
     *
     * @param searchDto
     */
    int getAuthorSearchTotalCnt(SearchDto searchDto);

    /**
     * 작가 검색 결과 리스트
     *
     * @param searchDto
     */
    List<ContentDto> getAuthorSearchList(SearchDto searchDto);

    /**
     * 태그 검색 결과 카운트
     *
     * @param searchDto
     */
    int getTagSearchTotalCnt(SearchDto searchDto);

    /**
     * 태그 검색 결과 리스트
     *
     * @param searchDto
     */
    List<ContentDto> getTagSearchList(SearchDto searchDto);

    /**
     * 내가 찜한 작품 리스트 조회
     *
     * @param searchDto
     * @return
     */
    List<Integer> getMemberLikeContentList(SearchDto searchDto);

    /**
     * 관심 컨텐츠 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getFavoriteContentsTotalCnt(SearchDto searchDto);

    /**
     * 관심 컨텐츠 리스트
     *
     * @param searchDto
     * @return
     */
    List<ContentDto> getFavoriteContentsList(SearchDto searchDto);

    /**
     * 작품 정보 조회 (내 서재)
     *
     * @param searchDto
     * @return
     */
    ContentDto getContentsInfoFromLibrary(SearchDto searchDto);

    /**
     * 신고한 컨텐츠 조회
     *
     * @param contentDto
     * @return
     */
    ContentDto getContentReport(ContentDto contentDto);

    /**
     * 찜한 컨텐츠 조회
     *
     * @param contentDto
     * @return
     */
    ContentDto getContentFavorite(ContentDto contentDto);

    /**
     * 현재 작품의 찜 개수 카운트
     * @param contentDto
     * @return
     */
    int getFavoriteCnt(ContentDto contentDto);

    /**
     * 해당 작품이 성인 작품 또는 성인관 작품인지 체크
     *
     * @param contentsIdx
     * @return
     */
    ContentDto checkIsContentAdult(Integer contentsIdx);

    /**
     * 랭킹에 등록된 작품 idx 리스트 조회
     */
    List<Integer> getRankContentsIdxList();

    /**
     * 컨텐츠 상세 정보 조회
     * @param contentsIdx
     * @return
     */
    ContentDto getContentInfo(Integer contentsIdx);

    /**
     * 컨텐츠 제목 정보 조회
     * @param searchDto
     * @return
     */
    String getContentTitle(SearchDto searchDto);

    /**
     * 해당 작품의 카테고리 idx 조회
     * @param contentsIdx
     * @return
     */
    int getCategoryIdx(Integer contentsIdx);

    /**
     * 선택한 작품 정보 조회
     * @param searchDto
     * @return
     */
    ContentDto checkContentInfo(SearchDto searchDto);

    /**
     * 신규 회차가 업로드된 작품 및 회차 번호 조회
     * @param dto
     * @return
     */
    HashMap<String, Object> getNewEpisodeInfo(NotificationDto dto);

    /**
     * 완결작일 경우 해당 작품의 최종화 회차번호 조회
     * @param contentsIdx
     * @return
     */
    Integer getLastEpisodeNumber(Integer contentsIdx);


}
