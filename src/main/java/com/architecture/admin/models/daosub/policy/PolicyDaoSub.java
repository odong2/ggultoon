package com.architecture.admin.models.daosub.policy;

import com.architecture.admin.models.dto.policy.PolicyDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface PolicyDaoSub {

    /**
     * 약관 목록 가져오기
     * 
     * @param lang 사용언어
     * @return PolicyDto
     */
    List<PolicyDto> getList(String lang);

}
