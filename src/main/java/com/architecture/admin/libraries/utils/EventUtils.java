package com.architecture.admin.libraries.utils;

import lombok.SneakyThrows;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * 이벤트 날짜 상수 클래스
 */
public abstract class EventUtils {

    private EventUtils() {
    }
    /** 서비스 종료 -> 코인 구매 불가 시작 시간 변수 (Asia/Seoul 타임존 기준) **/
    public static final String COIN_PURCHASE_DISABLED = "2024-02-29 10:00:00";

    /** 2024 설연휴 전작품 무료 감상 이벤트 상태 변수 **/
    public static final Boolean EVENT_STATE = true; // 이벤트 중지 시 false로 변경

    /** 이벤트 시작일 & 종료일 변수 (Asia/Seoul 타임존 기준) **/

    // 최초 로그인 마일리지 지급 이벤트 기간
    public static final String START_FIRST_LOGIN = "2023-08-24 00:00:00";
    public static final String END_FIRST_LOGIN = "2024-12-31 23:59:59";

    // 1일 1회 로그인 마일리지 지급 이벤트 기간
    public static final String START_DAILY_LOGIN = "2023-08-24 10:00:00";
    public static final String END_DAILY_LOGIN = "2024-12-31 09:59:59";

    // 전작품 무료 감상 이벤트 기간
    public static final String START_FREE_VIEW = "2024-02-05 00:00:00";
    public static final String END_FREE_VIEW = "2024-02-13 11:00:00";

    /**
     * 로그인 마일리지 이벤트 사용 기한 계산
     *
     * @return 시작일 : 조회 날짜 기준 당일 오전 10시
     * @return 종료일 : 조회 날짜 기준 익일 오전 9시 59분
     */
    public static HashMap<String, String> getEventStartAndEndDate() {

        HashMap<String, String> map = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        Date startDate;
        Date endDate;
        String stringStartDate;
        String stringEndDate;

        // 시작일
        SimpleDateFormat startFormatDate = new SimpleDateFormat("yyyy-MM-dd 01:00:00"); // 로그인 시점 기준 당일 오전 10시 -> UTC 변환
        startFormatDate.setTimeZone(utcZone);

        // 현재 시간이 오전 9시 ~ 오전 9시 59분 59초인 경우
        if (cal.get(Calendar.HOUR_OF_DAY) == 9) {
            cal.add(Calendar.DATE, -1); // 어제
        }

        startDate = cal.getTime();
        stringStartDate = startFormatDate.format(startDate);
        map.put("start", stringStartDate);

        // 종료일
        SimpleDateFormat endFormatDate = new SimpleDateFormat("yyyy-MM-dd 00:59:59"); // 로그인 시점 기준 익일 오전 9시 59분 -> UTC 변환
        endFormatDate.setTimeZone(utcZone);
        cal.add(Calendar.DATE, +1); // 익일
        endDate = cal.getTime();
        stringEndDate = endFormatDate.format(endDate);
        map.put("end", stringEndDate);

        return map;
    }

    /**
     * 로그인 마일리지 이벤트로 지급되는 마일리지 만료일 계산
     * 지급 날짜 기준 당일 오전 10시 ~ 익일 오전 9시 59분(24시간)까지만 사용 가능
     */
    public static String getLoginMileageExpireDate() {

        // 날짜 포맷(UTC 타임존 기준)
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd 00:59:59");

        // 현재 날짜 및 시간(로그인한 시점)
        Calendar cal = Calendar.getInstance();

        // 만료일(기본값 -> 당일 오전 9시 59분까지)
        Date expireDate = cal.getTime();
        String stringExpireDate = formatDate.format(expireDate);

        /** 만료일 계산 공식
         * (사용 가능 기간의 시작 시간) - (현재 시간(24시간제))
         *
         * 결과값이 음수일 때 : 만료일은 "익일" 오전 9시 59분까지
         * 결과값이 양수일 때 : 만료일은 "당일" 오전 9시 59분까지
         **/

        // 사용 가능 기간 시작 시간(정책 = 오전 10시)
        int startHour = 10;

        // 결과값이 0 또는 음수일 때 -> 만료일 = "익일" 오전 9시 59분까지
        if (startHour - cal.get(Calendar.HOUR_OF_DAY) <= 0) {

            // 익일로 변경
            cal.add(Calendar.DATE, +1);
            expireDate = cal.getTime();
            stringExpireDate = formatDate.format(expireDate);
        }

        return stringExpireDate;
    }

    /**
     * 최초 로그인 시 "1일 1회 로그인 마일리지" 지급 일자 계산
     * 팝업 안내 문구 노출용
     * 
     * ex. 24일 오전 1시 가입 -> 24일 오전 10시 지급
     * ex. 24일 오후 10시 가입 -> 25일 오전 10시 지급
     * 
     * @param inputDate : 가입일 (UTC 타임존 기준)
     * @return regdate : 지급일
     */
    @SneakyThrows
    public static String getLoginMileageRegDate(String inputDate) {

        // 날짜 포맷
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 기본
        SimpleDateFormat formatDay = new SimpleDateFormat("yyyy-MM-dd 10:00:00"); // 날짜
        SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss"); // 시간
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatTime.setTimeZone(utcZone);

        // 시간 비교용 변수 세팅(UTC 기준)
        Date startTime = formatTime.parse("01:00:00"); // 오전 10시 00분
        Date endTime = formatTime.parse("14:59:59"); // 오후 11시 59분

        // 가입일 일자 변환
        Date regDate = formatDate.parse(inputDate); // 기본
        int index = inputDate.indexOf(" ");
        String inputTime = inputDate.substring(index + 1); // 시간만 추출
        Date regTime = formatTime.parse(inputTime); // 시간

        // 지급일 = 익일 오전 10시(가입일이 오전 10시 ~ 오후 11시 59분 사이)
        if (startTime.compareTo(regTime) <= 0 && endTime.compareTo(regTime) >= 0) {

            // 익일로 세팅
            Calendar cal = Calendar.getInstance();
            cal.setTime(regDate); // 시간 설정
            cal.add(Calendar.DATE, 1); // +1일 연산
            regDate = cal.getTime();
        }

        // 지급일 세팅 --> 기본값 = 당일 오전 10시(가입일이 오전 00시 ~ 오전 09시 59분 사이)
        String regdate = formatDay.format(regDate);
        return regdate;
    }
}
