package com.architecture.admin.services.payment;

import com.architecture.admin.models.dao.notification.NotificationDao;
import com.architecture.admin.libraries.CurlLibrary;
import com.architecture.admin.models.dao.payment.PaymentDao;
import com.architecture.admin.models.daosub.payment.PaymentDaoSub;
import com.architecture.admin.models.dto.coin.CoinDto;
import com.architecture.admin.models.dto.payment.PaymentAppDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import com.architecture.admin.models.dto.payment.PaymentDto;
import com.architecture.admin.models.dto.payment.PaymentMethodDto;
import com.architecture.admin.models.dto.payment.PaymentNotiDto;
import com.architecture.admin.models.dto.product.ProductDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.libraries.utils.pg.EncryptUtil;
import com.architecture.admin.libraries.utils.pg.StringUtil;
import com.architecture.admin.services.coin.CoinService;
import com.architecture.admin.services.member.GradeService;
import com.architecture.admin.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static com.architecture.admin.libraries.utils.CoinUtils.*;
import static com.architecture.admin.libraries.utils.NotificationUtils.CHARGE;

@RequiredArgsConstructor
@Service
@Transactional
public class SettlePaymentService extends BaseService {
    // 노티 처리 관련 로거
    private final Logger notiLogger = LoggerFactory.getLogger("notiTrans");

    private final PaymentDao paymentDao;
    private final PaymentDaoSub paymentDaoSub;
    private final NotificationDao notificationDao;
    private final CoinService coinService;
    private final ProductService productService;
    private final GradeService gradeService;

    /**
     *
     * @param map
     * @return
     * @throws Exception
     */

    public JSONObject encryptParams(Map<String, Object> map) throws Exception {


        /** 로거 얻기 */
        Logger logger = LoggerFactory.getLogger("trans");

        /** 해쉬 및 aes256암호화 후 리턴 될 json */
        JSONObject rsp = new JSONObject();

        /** SHA256 해쉬 파라미터 */
        String mchtId       = StringUtil.isNull(map.get("mchtId"));
        String method       = StringUtil.isNull(map.get("method"));
        String mchtTrdNo    = StringUtil.isNull(map.get("mchtTrdNo"));
        String trdDt        = StringUtil.isNull(map.get("trdDt"));
        String trdTm        = StringUtil.isNull(map.get("trdTm"));
        String trdAmt       = StringUtil.isNull(map.get("plainTrdAmt"));

        /** AES256 암호화 파라미터 */
        HashMap<String,String> params = new HashMap<String, String>();
        params.put("trdAmt",            trdAmt);
        params.put("mchtCustNm",        StringUtil.isNull(map.get("plainMchtCustNm")));
        params.put("cphoneNo",          StringUtil.isNull(map.get("plainCphoneNo")));
        params.put("email",             StringUtil.isNull(map.get("plainEmail")));
        params.put("mchtCustId",        StringUtil.isNull(map.get("plainMchtCustId")));
        params.put("taxAmt",            StringUtil.isNull(map.get("plainTaxAmt")));
        params.put("vatAmt",            StringUtil.isNull(map.get("plainVatAmt")));
        params.put("taxFreeAmt",        StringUtil.isNull(map.get("plainTaxFreeAmt")));
        params.put("svcAmt",            StringUtil.isNull(map.get("plainSvcAmt")));
        params.put("clipCustNm",        StringUtil.isNull(map.get("plainClipCustNm")));
        params.put("clipCustCi",        StringUtil.isNull(map.get("plainClipCustCi")));
        params.put("clipCustPhoneNo",   StringUtil.isNull(map.get("plainClipCustPhoneNo")));




        /** 설정 정보 얻기 */
        // 라이센스키 조회
        PaymentMethodDto paymentMethodDto = new PaymentMethodDto();
        paymentMethodDto.setMethod(method);
        paymentMethodDto.setMchtId(mchtId);
        paymentMethodDto = paymentDaoSub.getPaymentMethodKey(paymentMethodDto);

        /** 설정 정보 저장 */
        String licenseKey = paymentMethodDto.getLicenseKey();
        String aesKey = paymentMethodDto.getAes256Key();


        /*============================================================================================================================================
         *  SHA256 해쉬 처리
         *조합 필드 : 상점아이디 + 결제수단 + 상점주문번호 + 요청일자 + 요청시간 + 거래금액(평문) + 라이센스키
         *============================================================================================================================================*/
        String hashPlain = String.format("%s%s%s%s%s%s%s", mchtId, method, mchtTrdNo, trdDt, trdTm, trdAmt, licenseKey);
        String hashCipher ="";
        /** SHA256 해쉬 처리 */
        try{
            hashCipher = EncryptUtil.digestSHA256(hashPlain);//해쉬 값
        }catch(Exception e){
            logger.error("["+mchtTrdNo+"][SHA256 HASHING] Hashing Fail! : " + e);
            throw e;
        }finally{
            logger.info("["+mchtTrdNo+"][SHA256 HASHING] Plain Text["+hashPlain+"] ---> Cipher Text["+hashCipher+"]");
            rsp.put("hashCipher", hashCipher); // sha256 해쉬 결과 저장
        }

        /*============================================================================================================================================
         *  AES256 암호화 처리(AES-256-ECB encrypt -> Base64 encoding)
         *============================================================================================================================================ */
        try{
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key   = entry.getKey();
                String value =  entry.getValue();

                String aesPlain = params.get(key);
                if( !("".equals(aesPlain))){
                    byte[] aesCipherRaw = EncryptUtil.aes256EncryptEcb(aesKey, aesPlain);
                    String aesCipher = EncryptUtil.encodeBase64(aesCipherRaw);

                    params.put(key, aesCipher);//암호화된 데이터로 세팅
                    logger.info("["+mchtTrdNo+"][AES256 Encrypt] "+key+"["+aesPlain+"] ---> ["+aesCipher+"]");
                }
            }

        }catch(Exception e){
            logger.error("["+mchtTrdNo+"][AES256 Encrypt] AES256 Fail! : " + e);
            throw e;
        }finally{
            JSONObject encParams = JSONObject.fromObject(params); //aes256 암호화 결과 저장
            rsp.put("encParams", encParams);
        }


