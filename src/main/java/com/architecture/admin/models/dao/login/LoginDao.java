package com.architecture.admin.models.dao.login;

import com.architecture.admin.models.dto.member.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface LoginDao {

    /**
     * 로그인 성공시 회원 정보 가져오기
     *
     * @param memberDto 회원dto
     * @return MemberDto
     */
    MemberDto getInfoForLogin(MemberDto memberDto);

    /**
     * 로그인 성공시 회원 세팅 정보 가져오기
     *
     * @param memberDto
     * @return
     */
    MemberDto getSettingInfo(MemberDto memberDto);

    /**
     * 회원 로그인시간 업데이트
     *
     * @param memberDto 회원dto
     */
    void updateLoginDate(MemberDto memberDto);

    /**
     * 자동로그인키 입력
     *
     * @param memberDto 회원 Dto
     */
    void insertLoginKey(MemberDto memberDto);

    /**
     * 자동로그인키 삭제
     *
     * @param id 회원 아이디
     */
    void deleteLoginKey(String id);


}
