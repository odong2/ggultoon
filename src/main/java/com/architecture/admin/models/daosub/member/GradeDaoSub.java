package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.member.MemberGradeDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface GradeDaoSub {

    /**
     * 회원 등급
     * @param memberIdx
     * @return
     */
    MemberGradeDto getMemberGrade(Long memberIdx);

    /**
     * 3개월 결제금액
     * @param map
     * @return
     */
    List<Map<String, Object>> getMonthPayment(Map<String, Object> map);
}
