package com.architecture.admin.services.policy;

import com.architecture.admin.models.daosub.policy.PolicyDaoSub;
import com.architecture.admin.models.dto.policy.PolicyDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*****************************************************
 * 이용약관 모델러
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class PolicyService extends BaseService {
    private final PolicyDaoSub policyDaoSub;

    /**
     * 이용약관 목록
     *
     * @return
     */
    public List<PolicyDto> getList() {
        String localeLang = super.getLocaleLang();

        return policyDaoSub.getList(localeLang);
    }
}
