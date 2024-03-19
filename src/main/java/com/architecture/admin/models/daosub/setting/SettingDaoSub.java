package com.architecture.admin.models.daosub.setting;

import com.architecture.admin.models.dto.setting.SettingDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SettingDaoSub {

    /*****************************************************
     * Select
     ****************************************************/

    /**
     * 마이페이지 - 환경설정 목록
     */
    List<SettingDto> getSettingList();

    /**
     * 마이페이지 - 환경설정 목록 전체 카운트
     *
     * @param memberIdx (회원 idx)
     * @return
     */
    int getTotalSettingCnt(Long memberIdx);

    /**
     * 마이페이지 - 회원의 환경설정 목록
     *
     * @param memberIdx (회원 idx)
     * @return
     */
    List<SettingDto> getMemberSettingList(Long memberIdx);

    /**
     * 마이페이지 - 선택한 환경설정 조회
     *
     * @param settingDto (회원 idx, 환경설정 idx)
     * @return
     */
    SettingDto getMemberSetting(SettingDto settingDto);

}
