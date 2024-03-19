package com.architecture.admin.models.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentAppDto {
    private Long idx;                    // 고유번호
    private Long memberIdx;              // 회원번호
    private Integer paymentIdx;          // 결제번호
    private String adId;
    private String simOperator;
    private String installerPackageName;
    private Integer state;               // 상태
    private String sendMsg;              // 보낸메세지
    private String returnMsg;            // 결과메세지
    private String regdate;              // 등록일
    private String regdateTz;            // 등록일 타임존

    private Integer insertedId;
}
