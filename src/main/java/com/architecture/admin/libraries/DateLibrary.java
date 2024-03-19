package com.architecture.admin.libraries;

import com.architecture.admin.models.dto.SearchDto;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/*****************************************************
 * 시간 라이브러리
 ****************************************************/

/*****************************************************************************
 * <남은 시간 표시 정책 - 대여 후 잔여 사용 가능 기간 등>
 * "00일 00시간 남음"으로 표시
 * 남은 시간이 1시간 미만일 경우에는 "00분 남음"으로 표시
 *
 * <지난 시간 표시 정책 - 게시글 및 댓글 작성일 등>
 * 당일에 작성된 게시글/댓글은 "1분 전" ~ "24시간 전"으로 표시
 * 작성 직후 ~ 1분 경과 시까지 모두 "1분 전"으로 통일 (방금 전, 00초 전 사용 안함)
 * 작성 후 24시간이 경과한 뒤부터는 정확한 날짜 표시 ex. 4월 26일
 * 올해를 기준으로 작성 년도가 지난 댓글은 연도 표시(24년부터 적용) ex. 22년 4월 26일
 *****************************************************************************/
@Component
@Data
public class DateLibrary {
    public static final int ONE_MIN = 60;
    public static final int ONE_HOUR = 3600;
    public static final int ONE_DAY = 86400;
    public static final int ONE_WEEK = 604800;
    public static final int ONE_MONTH = 2592000;
    public static final int ONE_YEAR = 31104000;

    /**
     * date 형식 시간 구하기
     *
     * @return UTC 기준 시간 yyyy-MM-dd hh:mm:ss
     */
    public static String getDatetime() {
        java.util.Date dateNow = new java.util.Date(System.currentTimeMillis());

        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(dateNow);
    }

