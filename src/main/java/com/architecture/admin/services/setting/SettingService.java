package com.architecture.admin.services.setting;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.member.MemberDao;
import com.architecture.admin.models.dao.setting.SettingDao;
import com.architecture.admin.models.daosub.setting.SettingDaoSub;
import com.architecture.admin.models.dto.setting.SettingDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class SettingService extends BaseService {

    private final SettingDaoSub settingDaoSub;
    private final SettingDao settingDao;
    private final MemberDao memberDao;

    /********************************************************************************
     * SELECT
     ********************************************************************************/

    /**
     * 마이페이지 - 환경설정
     *
     * @param memberIdx (회원 idx)
     */
    public JSONObject getMemberSettingList(Long memberIdx) {

        // 데이터를 담아서 리턴할 객체 생성
        JSONObject jsonData = new JSONObject();
        List<SettingDto> settingList = null;

        // 환경설정 목록 전체 카운트
        int totalCnt = settingDaoSub.getTotalSettingCnt(memberIdx);

        // 환경설정 목록이 있다면
        if (totalCnt > 0) {

            // 환경설정 목록 조회
            settingList = settingDaoSub.getMemberSettingList(memberIdx);

            // 환경설정 상태값 문자변환
            settingText(settingList);
        }

        // list
        jsonData.put("settingList", settingList);

        return jsonData;
    }

    /********************************************************************************
     * UPDATE
     ********************************************************************************/

    /**
     * 마이페이지 - 환경설정 상태 변경
     *
     * @param settingDto (회원 idx, 환경설정 idx)
     */
    public void modifyMemberSetting(SettingDto settingDto) {
        
        // 환경설정 유효성 검사 -> 변경할 환경설정 정보 조회
        SettingDto setting = settingValidate(settingDto);

        // 환경설정 변경할 상태값 set
        if (setting.getState() == 0) { // 현재 꺼짐 상태라면
            settingDto.setState(1); // 켜기
            
            
        } else if (setting.getState() == 1) { // 현재 켜짐 상태라면
            settingDto.setState(0); // 끄기
        }

        // 설정 변경일 세팅
        settingDto.setModifyDate(dateLibrary.getDatetime());

        // 환경설정 상태 변경
        int result = settingDao.modifyMemberSetting(settingDto);

        // 변경하려는 설정이 [광고, 혜택 알림]일 경우 member_policy 테이블의 marketing 상태값 업데이트
        if (settingDto.getSettingIdx() == 3) {
            
            if (settingDto.getState() == 1) { // 설정을 ON으로 변경하는 경우
                settingDto.setStateText("Y");
                settingDto.setRegdate(dateLibrary.getDatetime());

            } else { // 설정을 OFF로 변경하는 경우
                settingDto.setStateText("N");
                settingDto.setRegdate(dateLibrary.getDatetime());
            }
            // 마케팅 수신 동의 상태값 변경
            memberDao.updateMemberMarketing(settingDto);
        }

        // 환경설정 상태 변경 실패한 경우
        if (result < 1) {
            throw new CustomException(CustomError.SETTING_SWITCH_FAIL); // 환경설정 상태를 변경할 수 없습니다.
        }
    }

    /********************************************************************************
     * SUB
     ********************************************************************************/

    /**
     * 회원 환경설정 정보 문자변환
     *
     * @param memberSettingList (회원 환경설정 정보)
     */
    private void settingText(List<SettingDto> memberSettingList) {

        for (SettingDto memberSettingDto : memberSettingList) {

            if(memberSettingDto != null) {

                if (memberSettingDto.getState() == 0) { // 환경설정 옵션 끈 경우
                    memberSettingDto.setStateText(super.langMessage("lang.setting.off")); // OFF

                } else if (memberSettingDto.getState() == 1) { // 환경설정 옵션 킨 경우
                    memberSettingDto.setStateText(super.langMessage("lang.setting.on")); // ON
                }
            }
        }
    }

    /**************************************************************************
     * Validation
     **************************************************************************/

    /**
     * 환경설정 IDX 유효성 검사
     *
     * @param settingDto (회원 idx, 환경설정 idx)
     * @throws Exception
     */
    private SettingDto settingValidate(SettingDto settingDto) {

        // 선택한 환경설정 idx가 없는 경우
        if (settingDto.getSettingIdx() == null || settingDto.getSettingIdx() < 1) {
            throw new CustomException(CustomError.SETTING_IDX_ERROR); // 변경할 환경설정 옵션을 선택해주세요.
        }

        // DB에 존재하지 않는 환경설정 idx 값인 경우
        SettingDto setting = settingDaoSub.getMemberSetting(settingDto);
        if (setting == null) {
            throw new CustomException(CustomError.SETTING_IDX_NOT_EXIST); // 변경할 환경설정 옵션을 찾을 수 없습니다.
        }
        
        // 변경할 환경설정 정보 리턴
        return setting;
    }
}
