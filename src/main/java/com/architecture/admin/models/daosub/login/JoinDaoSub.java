package com.architecture.admin.models.daosub.login;

import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface JoinDaoSub {
    /*****************************************************
     * Select
     ****************************************************/

    /**
     * 아이디에 해당하는 카운트 가져오기: 아이디 중복 체크에 사용
     *
     * @param memberDto 회원 Dto
     * @return 아이디 카운트 값
     */
    Integer getCountById(MemberDto memberDto);

    /**
     * 아이디에 해당하는 카운트 가져오기: 아이디 중복 체크에 사용
     *
     * @param memberDto 회원 Dto
     * @return 본인인증 번호 카운트 값
     */
    String getCountByTxseq(MemberDto memberDto);


    /**
     *
     * @param memberDto
     * @return 이벤트 참여내역
     */
    MemberOttDto getEventCiCheck(MemberDto memberDto);

    /**
     *
     * @param memberOttDto
     * @return 이벤트 참여횟수
     */
    Long getEventCheck(MemberOttDto memberOttDto);

    /**
     *
     * @param memberIdx 회원번호
     * @return 이벤트 회원 정보
     */
    MemberOttDto getEventMember(Long memberIdx);

    /**
     *
     * @param memberDto 회원 정보
     * @return 이벤트 회원 정보
     */
    List<MemberOttDto> getEventCiCheckList(MemberDto memberDto);

}
