package com.architecture.admin.models.dao.member;

import com.architecture.admin.models.dto.member.MemberAppDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberSimpleDto;
import com.architecture.admin.models.dto.setting.SettingDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

@Repository
@Mapper
public interface MemberDao {

    /*****************************************************
     * Insert
     ****************************************************/

    /**
     * 회원 탈퇴(member_out 테이블 insert)
     *
     * @param memberDto
     */
    void insertOutMember(MemberDto memberDto);

    /**
     * 회원 정보입력(member_info 테이블 insert)
     *
     * @param memberDto
     */
    void insertMemberInfo(MemberDto memberDto);
    /**
     * 회원 정책입력(member_policy 테이블 insert)
     *
     * @param memberDto
     */
    void insertMemberPolicy(MemberDto memberDto);

    /*****************************************************
     * Update
     ****************************************************/

    /**
     * 닉네임 변경
     *
     * @param memberDto
     */
    void modifyNick(MemberDto memberDto);

    /**
     * 비밀번호 변경
     * member 테이블
     *
     * @param memberDto
     */
    void modifyPasswordLogin(MemberDto memberDto);

    /**
     * 비밀번호 찾기 성공 > 비밀번호 재설정
     *
     * @param memberDto 비밀번호 찾기를 통해 리턴받은 회원의 id, ci, di 정보 + 입력받은 newPassword(변경할 비밀번호), newPasswordConfirm(비밀번호 확인)
     */
    int modifyPasswordNonLogin(MemberDto memberDto);

    /**
     * 회원 정보 업데이트
     *
     * @param memberDto 회원dto
     */
    void updateMemberInfo(MemberDto memberDto);

    /*****************************************************
     * Delete
     ****************************************************/

    /**
     * 회원 탈퇴(member 테이블)
     *
     * @param memberIdx
     * @return
     */
    int deleteMember(Long memberIdx);

    /**
     * 회원 탈퇴(member_info 테이블)
     *
     * @param memberIdx
     * @return
     */
    int deleteMemberInfo(Long memberIdx);

    /**
     * 간편가입 탈퇴
     *
     * @param memberSimpleDto
     */
    void insertMemberSimpleOut(MemberSimpleDto memberSimpleDto);

    /**
     * 특별 혜택 받음으로 업데이트
     *
     * @param memberIdx
     * @return
     */
    int updateDeleteBenefit(Long memberIdx);

    /**
     * 코인 알림 받지 않음 설정
     *
     * @param memberDto : idx(회원 idx), coinAlarm(코인 알림 상태값)
     */
    void modifyCoinAlarm(MemberDto memberDto);

    /**
     * 본인인증 로그 저장
     *
     * @param okCertLog : 본인인증 정보
     */
    void insertLog(HashMap<String, String> okCertLog);

    /**
     * 광고아이디 저장
     *
     * @param memberAppDto : App회원 관련 정보
     */
    void insertAdid(MemberAppDto memberAppDto);

    /**
     * 알림토큰 저장
     *
     * @param memberAppDto : App회원 관련 정보
     */
    void insertAppToken(MemberAppDto memberAppDto);
    
    /**
     * 성인인증 정보 업데이트
     *
     * @param memberDto : idx(회원 idx), adult(성인인증 상태값)
     */
    void modifyAdult(MemberDto memberDto);

    /**
     * 마케팅 수신 동의 상태 정보 업데이트
     *
     * @param settingDto : 마케팅 수신 동의 여부 상태값, 변경일
     */
    void updateMemberMarketing(SettingDto settingDto);


}
