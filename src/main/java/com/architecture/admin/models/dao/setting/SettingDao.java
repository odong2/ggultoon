package com.architecture.admin.models.dao.setting;

import com.architecture.admin.models.dto.setting.SettingDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SettingDao {

    /*****************************************************
     * INSERT
     ****************************************************/
    /**
     * 마이페이지 - 환경설정 정보 등록 (회원가입 시)
     *
     * @param settingList (회원 idx, 옵션 idx, 상태값, 등록일)
     */
    int insertMemberSetting(List<SettingDto> settingList);


    /*****************************************************
     * UPDATE
     ****************************************************/

    /**
     * 마이페이지 - 환경설정 상태 변경
     *
     * @param settingDto (회원 idx, 옵션 idx, 변경 후 상태값)
     */
    int modifyMemberSetting(SettingDto settingDto);
}
