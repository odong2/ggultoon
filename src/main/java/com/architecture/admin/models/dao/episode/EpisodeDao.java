package com.architecture.admin.models.dao.episode;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.episode.EpisodeDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface EpisodeDao {

    /**
     * 회차 뷰 카운트 업데이트
     *
     * @param episodeDto : idx(회차 idx), view(뷰 카운트)
     */
    void updateViewCnt(EpisodeDto episodeDto);

    /**
     * 최근 본 작품 등록
     *
     * @param episodeDto
     */
    void insertMemberLastView(EpisodeDto episodeDto);

    /**
     * 최근 본 작품 업데이트
     *
     * @param episodeDto
     * @return
     */
    int updateMemberLastView(EpisodeDto episodeDto);

    /**
     * 최근 본 내역 삭제 (내 서재)
     *
     * @param searchDto
     * @return
     */
    int deleteMemberLastViewList(SearchDto searchDto);

}