    /**
     * 로컬시간을 Asia/Seoul 시간으로 변경
     *
     * @return Asia/Seoul 기준 시간 yyyy-MM-dd hh:mm:ss
     */
    public static String getDatetimeToSeoul() {
        java.util.Date dateNow = new java.util.Date(System.currentTimeMillis());

        // 타임존 Asia/Seoul 기준
        TimeZone seoulZone = TimeZone.getTimeZone("Asia/Seoul");
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(seoulZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(dateNow);
    }

    /**
     * 로컬시간을 UTC 시간으로 변경
     *
     * @param date 로컬 시간 yyyy-MM-dd hh:mm:ss
     * @return UTC 기준 시간 yyyy-MM-dd hh:mm:ss
     */
    public String localTimeToUtc(String date) {
        // 타임존 UTC 기준값
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(utcZone);
        Timestamp timestamp = Timestamp.valueOf(date);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(timestamp);
    }


    /**
     * UTC 시간을 로컬시간으로 변경
     *
     * @param date UTC 시간 yyyy-MM-dd hh:mm:ss
     * @return 로컬 시간 yyyy-MM-dd hh:mm:ss
     */
    public String utcToLocalTime(String date) {
        // 입력시간을 Timestamp 변환
        long utcTime = Timestamp.valueOf(date).getTime();
        TimeZone z = TimeZone.getDefault();
        int offset = z.getOffset(utcTime); // getRawOffset는 썸머타임 반영 문제로 getOffset 사용
        // Timestamp 변환시 UTC 기준으로 로컬타임과 차이 발생하여 2회 적용
        long localDateTime = utcTime + (offset * 2);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatDatetime.format(new Timestamp(localDateTime));
    }

    /**
     * timestamp 형식 시간 구하기
     */
    public String getTimestamp() {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        return String.valueOf(time.getTime() / 1000L);
    }

    /**
     * yyyy-MM-dd 포맷으로 날짜 변경
     */
    public String getDay(String inputDate) {

        int index = inputDate.indexOf(" ");
        String returnDate = inputDate.substring(0 , index);
        return returnDate;
    }

    /**
     * yyyy년 MM월 dd일 포맷으로 날짜 변경
     *
     * @return Asia/Seoul 기준 시간 yyyy-MM-dd
     */
    @SneakyThrows
    public String formatDay(String inputDate) {
        SimpleDateFormat IncludeYearDatetime = new SimpleDateFormat("yy년 M월 d일");
        // String -> Date 변환
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = formatDatetime.parse(inputDate);
        
        // Date -> String 변환
        String formatDate = IncludeYearDatetime.format(date);

        return formatDate;
    }

    /**
     * 입력 받은 날짜가 기준일을 안지났는지 체크하는 메서드
     *
     * @param inputDate   : 입력 받은 날짜
     * @param standardDay : 기준 일 ex) ONE_DAY, ONE_MONTH
     * @return
     */
    @SneakyThrows
    public boolean isNotAfterDate(String inputDate, int standardDay) {
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean result = true; // true 는 한달이 안 지남

        // 타임존 Asia/Seoul 기준
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 파라미터로 전달받은 날짜 String -> Date 변환
        Date formatDate = formatDatetime.parse(inputDate);

        // 현재 시간 - inputDate 시간
        long milliseconds = formatNowDate.getTime() - formatDate.getTime();
        long second = TimeUnit.SECONDS.convert(milliseconds, TimeUnit.MILLISECONDS);

        /** 하루 안지났는지 체크 **/
        if (standardDay == ONE_DAY) {
            // inputDate 가 하루 지남
            if (second > ONE_DAY) {
                result = false;
            }
            
        /** 한달 안지났는지 체크 **/
        } else if (standardDay == ONE_MONTH) {
            // inputDate 가 한달 지남
            if (second > ONE_MONTH) {
                result = false;
            }
        }
        return result;
    }

    /**
     * 이벤트 시작일과 종료일을 입력받아 현재 이벤트 진행 상태를 계산하는 메서드
     *
     * @param startDate   : 이벤트 시작일
     * @param endDate     : 이벤트 종료일
     * @return
     */
    @SneakyThrows
    public boolean checkEventState(String startDate, String endDate) {

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // return value (기본값 : 이벤트 진행중 아님)
        boolean result = false;

        // 타임존 Asia/Seoul 기준 현재 날짜 구하기
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 이벤트 시작일 String -> Date 변환
        Date formatStartDate = formatDatetime.parse(startDate);

        // 이벤트 종료일 String -> Date 변환
        Date formatEndDate = formatDatetime.parse(endDate);

        // 이벤트 시작일보다 종료일이 늦는 경우에 한해
        if (formatStartDate.compareTo(formatEndDate) < 0) {

            // 현재 날짜를 기준으로 이벤트가 진행 중인 경우
            if (formatStartDate.compareTo(formatNowDate) < 0 && formatEndDate.compareTo(formatNowDate) > 0) {
                result = true; // 이벤트 진행중
            }
        }
        return result;
    }

    /**
     * 현재 시간을 기준으로 지난 시간 계산
     *
     * @param inputDate : 게시글 등록일, 댓글 등록일, 알림 등록일
     * @return
     */
    @SneakyThrows
    public static String getConvertRegdate(String inputDate) {
        
        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat ExcludeYearDatetime = new SimpleDateFormat("M월 d일");
        SimpleDateFormat IncludeYearDatetime = new SimpleDateFormat("yy년 M월 d일");
        // 타임존 Asia/Seoul 기준 현재 날짜 구하기
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // inputDate String -> Date 변환
        Date formatInputDate = formatDatetime.parse(inputDate);

        // 비교 날짜와 현재 날짜 크기 비교
        int result = formatInputDate.compareTo(formatNowDate);

        // return value
        String calculatedTime = "";

        /** 입력 받은 날짜가 현재 날짜와 같거나 이전일 경우에만 계산 **/
        if (result <= 0) {
            // 시간 간격을 초 단위로 변환
            long differenceInMillis = formatNowDate.getTime() - formatInputDate.getTime();
            long second = TimeUnit.SECONDS.convert(differenceInMillis, TimeUnit.MILLISECONDS);

            // 현재 시간 대비 지난 시간이 얼마인지 계산
            if (0 <= second && second < ONE_HOUR) { // 등록 직후 ~ 1시간 경과 전
                if (second < ONE_MIN) {
                    calculatedTime = "1분 전";
                } else {
                    calculatedTime = second / ONE_MIN + "분 전";
                }
            } else if (ONE_HOUR <= second && second < ONE_DAY) { // 1시간 후 ~ 24시간 경과 전
                calculatedTime = second / ONE_HOUR + "시간 전";

            } else if (ONE_DAY <= second && second < ONE_YEAR) { // 24시간 경과 후 ~ 1년 경과 전
                calculatedTime = ExcludeYearDatetime.format(formatInputDate);

            } else if (ONE_YEAR <= second) { // 1년 경과 직후 ~
                calculatedTime = IncludeYearDatetime.format(formatInputDate);
            }
        }
        return calculatedTime;
    }

    /**
     * 현재 시간을 기준으로 남은 시간 계산
     * 대여 후 잔여 사용 가능 시간 표시
     *
     * @param inputDate : 남은 시간을 계산할 날짜 - (현재 시간 기준) 대여 만료일과 비교
     * @return
     */
    @SneakyThrows
    public static String getConvertRentExpiredate(String inputDate) {
        
        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 타임존 Asia/Seoul 기준 현재 날짜 구하기
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 회차 구매 날짜 String -> Date 변환
        Date formatDate = formatDatetime.parse(inputDate);

        // return value
        String calculatedTime = "";

        // 시간 간격을 초 단위로 변환
        long differenceInMillis = formatDate.getTime() - formatNowDate.getTime();
        long second = TimeUnit.SECONDS.convert(differenceInMillis, TimeUnit.MILLISECONDS);

        // 현재 시간 대비 남은 시간이 얼마인지 계산
        if (second <= 0) { // 남은 시간이 없음
            calculatedTime = "대여 기간 만료";

        } else if (0 < second && second < ONE_HOUR) { // 남은 시간이 1시간 미만
            calculatedTime = second / ONE_MIN + "분 남음";

        } else if (ONE_HOUR <= second && second < ONE_DAY) { // 남은 시간이 1시간 이상 ~ 24시간 미만
            calculatedTime = second / ONE_HOUR + "시간 남음";

        } else if (ONE_DAY <= second) { // 남은 시간이 24시간 초과
            if ((second - (second / ONE_DAY * ONE_DAY)) < ONE_HOUR) {
                calculatedTime = second / ONE_DAY + "일 남음";
            } else {
                calculatedTime = second / ONE_DAY + "일" + " " + (second - (second / ONE_DAY * ONE_DAY)) / ONE_HOUR + "시간 남음";
            }
        }
        return calculatedTime;
    }

    /**
     * 24시간 후 만료일 계산
     * 개별 회차 구매 만료일 - 구매일로부터 24시간 후
     *
     * @param inputDate : 구매일
     * @return
     */
    @SneakyThrows
    public static String getExpireDate(String inputDate) {

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 계산 기준 일자 String -> Date 변환
        Date formatDate = formatDatetime.parse(inputDate);

        Calendar cal = Calendar.getInstance();
        cal.setTime(formatDate); // 시간 설정
        cal.add(Calendar.DATE, 1); // +1일 연산

        // Calendar -> String 변환
        String expireDate = formatDatetime.format(cal.getTime());
        return expireDate;
    }

    /**
     * 기준 날짜가 현재 날짜와 동일한지 계산
     *
     * @param inputDate : 기준 날짜 (UTC 타임존 기준)
     * @return
     */
    @SneakyThrows
    public static Boolean checkIsSameDay(String inputDate) {

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd"); // 날짜 비교용

        // 타임존 UTC 기준 현재 날짜 구하기
        String nowDate = getDatetime();
        Date parseNowDate = formatDatetime.parse(nowDate);
        String formatNowDate = formatDatetime.format(parseNowDate);

        Date parseDate = formatDatetime.parse(inputDate);
        String formatDate = formatDatetime.format(parseDate);

        // 기준 날짜와 현재 날짜가 같은 날인지 체크
        if (formatNowDate.equals(formatDate)) {
            return true; // 같은 날
        }
        return false; // 다른 날
    }

    /**
     * 시작일 및 종료일 시분초 세팅 (UTC)
     * @param searchDto : 날짜 타입(오늘 / 내일)
     * @param type : 시분초 세팅 (start - 00시 00분 00초 / end - 23시 59분 59초)
     * @return
     */
    @SneakyThrows
    public static String setDateTime(SearchDto searchDto, String type) {

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 시분초 포맷
        SimpleDateFormat formatTime = null;

        if (type == "start") {
            formatTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00");

        } else if (type == "end") {
            formatTime = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        }
        formatTime.setTimeZone(utcZone);

        // 타임존 UTC 기준 현재 날짜 구하기
        String nowDate = getDatetime();
        Date parseNowDate = formatDatetime.parse(nowDate);

        // 시간 설정
        Calendar cal = Calendar.getInstance();
        cal.setTime(parseNowDate); 
        
        // 시분초 세팅할 날짜가 내일인 경우
        if (searchDto.getSearchDateType() == "tomorrow") {
            // 현재 날짜에서 +1일 연산
            cal.add(Calendar.DATE, 1);
        }

        // Calendar -> String 변환
        String formatDate = formatTime.format(cal.getTime());
        return formatDate;
    }

    /**
     * 선물함
     * 현재 날짜 기준으로 입력 받은 유효 시간을 더한 날짜(만료일) 계산
     *
     * @param availableTime : 유효 시간
     * @return
     */
    @SneakyThrows
    public static String getTicketExpireDate(Integer availableTime) {

        // return value
        String expireDate = null;
        
        // 유효 시간이 있을 때만 계산
        if (availableTime != null && availableTime > 0) {

            // 타임존 UTC 기준 현재 날짜 구하기
            String nowDate = getDatetime();

            // 현재 날짜 String -> Date 변환
            SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date formatNowDate = formatDatetime.parse(nowDate);

            // 현재 날짜에서 입력 받은 유효시간만큼 더하기
            Calendar cal = Calendar.getInstance();
            cal.setTime(formatNowDate); // 시간 설정
            cal.add(Calendar.HOUR, availableTime);

            // Calendar -> String 변환
            expireDate = formatDatetime.format(cal.getTime());
        }
        return expireDate;
    }

    /**
     * 선물함
     * 오늘 받을 수 있는 선물 -> 사용 종료까지 남은 시간 표시(종료일 기준) ex. 1일 3시간 남음
     * 내일 받을 수 있는 선물 -> 지급 시작까지 남은 시간 표시(시작일 기준) ex. 9시간 후 선물
     *
     * @param searchDto : 지급 가능 일자(오늘 또는 내일)
     * @param giftDate : 사용 가능 기간의 시작일 또는 종료일
     * @return
     */
    @SneakyThrows
    public String convertGiftDate(SearchDto searchDto, String giftDate) {

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 타임존 Asia/Seoul 기준 현재 날짜 구하기
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 선물 사용 가능 기간 날짜 String -> Date 변환
        Date formatGiftDate = formatDatetime.parse(giftDate);

        // return value
        String calculatedTime = "";

        // 시간 간격을 초 단위로 변환
        long differenceInMillis = formatGiftDate.getTime() - formatNowDate.getTime();
        long second = TimeUnit.SECONDS.convert(differenceInMillis, TimeUnit.MILLISECONDS);
        String searchType = searchDto.getSearchDateType();

        // 현재 시간 대비 남은 시간이 얼마인지 계산
        if (second <= 0) { // 남은 시간이 없음
            calculatedTime = "사용 불가";

        } else if (0 < second && second < ONE_HOUR) { // 남은 시간이 1시간 미만
            if (searchType.equals("today")) {
                calculatedTime = second / ONE_MIN + "분 남음";

            } else if (searchType.equals("tomorrow")) {
                calculatedTime = second / ONE_MIN + "분 후 선물";
            }

        } else if (ONE_HOUR <= second && second < ONE_DAY) { // 남은 시간이 1시간 이상 ~ 24시간 미만
            if (searchType.equals("today")) {
                calculatedTime = second / ONE_HOUR + "시간 남음";

            } else if (searchType.equals("tomorrow")) {
                calculatedTime = second / ONE_HOUR + "시간 후 선물";
            }

        } else if (ONE_DAY <= second) { // 남은 시간이 24시간 초과
            if ((second - (second / ONE_DAY * ONE_DAY)) < ONE_HOUR) {
                if (searchType.equals("today")) {
                    calculatedTime = second / ONE_DAY + "일 남음";

                } else if (searchType.equals("tomorrow")) {
                    calculatedTime = second / ONE_DAY + "일 후 선물";
                }

            } else {
                if (searchType.equals("today")) {
                    calculatedTime = second / ONE_DAY + "일" + " " + (second - (second / ONE_DAY * ONE_DAY)) / ONE_HOUR + "시간 남음";

                } else if (searchType.equals("tomorrow")) {
                    calculatedTime = second / ONE_DAY + "일" + " " + (second - (second / ONE_DAY * ONE_DAY)) / ONE_HOUR + "시간 후 선물";
                }
            }
        }
        return calculatedTime;
    }

    /**
     * 선물함
     * 입력 받은 시간이 현재 시간을 지났는지 체크
     * true : 현재 시간을 지남
     * false : 현재 시간을 지나지 않음
     *
     * @param inputDate
     * @return
     */
    @SneakyThrows
    public static boolean checkIsPassed(String inputDate) {

        // return value
        boolean result = false;

        // 날짜 포맷
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 타임존 Asia/Seoul 기준 현재 날짜 구하기
        String nowDate = getDatetimeToSeoul();

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 입력 받은 날짜 String -> Date 변환
        Date formatInputDate = formatDatetime.parse(inputDate);

        // return value
        String calculatedTime = "";

        // 시간 간격을 초 단위로 변환
        long differenceInMillis = formatInputDate.getTime() - formatNowDate.getTime();
        long second = TimeUnit.SECONDS.convert(differenceInMillis, TimeUnit.MILLISECONDS);

        // 입력 받은 시간 기준 현재 시간이 같거나 이후인 경우
        if (second <= 0) {
            result = true; // 지급 가능
        }
        return result;
    }
}
