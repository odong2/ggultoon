package com.architecture.admin.models.daosub.episode;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import com.architecture.admin.models.dto.episode.EpisodeImgDto;
import com.architecture.admin.models.dto.episode.EpisodeLastViewDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface EpisodeDaoSub {

    /**
     * 회차 뷰 카운트 조회
     *
     * @param idx
     * @return
     */
    int getEpisodeViewCnt(Long idx);


    /**
     * 컨텐츠 전체 회차 개수
     *
     * @param episodeDto
     * @return
     */
    int getEpisodeTotalCnt(EpisodeDto episodeDto);

    /**
     * 회원 해당 컨텐츠 최근 본 회차 있는지 조회
     * 없으면 0 리턴
     *
     * @param episodeDto : contentIdx(회차 idx), memberIdx(회원 idx)
     * @return
     */
    int getMemberLastViewCnt(EpisodeDto episodeDto);

    /**
     * 유효한 회차인지 조회
     *
     * @param episodeDto : idx(에피소드 idx), nowDate(현재 시간)
     * @return
     */
    int getEpisodeCnt(EpisodeDto episodeDto);

    /**
     * 웹툰 회차 이미지 리스트 조회
     *
     * @param episodeDto
     * @return
     */
    List<EpisodeImgDto> getEpisodeWebtoonImgList(EpisodeDto episodeDto);

    /**
     * 만화 회차 이미지 리스트 조회
     *
     * @param episodeDto
     * @return
     */
    List<EpisodeImgDto> getEpisodeComicImgList(EpisodeDto episodeDto);

    /**
     * 소설 회차 이미지 리스트 조회
     *
     * @param idx
     * @return
     */
    List<EpisodeImgDto> getEpisodeNovelImgList(Long idx);

    /**
     * 소설 커버 이미지 url 조회
     *
     * @param episodeDto
     * @return
     */
    EpisodeImgDto getNovelCoverImg(EpisodeDto episodeDto);

    /**
     * 뷰어 회차 리스트
     *
     * @param searchDto
     * @return
     */
    List<EpisodeDto> getEpisodeList(SearchDto searchDto);

    /**
     * 무료 회차 조회
     *
     * @param contentIdx
     * @return
     */
    int getFreeEpisodeCnt(Integer contentIdx);

    /**
     * 이벤트 무료회차 조회
     *
     * @param episodeDto
     * @return
     */
    int getEventFreeEpisodeCnt(EpisodeDto episodeDto);

    /**
     * 회차 idx 조회
     *
     * @param episodeDto : sort(회차 순서), contentIdx(컨텐츠 idx)
     * @return
     */
    Long getIdxBySort(EpisodeDto episodeDto);

    /**
     * 무료 회차 idx 리스트 조회
     *
     * @param episodeDto : sort , contentIdx
     * @return
     */
    List<Long> getFreeIdxListBySort(EpisodeDto episodeDto);

    /**
     * 해당 회차 정보 조회(뷰어에서 필요한 정보)
     *
     * @param idx
     * @return
     */
    EpisodeDto getEpisodeInfo(Long idx);

    /**
     * 컨텐츠 전체 회차 개수 조회 by SearchDto
     *
     * @param searchDto : contentIdx, nowDate
     * @return
     */
    int getEpisodeTotalCntBySearchDto(SearchDto searchDto);

    /**
     * 컨텐츠 회차 개수 카운트
     *
     * @param searchDto
     * @return
     */
    int getContentEpisodesTotalCnt(SearchDto searchDto);

    /**
     * 컨텐츠 회차 리스트 조회(로그인, 비로그인)
     *
     * @param searchDto
     * @return
     */
    List<EpisodeDto> getContentEpisodeList(SearchDto searchDto);

    /**
     * 회원이 마지막으로 본 회차 IDX + 회차 번호 조회
     *
     * @param searchDto
     * @return
     */
    EpisodeLastViewDto getMemberLastViewNumber(SearchDto searchDto);

    /**
     * 해당 작품의 첫번째 회차 IDX + 회차 번호 조회
     *
     * @param searchDto
     * @return
     */
    Map<String, Object> getFirstEpisodeInfo(SearchDto searchDto);

    /**
     * 회원이 대여한 회차 조회
     *
     * @param searchDto
     * @return
     */
    List<EpisodeDto> getMemberRentList(SearchDto searchDto);

    /**
     * 회원이 소장한 회차 번호 조회
     *
     * @param searchDto
     * @return
     */
    List<Long> getMemberHaveList(SearchDto searchDto);

    /**
     * 무료 회차 & 이벤트 무료 회차 & 이벤트 진행 상태 조회
     *
     * @param searchDto
     * @return
     */
    EpisodeDto episodeFreeAndEventInfo(SearchDto searchDto);

    /**
     * 무료 회차 & 이벤트 무료 회차 & 이벤트 진행 상태 조회
     *
     * @param contentsIdx
     * @return
     */
    EpisodeDto getEpisodeFreeInfo(Integer contentsIdx);

    /**
     * 회차 이벤트 대여 코인 조회
     *
     * @param episodeDto : idx(회차 idx), nowDate(현재 시간)
     * @return
     */
    Integer getEpisodeEventRentCoin(EpisodeDto episodeDto);

    /**
     * 회차 이벤트 소장 코인 조회
     *
     * @param episodeDto
     * @return
     */
    Integer getEpisodeEventCoin(EpisodeDto episodeDto);

    /**
     * 회차 이벤트 리스트 조회(episode_idx, coin)
     *
     * @param episodeDto : contentsIdx(컨텐츠 idx), nowDate(현재 시간)
     * @return
     */
    List<EpisodeDto> getEpisodeEvent(EpisodeDto episodeDto);

    /**
     * 회차 이벤트 단일 조회
     *
     * @param episodeDto : idx(회차 idx), nowDate(현재 시간)
     * @return
     */
    EpisodeDto getEpisodeEventByIdx(EpisodeDto episodeDto);

    int getEpisodeSortByIdx(Long episodeIdx);

    /**
     * 해당 작품의 전체 회차 idx 리스트
     *
     * @param searchDto : contentsIdx(작품 idx), nowDate(현재 시간)
     * @return
     */
    List<Long> getEpisodeIdxList(SearchDto searchDto);
}
