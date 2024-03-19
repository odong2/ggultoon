package com.architecture.admin.controllers.v1.banner;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.banner.BannerDto;
import com.architecture.admin.services.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/banner")
public class BannerV1Controller extends BaseController {

    private final BannerService bannerService;

    private String searchSuccessMsg = "lang.common.success.search"; // 조회를 완료하였습니다.

    /**
     * 배너 조회
     * @param bannerDto
     * @param request
     * @return
     */
    @GetMapping()
    public String getBannerList(BannerDto bannerDto, HttpServletRequest request) {

        // 배너 조회
        JSONObject data = bannerService.getBannerList(bannerDto, request);

        // 결과 메시지 처리
        String message = super.langMessage(searchSuccessMsg);

        return displayJson(true, "1000", message, data);
    }

    /**
     * 배너 유입 통계
     * 배너 클릭 수 업데이트
     * @param bannerMappingIdx
     */
    @PostMapping("/visit/{bannerMappingIdx}")
    public void bannerVisit(@PathVariable(name = "bannerMappingIdx") Integer bannerMappingIdx) {

        // 배너 클릭 수 업데이트
        bannerService.getBannerMapping(bannerMappingIdx);
    }
}
