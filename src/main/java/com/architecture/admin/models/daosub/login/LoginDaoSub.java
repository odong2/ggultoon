package com.architecture.admin.models.daosub.login;

import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface LoginDaoSub {
    /*****************************************************
     * Select
     ****************************************************/

    /**
     * 자동로그인키 조회
     *
     * @param memberDto 회원 Dto
     * @return 자동로그인 키
     */
    String getLoginKey(MemberDto memberDto);

}
