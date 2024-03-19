package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberSimpleDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface MemberDaoSub {

    /*****************************************************
     * Select
     ****************************************************/

    /**
     * 닉네임에 해당하는 카운트 가져오기: 닉네임 중복 체크에 사용
     *
     * @param nick : 변경할 닉네임
     * @return
     */
    Integer getCountByNick(String nick);

    /**
     * CI, DI 정보로 회원 정보 가져오기
     *
     * @param memberDto
     * @return
     */
    List<MemberDto> getMemberInfoByCiDi(MemberDto memberDto);

    /**
     * 회원 아이디 유무 조회
     *
     * @param inputId (회원이 입력한 아이디)
     * @throws Exception
     */
    int getCountById(String inputId);

    /**
     * idx 로 회원정보 조회
     *
     * @param memberIdx
     * @return
     */
    MemberDto getMemberInfoByIdx(Long memberIdx);

    /**
     * 간편가입 유무 조회
     *
     * @param memberIdx
     * @return
     */
    int getMemberIsSimpleByIdx(Long memberIdx);

    /**
     * 간편가입 정보 조회
     *
     * @param memberIdx
     * @return
     */
    MemberSimpleDto getMemberSimpleInfoByIdx(Long memberIdx);

    /**
     * 회원탈퇴 관련 혜택 받은적 있는지 체크
     *
     * @param memberIdx
     * @return
     */
    int getDeleteBenefitCntByIdx(Long memberIdx);

    /**
     * 비밀번호 변경을 위해 이전 비밀번호 조회
     *
     * @param memberDto
     * @return
     */
    String getOldPassword(MemberDto memberDto);

    /**
     * 회원 환경 설정 정보(코인 알림)
     *
     * @param memberIdx
     * @return
     */
    MemberDto getSettingInfo(Long memberIdx);

    /**
     * 특정 ci 개수 정보 조회
     * 동일 회원이 다계정 보유하고 있는지 체크
     *
     * @param ci : 검색할 ci
     * @return
     */
    int getMemberCiCnt(String ci);

    /**
     * 특정 ci 개수 정보 조회
     * OTT 가입 이벤트 마일리지 받았는지 체크
     *
     * @param memberIdx : 회원 idx
     * @return
     */
    int getMemberOttCnt(Long memberIdx);

    /**
     * 회원 CI 정보 조회
     *
     * @param searchDto : memberIdx(회원 idx)
     * @return
     */
    String getMemberCi(SearchDto searchDto);

    /**
     * 회원 가입 사이트 정보 조회
     *
     * @param memberIdx : 회원 idx
     * @return
     */
    String getMemberSite(Long memberIdx);
}
