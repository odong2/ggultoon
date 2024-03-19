package com.architecture.admin.models.dto.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDto {
    /**
     * member
     **/
    private Long idx;               // 회원번호
    private String id;              // 아이디
    private String site;            // 사이트
    private String password;        //비밀번호
    private String passwordConfirm; // 패스워드 확인
    private Integer auth;           // 본인인증(0:미인증, 1:인증)
    private String authText;        // 본인인증 문자변환
    private Integer adult;          // 성인인증(0:비성인, 1:성인)
    private String adultText;       // 성인인증 문자변환
    private String joinIp;          // 가입 아이피
    private String joinType;        // 가입 구분 app,pc,mobile
    private String loginType;       // 로그인 구분 app,pc,mobile
    private Integer isSimple;       // 간편가입
    private Integer isBenefit;      // 회원 탈퇴관련 특별혜택 유무(0: 받은적 없음, 1: 받은적 있음)
    private String benefitText;     // 지급(받은적 있음),미지급(받은적 없음)
    private String logindate;       // 로그인시간
    private String logindateTz;     // 로그인시간 타임존
    private String regdate;         // 등록일
    private String regdateTz;       // 등록일 타임존
    private String modifyDate;      // 비밀번호 변경일
    private String modifyDateTz;    // 비밀번호 변경일 타임존

    /**
     * member_info
     **/

    private Long memberIdx;         // 회원번호
    @Email
    private String email;           // 이메일
    private String txseq;           // 본인인증거래번호
    private String phone;           // 전화번호
    private String com;             // 통신사
    private String gender;          // 성별(M: male, F: female)
    private String genderText;      // 성별 문자변환
    private String lang;            // 사용언어
    private String birth;           // 생년월일
    private Integer state;          // 상태값
    private String stateText;       // 상태값 문자변환
    private String stateBg;         // 상태 bg 색상
    private String ci;              // 개인 식별 고유값
    private String di;              // 업체 중복가입 확인값
    private String name;            // 이름
    private String ip;              // 아이피

    /**
     * member_nick
     **/
    // 닉네임
    private String nick;            // 닉네임

    /**
     * member_simple
     **/
    private String simpleType;
    private String authToken;

    /**
     * member_setting
     */
    private Integer coinAlarm;  // 코인 차감 안내

    /**
     * 비밀번호 변경
     */
    private String newPassword;         // 변경할 비밀번호
    private String newPasswordConfirm;  // 변경할 비밀번호 확인
    /**
     * 약관동의
     **/
    @Pattern(regexp = "[Y]", message = "{lang.login.exception.privacy}")
    private String privacy;
    @Pattern(regexp = "[Y]", message = "{lang.login.exception.txseq}")
    private String age;
    private String marketing;

    /**
     * 회원 탈퇴
     */
    private String outdate;     //탈퇴일
    private String outdateTz;   //탈퇴일 타임존

    /**
     * 자동로그인정보
     */
    private String auto;      // 자동로그인정보

    /**
     * member_notification
     **/
    private Integer unreadNotiCnt; // 읽지 않은 알림 개수

    /**
     * 연동 데이터
     */
    private String edata;     // OTT 회원연동 데이터
    private String eventType; // 이벤트 타입

    /**
     * 기타
     */
    private String startDate; // 시작일
    private String endDate;   // 종료일

    // sql
    private Long insertedIdx;
    private Integer affectedRow;
}