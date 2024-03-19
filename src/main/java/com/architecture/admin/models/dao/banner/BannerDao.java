package com.architecture.admin.models.dao.banner;

import com.architecture.admin.models.dto.banner.BannerDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface BannerDao {

    /**
     * 배너 클릭 수 업데이트
     * @param banner
     */
    void setBannerClick(BannerDto banner);
}
