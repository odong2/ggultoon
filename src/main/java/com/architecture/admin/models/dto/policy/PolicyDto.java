package com.architecture.admin.models.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyDto {

    /**
     * sns_policy
     **/
    private Integer idx;        // 고유번호
    private String title;       // 약관제목
    private String required;    //필수값 여부
    private Integer state;      // 상태값
    private String regdate;     // 등록일
    private String regdateTz;   // 등록일 타임존

    /**
     * sns_policy_detail
     **/
    private Integer policyIdx;  // 약관번호
    private String detail;      // 약관내용
    private String lang;        // 언어

    /**
     * sns_policy_name
     **/
    private String name;        //약관명

    /**
     * sns_policy_agree
     **/
    private Integer memberIdx;  // 회원 번호

    // sql
    private Integer insertedIdx;
    private Integer affectedRow;


}
