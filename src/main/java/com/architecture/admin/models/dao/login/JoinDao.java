package com.architecture.admin.models.dao.login;

import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberOttDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface JoinDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 회원 등록
     *
     * @param memberDto 회원 dto
     * @return insertedIdx
     */
    Integer insert(MemberDto memberDto);

    /**
     * 회원 정보 등록
     *
     * @param memberDto 회원 dto
     */
    void insertInfo(MemberDto memberDto);

    /**
     * 간편로그인 등록
     *
     * @param memberDto 회원 dto
     */
    void insertSimple(MemberDto memberDto);

    /**
     * OTT 회원 정보 입력
     *
     * @param memberOttDto : OTT회원 정보
     */
    void insertMemberOtt(MemberOttDto memberOttDto);

    /**
     * OTT 회원 정보 업데이트
     *
     * @param memberOttDto : OTT회원 정보
     */
    void updateMemberOtt(MemberOttDto memberOttDto);

    /**
     * OTT 통계 정보 입력
     *
     * @param memberOttDto : OTT회원 정보
     */
    void insertEventOtt(MemberOttDto memberOttDto);

    /**
     * 이벤트 LOG 입력
     *
     * @param memberOttDto : OTT회원 정보
     */
    void insertEventLog(MemberOttDto memberOttDto);

}
