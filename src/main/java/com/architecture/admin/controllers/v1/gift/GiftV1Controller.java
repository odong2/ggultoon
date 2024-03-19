package com.architecture.admin.controllers.v1.gift;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.services.gift.GiftService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.architecture.admin.libraries.utils.DeviceUtils.*;
import static com.architecture.admin.libraries.utils.DeviceUtils.ORIGIN;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/gifts")
public class GiftV1Controller extends BaseController {

    private final GiftService giftService;
    private String searchSuccessMsg = "lang.common.success.search"; // 조회 완료하였습니다.

    /**
     * 선물함
     * 회원이 지급 받은 선물 리스트 조회
     *
     * @param searchDto : contentsIdx(작품 idx)
     */
    @GetMapping()
    public String memberGiftList(HttpServletRequest request,
                                 @ModelAttribute @Valid SearchDto searchDto,
                                 BindingResult result) {

        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 회원 세션 정보 체크
        super.checkSession();

        // 회원 idx 세팅
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        searchDto.setMemberIdx(memberIdx);

        // 회원 성인 여부 세팅
        Integer adult = Integer.valueOf(getMemberInfo(SessionConfig.ADULT));
        searchDto.setAdult(adult);

        // device 세팅
        String device = request.getHeader("User-Agent");

        // 모바일
        if (isMobile(device)) {
            searchDto.setDevice(ORIGIN);
            // 태블릿
        } else if (isTablet(device)) {
            searchDto.setDevice(ORIGIN);
            // pc
        } else {
            searchDto.setDevice(ORIGIN);
        }

        // 회원이 지급 받은 선물 리스트 조회
        JSONObject data = giftService.getMemberGiftList(searchDto);

        String message = super.langMessage(searchSuccessMsg); // 조회 완료하였습니다.
        return displayJson(true, "1000", message, data);
    }
}
