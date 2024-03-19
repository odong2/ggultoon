package com.architecture.admin.controllers.v1.policy;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.policy.PolicyDto;
import com.architecture.admin.services.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/policy")
public class PolicyV1Controller extends BaseController {
    private final PolicyService policyService;

    /**
     * 이용약관 목록
     *
     * @return
     */
    @GetMapping("/list")
    public String lists() {

        List<PolicyDto> list = policyService.getList();

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);

        JSONObject data = new JSONObject(map);
        String message = super.langMessage("");

        return displayJson(true, "1000", message, data);
    }
}
