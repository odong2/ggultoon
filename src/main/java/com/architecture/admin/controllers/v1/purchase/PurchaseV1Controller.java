package com.architecture.admin.controllers.v1.purchase;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.purchase.PurchaseDto;
import com.architecture.admin.services.purchase.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase")
public class PurchaseV1Controller extends BaseController {
    private final PurchaseService purchaseService;
    private String searchSuccessMsg = "lang.common.success.search"; // 조회 완료하였습니다.

    /**
     * 개별 회차 구매
     *
     * @param contentIdx  : contents.idx
     * @param episodeIdx  : episode.idx
     * @param purchaseDto : type(구매 유형) -> 1: 대여, 2: 소장, isNotSendAlarm(코인 차감 안내 유무), route(구매경로)
     * @return
     */
    @PostMapping("/contents/{contentIdx}/episodes/{episodeIdx}")
    public String episodePurchase(@PathVariable Integer contentIdx,
                                  @PathVariable Long episodeIdx,
                                  @RequestBody PurchaseDto purchaseDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        purchaseDto.setContentsIdx(contentIdx);
        purchaseDto.setEpisodeIdx(episodeIdx);
        purchaseDto.setMemberIdx(memberIdx);
        purchaseDto.setNowDate(dateLibrary.getDatetime());

        /**  회차 개별 구매(소장 or 대여) **/
        purchaseService.purchaseEpisode(purchaseDto);

        // return value
        String message = super.langMessage("lang.purchase.success.episode.buy"); // 회차를 구매하였습니다.

        return displayJson(true, "1000", message);
    }

    /**
     * 전체 대여 팝업 정보
     *
     * @param contentIdx
     * @return
     */
    @GetMapping("/contents/{contentIdx}/rent")
    public String allRentPopupInfo(@PathVariable Integer contentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        PurchaseDto purchaseDto = PurchaseDto.builder()
                .contentsIdx(contentIdx)
                .memberIdx(memberIdx)
                .build();

        JSONObject data = purchaseService.getAllRentPupUpInfo(purchaseDto);

        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 전체 소장 팝업 정보
     *
     * @param contentIdx
     * @return
     */
    @GetMapping("/contents/{contentIdx}/have")
    public String allHavePopupInfo(@PathVariable Integer contentIdx) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        PurchaseDto purchaseDto = PurchaseDto.builder()
                .contentsIdx(contentIdx)
                .memberIdx(memberIdx)
                .build();

        JSONObject data = purchaseService.getAllHavePupUpInfo(purchaseDto);

        String message = super.langMessage(searchSuccessMsg); // 조회를 완료하였습니다.

        return displayJson(true, "1000", message, data);
    }

    /**
     * 전체 대여
     * @param contentIdx  : 컨텐츠 idx
     * @param purchaseDto : route(구매 경로)
     * @return
     */
    @PostMapping("contents/{contentIdx}/rent")
    public String contentsAllRent(@PathVariable Integer contentIdx,
                                  @RequestBody PurchaseDto purchaseDto) {

        // 회원 IDX session 체크
        super.checkSession();
        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        purchaseDto.setContentsIdx(contentIdx);
        purchaseDto.setMemberIdx(memberIdx);

        purchaseService.rentAllEpisode(purchaseDto);

        // return value
        String message = super.langMessage("lang.purchase.success.episode.buy.all"); // 전체 회차를 구매하였습니다.

        return displayJson(true, "1000", message);
    }


    /**
     * 전체 소장
     *
     * @param contentIdx : 컨텐츠 idx
     * @param purchaseDto : includeFree(무료 회차 포함 여부), route(구매 경로)
     * @return
     */
    @PostMapping("contents/{contentIdx}/have")
    public String contentsAllHave(@PathVariable Integer contentIdx,
                                  @RequestBody PurchaseDto purchaseDto) {

        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));

        purchaseDto.setContentsIdx(contentIdx);
        purchaseDto.setMemberIdx(memberIdx);
        // 무료 회차 포함여부
        Boolean isIncludeFree = purchaseDto.getIncludeFree();

        if (isIncludeFree == null) {
            throw new CustomException(CustomError.PURCHASE_INCLUDE_FREE_NOT_MATCH); // 무료 회차 포함여부가 불명확합니다.
        }

        // 뮤료 회차 포함 소장
        if (Boolean.TRUE == isIncludeFree) {
            purchaseService.haveAllIncludeFreeEpisode(purchaseDto);

        // 유료 회차만 소장
        } else {
            purchaseService.haveAllOnlyPaidEpisode(purchaseDto);
        }

        // return value
        String message = super.langMessage("lang.purchase.success.episode.buy.all"); // 전체 회차를 구매하였습니다.

        return displayJson(true, "1000", message);
    }

}