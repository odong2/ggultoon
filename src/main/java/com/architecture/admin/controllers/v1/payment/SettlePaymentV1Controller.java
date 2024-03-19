package com.architecture.admin.controllers.v1.payment;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.payment.PaymentMethodDto;
import com.architecture.admin.models.dto.payment.PaymentNotiDto;
import com.architecture.admin.models.dto.product.ProductDto;
import com.architecture.admin.services.member.MemberService;
import com.architecture.admin.services.payment.PaymentService;
import com.architecture.admin.services.payment.SettlePaymentService;
import com.architecture.admin.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/payment/")
public class SettlePaymentV1Controller extends BaseController {

    private final SettlePaymentService settlePaymentService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final MemberService memberService;
    private String paymentDisabledMsg = "lang.payment.exception.shutdown"; // 서비스 종료로 결제가 불가능해요

    /**
     * 결제 해시키 생성
     * @param map 업체 전달값 -> map
     * @return 결제 정보 암호화 
     * @throws Exception
     */
    @RequestMapping("encryptParams")
    public String encryptParams(@RequestParam HashMap<String, Object> map) throws Exception {

        //map.put();
        //HttpServletRequest a = (HttpServletRequest)map;
        //결제 해시키 생성
        return new JSONObject(settlePaymentService.encryptParams(map)).toString();
    }

    /**
     * 결제 결과 noti 처리
     * @param request 업체 전달값
     * @param paymentNotiDto 업체 전달값
     * @return OK : 성공, FAIL : 실패 - 성공 아니면 재시도 처리
     */
    @RequestMapping("receiveNoti")
    public String receiveNoti(HttpServletRequest request, PaymentNotiDto paymentNotiDto) {

        return settlePaymentService.receiveNoti(request,paymentNotiDto);
    }

    /**
     * 정상 결제수단 가져오기
     * @param paymentMethodDto
     * @return
     * @throws Exception
     */
    @GetMapping("method")
    public String getPaymentMethodList(PaymentMethodDto paymentMethodDto) {
        JSONObject data = new JSONObject();
        data.put("list",paymentService.getPaymentMethodList(paymentMethodDto));

        return displayJson(true, "1000", "", data);
    }

    /**
     * 결제 정보
     * @param paymentMethodDto
     * @return
     * @throws Exception
     */
    @RequestMapping("info")
    public String getPaymentInfo(
            @RequestParam(value = "pid" ) Integer pid,
            PaymentMethodDto paymentMethodDto,
            ProductDto productDto
    ) throws Exception
    {

        /** 꿀툰 서비스 종료 -> 결제 불가 처리 **/
        String message = super.langMessage(paymentDisabledMsg); // 서비스 종료로 결제가 불가능해요
        return displayJson(false, "1000", message);

        // 주문 날짜/시간 형식
//        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
//        SimpleDateFormat time = new SimpleDateFormat("hhmmss");
//
//        // 결제정보 조회
//        JSONObject data = new JSONObject(paymentService.getPaymentMethod(paymentMethodDto));
//        // 상품정보 조회
//        productDto.setIdx(pid);
//        ProductDto ProductData =  productService.getProduct(productDto);
//
//        // 회원 기본 정보 set
//        JSONObject memberInfo = memberService.getMemberInfoByIdx(Long.valueOf(getMemberInfo(SessionConfig.IDX)));
//        //pushAlarm(memberDto.toString(),"LJH");
//        // 삼성페이 추가 코드
//        if(paymentMethodDto.getMethodType().equals("samsungPay")) {
//            // 특정결제 코드
//            data.put("cardGb", "SSP");
//            // 추가 결제정보
//            data.put("methodSub", "direct");
//        }
//
//        // 겳제URL
//        data.put("env", data.getString("paymentServer"));
//        // 회원아이디
//        data.put("plainMchtCustId",getMemberInfo(SessionConfig.LOGIN_ID));
//        // 회원이름
//        if(memberInfo.has("name")) data.put("plainMchtCustNm",memberInfo.getString("name"));
//        // 이메일
//        if(memberInfo.has("email")) data.put("plainEmail",memberInfo.getString("email"));
//        // 상품명
//        data.put("pmtPrdtNm",ProductData.getTitle());
//        // 상품가격
//        data.put("plainTrdAmt",ProductData.getPrice());
//        // 주문날짜
//        data.put("trdDt",date.format(new Date()));
//        // 주문시간
//        data.put("trdTm",time.format(new Date()));
//        // 주문번호 아이디 + 날짜 + 시간
//        data.put("mchtTrdNo",
//                data.getString("plainMchtCustId")
//                + data.getString("trdDt")
//                + data.getString("trdTm")
//        );
//        // 상품제공기간 날짜 + 시간
//        data.put("prdtTerm", data.getString("trdDt") + data.getString("trdTm"));
//
//        // 암호화 처리
//        Map<String, Object> map = data.toMap();
//        JSONObject encryptParams = new JSONObject(settlePaymentService.encryptParams(map).toString());
//        JSONObject encParams = encryptParams.getJSONObject("encParams");
//
//        // 상품가격 암호화
//        data.put("trdAmt",encParams.getString("trdAmt"));
//        // 회원아이디 암호화
//        data.put("mchtCustId",encParams.getString("mchtCustId"));
//        // 회원이름 암호화
//        data.put("mchtCustNm",encParams.getString("mchtCustNm"));
//        // 회원이메일 암호화
//        data.put("email",encParams.getString("email"));
//
//        // 암호화키
//        data.put("pktHash",encryptParams.getString("hashCipher"));
//
//        // 추가 파라메터
//        JSONObject mchtParam = new JSONObject();
//        // 상품번호
//        mchtParam.put("pid", pid);
//        // 상품번호
//        mchtParam.put("midx",getMemberInfo("idx"));
//        // 데이터 추가
//        data.put("mchtParam",mchtParam.toString());
//        // 앱스킴 추가
//        data.put("appScheme","ggultoon");
//
//        pushAlarm(data.toString(),"LJH");
//
//        return displayJson(true, "1000", "", data);
    }

}