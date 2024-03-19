package com.architecture.admin.models.daosub.banner;

import com.architecture.admin.models.dto.banner.BannerDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface BannerDaoSub {

    /**
     * 배너 조회
     * @param bannerDto
     * @return
     */
    List<BannerDto> getBannerList(BannerDto bannerDto);

    /**
     * 배너 매핑 정보
     * @param bannerMappingIdx
     * @return
     */
    BannerDto getBannerMapping(Integer bannerMappingIdx);
}