        return rsp;
    }


    public String receiveNoti(HttpServletRequest request, PaymentNotiDto paymentNotiDto) {
        /** 이 페이지는 수정시 주의가 필요합니다. 수정시 html태그나 자바스크립트가 들어가는 경우 동작을 보장할 수 없습니다 */


        /** 노티 처리 결과 */
        boolean resp=false;

        /** 노티 수신 파라미터 */
        String outStatCd        = request.getParameter("outStatCd"      ) == null ? "" : request.getParameter("outStatCd");
        String trdNo            = request.getParameter("trdNo"          ) == null ? "" : request.getParameter("trdNo");
        String method           = request.getParameter("method"         ) == null ? "" : request.getParameter("method");
        String bizType          = request.getParameter("bizType"        ) == null ? "" : request.getParameter("bizType");
        String mchtId           = request.getParameter("mchtId"         ) == null ? "" : request.getParameter("mchtId");
        String mchtTrdNo        = request.getParameter("mchtTrdNo"      ) == null ? "" : request.getParameter("mchtTrdNo");
        String mchtCustNm       = request.getParameter("mchtCustNm"     ) == null ? "" : request.getParameter("mchtCustNm");
        String mchtName         = request.getParameter("mchtName"       ) == null ? "" : request.getParameter("mchtName");
        String pmtprdNm         = request.getParameter("pmtprdNm"       ) == null ? "" : request.getParameter("pmtprdNm");
        String trdDtm           = request.getParameter("trdDtm"         ) == null ? "" : request.getParameter("trdDtm");
        String trdAmt           = request.getParameter("trdAmt"         ) == null ? "" : request.getParameter("trdAmt");
        String billKey          = request.getParameter("billKey"        ) == null ? "" : request.getParameter("billKey");
        String billKeyExpireDt  = request.getParameter("billKeyExpireDt") == null ? "" : request.getParameter("billKeyExpireDt");
        String bankCd           = request.getParameter("bankCd"         ) == null ? "" : request.getParameter("bankCd");
        String bankNm           = request.getParameter("bankNm"         ) == null ? "" : request.getParameter("bankNm");
        String cardCd           = request.getParameter("cardCd"         ) == null ? "" : request.getParameter("cardCd");
        String cardNm           = request.getParameter("cardNm"         ) == null ? "" : request.getParameter("cardNm");
        String telecomCd        = request.getParameter("telecomCd"      ) == null ? "" : request.getParameter("telecomCd");
        String telecomNm        = request.getParameter("telecomNm"      ) == null ? "" : request.getParameter("telecomNm");
        String vAcntNo          = request.getParameter("vAcntNo"        ) == null ? "" : request.getParameter("vAcntNo");
        String expireDt         = request.getParameter("expireDt"       ) == null ? "" : request.getParameter("expireDt");
        String AcntPrintNm      = request.getParameter("AcntPrintNm"    ) == null ? "" : request.getParameter("AcntPrintNm");
        String dpstrNm          = request.getParameter("dpstrNm"        ) == null ? "" : request.getParameter("dpstrNm");
        String email            = request.getParameter("email"          ) == null ? "" : request.getParameter("email");
        String mchtCustId       = request.getParameter("mchtCustId"     ) == null ? "" : request.getParameter("mchtCustId");
        String cardNo           = request.getParameter("cardNo"         ) == null ? "" : request.getParameter("cardNo");
        String cardApprNo       = request.getParameter("cardApprNo"     ) == null ? "" : request.getParameter("cardApprNo");
        String instmtMon        = request.getParameter("instmtMon"      ) == null ? "" : request.getParameter("instmtMon");
        String instmtType       = request.getParameter("instmtType"     ) == null ? "" : request.getParameter("instmtType");
        String phoneNoEnc       = request.getParameter("phoneNoEnc"     ) == null ? "" : request.getParameter("phoneNoEnc");
        String orgTrdNo         = request.getParameter("orgTrdNo"       ) == null ? "" : request.getParameter("orgTrdNo");
        String orgTrdDt         = request.getParameter("orgTrdDt"       ) == null ? "" : request.getParameter("orgTrdDt");
        String mixTrdNo         = request.getParameter("mixTrdNo"       ) == null ? "" : request.getParameter("mixTrdNo");
        String mixTrdAmt        = request.getParameter("mixTrdAmt"      ) == null ? "" : request.getParameter("mixTrdAmt");
        String payAmt           = request.getParameter("payAmt"         ) == null ? "" : request.getParameter("payAmt");
        String csrcIssNo        = request.getParameter("csrcIssNo"      ) == null ? "" : request.getParameter("csrcIssNo");
        String cnclType         = request.getParameter("cnclType"       ) == null ? "" : request.getParameter("cnclType");
        String mchtParam        = request.getParameter("mchtParam"      ) == null ? "" : request.getParameter("mchtParam");
        String acntType         = request.getParameter("acntType"       ) == null ? "" : request.getParameter("acntType");
        String kkmAmt           = request.getParameter("kkmAmt"         ) == null ? "" : request.getParameter("kkmAmt");
        String coupAmt          = request.getParameter("coupAmt"        ) == null ? "" : request.getParameter("coupAmt");
        String pktHash          = request.getParameter("pktHash"        ) == null ? "" : request.getParameter("pktHash");
        String ezpDivCd         = request.getParameter("ezpDivCd"       ) == null ? "" : request.getParameter("ezpDivCd");

        /* TODO : parameter validation */

        /* 응답 파라미터 List에 저장 */
        ArrayList<String> noti = new ArrayList<String>();
        noti.add("거래상태:"+ outStatCd);
        noti.add("거래번호:"+ trdNo);
        noti.add("결제수단:"+ method);
        noti.add("간편결제코드:"+ ezpDivCd);
        noti.add("업무구분:"+ bizType);
        noti.add("상점아이디:"+ mchtId);
        noti.add("상점거래번호:"+ mchtTrdNo);
        noti.add("주문자명:"+ mchtCustNm);
        noti.add("상점한글명:"+ mchtName);
        noti.add("상품명:"+ pmtprdNm);
        noti.add("거래일시:"+ trdDtm);
        noti.add("거래금액:"+ trdAmt);
        noti.add("자동결제키:"+ billKey);
        noti.add("자동결제키 유효기간:"+ billKeyExpireDt);
        noti.add("은행코드:"+ bankCd);
        noti.add("은행명:"+ bankNm);
        noti.add("카드사코드:"+ cardCd);
        noti.add("카드명:"+ cardNm);
        noti.add("이통사코드:"+ telecomCd);
        noti.add("이통사명:"+ telecomNm);
        noti.add("가상계좌번호:"+ vAcntNo);
        noti.add("가상계좌 입금만료일시:"+ expireDt);
        noti.add("통장인자명:"+ AcntPrintNm);
        noti.add("입금자명:"+ dpstrNm);
        noti.add("고객이메일:"+ email);
        noti.add("상점고객아이디:"+ mchtCustId);
        noti.add("카드번호:"+ cardNo);
        noti.add("카드승인번호:"+ cardApprNo);
        noti.add("할부개월수:"+ instmtMon);
        noti.add("할부타입:"+ instmtType);
        noti.add("휴대폰번호(암호화):"+ phoneNoEnc);
        noti.add("원거래번호:"+ orgTrdNo);
        noti.add("원거래일자:"+ orgTrdDt);
        noti.add("복합결제 거래번호:"+ mixTrdNo);
        noti.add("복합결제 금액:"+ mixTrdAmt);
        noti.add("실결제금액:"+ payAmt);
        noti.add("현금영수증 승인번호:"+ csrcIssNo);
        noti.add("취소거래타입:"+ cnclType);
        noti.add("기타주문정보:"+ mchtParam);
        noti.add("계좌구분:"+ acntType);
        noti.add("카카오머니 금액:"+ kkmAmt);
        noti.add("쿠폰 금액:"+ coupAmt);
        noti.add("해쉬값:"+ pktHash); //서버에서 전달된 해쉬 값

        // log 확인
        super.pushAlarm(noti.toString(),"LJH");

        // 라이센스키 조회
        PaymentMethodDto paymentMethodDto = new PaymentMethodDto();
        paymentMethodDto.setMethodNoti(method);
        paymentMethodDto.setMchtId(mchtId);
        paymentMethodDto = paymentDaoSub.getPaymentMethodNoti(paymentMethodDto);

        /** 설정 정보 저장 */
        String licenseKey = paymentMethodDto.getLicenseKey();

        /** 해쉬 조합 필드
         *  결과코드 + 거래일시 + 상점아이디 + 가맹점거래번호 + 거래금액 + 라이센스키 */
        String hashPlain = String.format("%s%s%s%s%s%s", outStatCd, trdDtm, mchtId, mchtTrdNo, trdAmt, licenseKey);
        String hashCipher ="";

        /** SHA256 해쉬 처리 */
        try{
            hashCipher = EncryptUtil.digestSHA256(hashPlain);//해쉬 값
        }catch(Exception e){
            notiLogger.error("["+mchtTrdNo+"][SHA256 HASHING] Hashing Fail! : " + e);
        }finally{
            notiLogger.info("["+mchtTrdNo+"][SHA256 HASHING] Plain Text["+hashPlain+"] ---> Cipher Text["+hashCipher+"]");
        }

        /**
         hash데이타값이 맞는 지 확인 하는 루틴은 세틀뱅크에서 받은 데이타가 맞는지 확인하는 것이므로 꼭 사용하셔야 합니다
         정상적인 결제 건임에도 불구하고 노티 페이지의 오류나 네트웍 문제 등으로 인한 hash 값의 오류가 발생할 수도 있습니다.
         그러므로 hash 오류건에 대해서는 오류 발생시 원인을 파악하여 즉시 수정 및 대처해 주셔야 합니다.
         그리고 정상적으로 데이터를 처리한 경우에도 세틀뱅크에서 응답을 받지 못한 경우는 결제결과가 중복해서 나갈 수 있으므로 관련한 처리도 고려되어야 합니다
         */
        if (hashCipher.equals(pktHash)) {
            notiLogger.info("["+ mchtTrdNo + "][SHA256 Hash Check] hashCipher[" + hashCipher + "] pktHash[" + pktHash + "] equals?[TRUE]");
            if ("0021".equals(outStatCd)){
                // 결제 성공
                notiLogger.info("["+ mchtTrdNo + "][Success] params:" + String.join("|", noti));
                resp = notiSuccess(paymentNotiDto,noti);
            }
            else if ("0051".equals(outStatCd)){
                // 입금대기
                notiLogger.info("["+ mchtTrdNo + "][Wait For Deposit] params:" + String.join("|", noti));
                resp = notiWaitingPay(paymentNotiDto);
            }
            else{
                notiLogger.info("["+ mchtTrdNo + "][Undefined Code] outStatCd:"+ outStatCd );
                resp = false;
            }
        }
        else {
            // 해시키 오류
            notiLogger.info("["+ mchtTrdNo + "][SHA256 Hash Check] hashCipher[" + hashCipher + "] pktHash[" + pktHash + "] equals?[FALSE]");
            resp = notiHashError(paymentNotiDto,noti);
        }

        // OK, FAIL문자열은 세틀뱅크로 전송되어야 하는 값이므로 변경하거나 삭제하지마십시오.
        if (resp){
            notiLogger.info("["+ mchtTrdNo + "][Result] OK");
            return "OK";
        }
        else{
            notiLogger.info("["+ mchtTrdNo + "][Result] FAIL");
            return "FAIL";
        }
    }

    // 노티를 성공적으로 수신한 경우
    boolean notiSuccess(PaymentNotiDto paymentNotiDto, List<String> noti){
        /* 중복 지급 방지 추가 */
        PaymentDto paymentDuplication = new PaymentDto();
        paymentDuplication.setTid(paymentNotiDto.getTrdNo());
        PaymentDto paymentDuplicationRes = paymentDaoSub.getPayment(paymentDuplication);

        // 이미 입력 값있으면 중단
        if(paymentDuplicationRes != null){
            super.pushAlarm("중복입력" + noti.toString(),"LJH");
            return true;
        }

        // pay_log 입력
        paymentDao.insertPayLog(paymentNotiDto);
        // 취소 로직 처리
        if(paymentNotiDto.getBizType().equals("C0")){
            // TODO 결제 취소 로직 추가
            super.pushAlarm("결제취소 : " + noti,"LJH");
            return true;
        }

        org.json.JSONObject mchtParam = new org.json.JSONObject(paymentNotiDto.getMchtParam());
        //pushAlarm(mchtParam.toString(),"LJH");

        // 상품정보 조회
        ProductDto productDto = new ProductDto();
        productDto.setIdx(mchtParam.getInt("pid"));
        ProductDto ProductData =  productService.getProduct(productDto);

        // 금액이 다르면 중단
        if(!ProductData.getPrice().equals(paymentNotiDto.getTrdAmt())){
            super.pushAlarm("결제 금액에러 : " + noti,"LJH");
            return false;
        }
        
        // payment 입력 정보
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setProductIdx(mchtParam.getInt("pid"));    // 상품번호
        paymentDto.setMemberIdx(mchtParam.getLong("midx"));   // 회원번호
        paymentDto.setCoin(ProductData.getCoin());                 //코인
        paymentDto.setCoinFree(ProductData.getCoinFree() + ProductData.getCoinFree2()); // 보너스코인
        paymentDto.setMileage(ProductData.getMileage());           // 마일리지
        paymentDto.setPay(paymentNotiDto.getTrdAmt());             // 결제금액
        paymentDto.setPayType(paymentNotiDto.getMethod());         // 결제수단
        paymentDto.setPayMethod(paymentNotiDto.getEzpDivCd());     // 간편결제 코드
        paymentDto.setCpId(paymentNotiDto.getMchtId());            // 상점코드
        paymentDto.setOrderNo(paymentNotiDto.getMchtTrdNo());      // 주문번호
        paymentDto.setTid(paymentNotiDto.getTrdNo());              // 거래번호
        paymentDto.setRegdate(dateLibrary.getDatetime());          // 등록일

        //pushAlarm(paymentDto.getMemberIdx().toString(),"LJH");

        // 결제 횟수 조회(첫 결제 여부)
        int paymentCnt  = paymentDaoSub.getPaymentCnt(paymentDto.getMemberIdx());

        // 결제 테이블 입력
        paymentDao.insertPayment(paymentDto);

        // paymentInfo 정보
        PaymentDto paymentInfo = new PaymentDto();
        paymentInfo.setIdx( paymentDto.getInsertedId().longValue());
        paymentInfo.setFirst(0);
        if (paymentCnt < 1) {
            paymentInfo.setFirst(1); // 첫 결제 set
        }
        paymentInfo.setRegdate(dateLibrary.getDatetime());

        /** payment_info(결제) 테이블에 등록 **/
        paymentDao.insertPaymentInfo(paymentInfo);

        /** 코인 & 마일리지 지급 **/
        int paymentMileage = 0;
        if (ProductData.getMileage() > 0) { // 결제한 상품에 마일리지가 지급되는 경우에만

            // 회원 등급 정보 조회
            Long memberIdx = mchtParam.getLong("midx");
            Integer memberGrade = gradeService.getMemberGradeLevel(memberIdx);

            // 충전 시 추가 지급되는 마일리지 계산
            paymentMileage = getPaymentMileage(ProductData.getMileage(), memberGrade);
        }

        // 회원 등급 등록 (다음 결제 부터 등급 올라감)
        gradeService.insertGrade(paymentDto.getMemberIdx());

        CoinDto coinDto = new CoinDto();
        coinDto.setProductIdx(mchtParam.getInt("pid"));             // 상품번호
        coinDto.setMemberIdx(mchtParam.getLong("midx"));            // 회원번호
        coinDto.setTitle(ProductData.getTitle());                       // 상품제목
        coinDto.setMileage(ProductData.getMileage() + paymentMileage);  // 마일리지 지급
        coinDto.setCoin(paymentDto.getCoin());                          // 유료코인 지급
        coinDto.setCoinFree(paymentDto.getCoinFree());                  // 무료코인 지급
        coinDto.setPaymentIdx(paymentDto.getInsertedId());              // payment.idx
        coinDto.setPosition("결제");
        coinService.coinPayment(coinDto);

        /** member_notification 테이블에 등록 : 회원 알림 전송 **/
        // dto set
        NotificationDto dto = NotificationDto.builder()
                .memberIdx(paymentDto.getMemberIdx()) // 회원 idx
                .category(CHARGE) // 알림 카테고리
                .type("payment") // 알림 보낼 테이블명
                .typeIdx(paymentDto.getInsertedId().longValue()) // 알림 보낼 테이블 idx
                .state(1)
                .regdate(dateLibrary.getDatetime())
                .build();

        // 코인 & 마일리지 충전 완료 알림 전송
        notificationDao.insertChargeNotification(dto);

        try {
            if(mchtParam.has("adId") && mchtParam.has("simOperator") && mchtParam.has("installer") ) {
                // 원스토어 토큰 생성
                MultiValueMap<String, String> edata = new LinkedMultiValueMap<>();
                edata.add("client_secret", "NgLOb3GsVZsh9+7mtd9vjgVX8B+NDe7huBw8i0dKrpY=");
                edata.add("client_id", "com.uxplusstudio.ggultoon");
                edata.add("grant_type", "client_credentials");
                String token = CurlLibrary.post("https://apis.onestore.co.kr/v2/oauth/token", edata);

                JSONObject json = JSONObject.fromObject(token);

                // 구매 상품 정보
                Map<Object, Object> product = new HashMap();
                product.put("developerProductId", mchtParam.getInt("pid"));
                product.put("developerProductName", ProductData.getTitle());
                product.put("developerProductPrice", paymentNotiDto.getTrdAmt());
                product.put("developerProductQty", 1);

                List<Map<Object, Object>> developerProductList = new ArrayList<>();
                developerProductList.add(product);

                // 결제 정보
                Map<Object, Object> purchaseMethod = new HashMap();
                purchaseMethod.put("purchaseMethodCd", payType(paymentDto)); // 결제 수단
                purchaseMethod.put("purchasePrice", paymentNotiDto.getTrdAmt()); //
                List<Map<Object, Object>> purchaseMethodList = new ArrayList<>();
                purchaseMethodList.add(purchaseMethod);

                org.json.JSONObject sendData = new org.json.JSONObject();
                sendData.put("developerProductList", developerProductList);
                sendData.put("purchaseMethodList", purchaseMethodList);
                sendData.put("adId", mchtParam.getString("adId"));
                sendData.put("developerOrderId", paymentNotiDto.getMchtTrdNo());
                sendData.put("simOperator", mchtParam.getString("simOperator"));
                sendData.put("installerPackageName", mchtParam.getString("installer"));
                sendData.put("developerProductName", ProductData.getTitle());
                sendData.put("totalPrice", paymentNotiDto.getTrdAmt());
                sendData.put("purchaseTime", System.currentTimeMillis());

                String res = CurlLibrary.post("https://apis.onestore.co.kr/v2/purchase/developer/com.uxplusstudio.ggultoon/send", sendData.toString(), json.getString("access_token"));

                /** payment_app(앱결제정보) 테이블에 등록 **/
                PaymentAppDto paymentAppDto = PaymentAppDto.builder()
                        .memberIdx(mchtParam.getLong("midx"))
                        .adId(mchtParam.getString("adId"))
                        .simOperator(mchtParam.getString("simOperator"))
                        .installerPackageName(mchtParam.getString("installer"))
                        .paymentIdx(paymentDto.getInsertedId())
                        .returnMsg(res)
                        .regdate(dateLibrary.getDatetime())
                        .build();
                paymentDao.insertPaymentApp(paymentAppDto);
                super.pushAlarm("원스토어 전송 : " + sendData.toString(), "LJH");
                super.pushAlarm("원스토어 결과 : " + JSONObject.fromObject(res).toString(), "LJH");
            }
        } catch (Exception e) {
            super.pushAlarm("원스토어 오류 : " + e.getMessage(),"LJH");

        }
        return true;
    }

    /** 입금대기시 */
    boolean notiWaitingPay(PaymentNotiDto paymentNotiDto){
        /* TODO : 관련 로직 추가 */
        paymentDao.insertPayLog(paymentNotiDto);

        return true;
    }

    /** 노티 수신중 해시 체크 에러가 생긴 경우 */
    boolean notiHashError(PaymentNotiDto paymentNotiDto,List<String> noti){
        /* TODO : 관련 로직 추가 */
        // log 확인
        super.pushAlarm("결제 해시체크에러 : " + noti,"LJH");
        paymentDao.insertPayLog(paymentNotiDto);

        return false;
    }

    /**
     * 결제수단 문자변환 dto(결제)
     *
     * @param payment
     */
    private String payType(PaymentDto payment) {
        Map<String, String> map = new HashMap<>() {{
            put("CA", "TRD_CREDITCARD");    // 신용카드
            put("RA", "TRD_BANKTRANSFER");  // 계좌이체
            put("VA", "TRD_BANKTRANSFER");  // 가상계좌
            put("MP", "TRD_MOBILEBILLING"); // 휴대전화
            put("HM", "TRD_HAPPYMONEY");    // 해피머니
            put("CG", "TRD_CULTURELAND");   // 컬쳐랜드
            put("BG", "TRD_BOOKNLIFE");     // 도서상품권
            put("TM", "TRD_TMONEY");        // 티머니
            put("NVP", "TRD_NAVERPAY");     // 네이버페이
            put("KKP", "TRD_KAKAOPAY");     // 카카오페이
            put("TC", "TRD_PURCHASE_ETC");  // 틴캐시
            put("CP", "TRD_PURCHASE_ETC");  // 포인트 다모아
            put("SG", "TRD_PURCHASE_ETC");  // 스마트문상
        }};
        if(Objects.equals(payment.getPayType(), "PZ")) {
            return map.get(payment.getPayMethod());
        }else{
            return map.get(payment.getPayType());
        }
    }
}
