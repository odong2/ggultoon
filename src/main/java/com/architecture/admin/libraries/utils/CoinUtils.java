package com.architecture.admin.libraries.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 마일리지 비율 상수 관리 클래스( 외부 생성 X )
 */
public class CoinUtils {
    public static final int COIN = 1;       // 1: 코인(유형)
    public static final int COIN_FREE = 2;  // 2: 보너스 코인(유형)
    public static final int MILEAGE = 3;    // 3: 마일리지
    public static final int BENEFIT_COIN = 10; // 회원 탈퇴 취소시 지급하는 특별 보너스 코인
    public static final int MILEAGE_PERCENTAGE = 100; // 마일리지 퍼센트 100:1
    public static final int MIN_USE_MILEAGE = 100;   // 최소 사용 가능 마일리지
    
    // 외부 생성 막음
    private CoinUtils(){}
    
    /**
     * 코인, 보너스 코인 ,마일리지 만료일 구하기
     *
     * @param coinType : COIN(1), COIN_FREE(2), MILEAGE(3)
     * @return
     */
    public static String getCoinExpireDate(int coinType) {
        Calendar cal = Calendar.getInstance();
        Date expireDate;
        String stringExpireDate = null;
        // 일반 코인
        if (coinType == COIN) {
            cal.add(Calendar.YEAR, 5); // 5년 후

            // 보너스 코인
        } else if (coinType == COIN_FREE) {
            cal.add(Calendar.MONTH, 1); // 한달 후

            // 마일리지
        } else if (coinType == MILEAGE) {
            cal.add(Calendar.MONTH, 1); // 한달 후
        }

        expireDate = cal.getTime();
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        stringExpireDate = formatDatetime.format(expireDate);

        return stringExpireDate;
    }

    /**
     * 소멸 예정 코인 날짜 구하기 (임시: 일주일 후 )
     *
     * @return
     */
    public static String getExpectedCoinExpireDate() {
        Calendar cal = Calendar.getInstance();
        Date expireDate;
        String stringExpireDate = null;

        // 한달 후
        cal.add(Calendar.DATE, Calendar.DAY_OF_WEEK + 1);
        expireDate = cal.getTime();
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        stringExpireDate = formatDatetime.format(expireDate);

        return stringExpireDate;
    }

    /**
     * 페이백 받을 마일리지 금액 구하기
     * 페이백 마일리지 계산 -> 소진 코인 : 등급 비율 반영 페이백 / 소진 마일리지 : 5% 페이백 고정
     *
     *
     * @param usedCoin : 회원이 구매하면서 소진한 코인 개수
     * @param usedMileage : 회원이 구매하면서 소진한 마일리지 개수
     * @param memberGrade : 회원의 현재 등급
     * @return
     */
    public static int getPayBackMileage(int usedCoin, int usedMileage, int memberGrade) {

        // 페이백 등급 비율
        int payBackPercent;

        switch (memberGrade) {

            default: payBackPercent = 5; // 5% - 0(기본)
                break;

            case 1 : payBackPercent = 6; // 6% - 1(동단지)
                break;

            case 2 : payBackPercent = 7; // 7% - 2(은색단지)
                break;

            case 3 : payBackPercent = 8; // 8% - 3(금단지)
                break;

            case 4 : payBackPercent = 9; // 9% - 4(루비단지)
                break;

            case 5 : payBackPercent = 10; // 10% - 5(다이아단지)
                break;
        }

        // 페이백 마일리지 계산 ( 소진 코인 : 등급 비율 반영 페이백 / 소진 마일리지 : 5% 페이백 고정)
        int payBackMileage = (usedCoin * 100 * payBackPercent / 100) + (usedMileage * 5 / 100);
        return payBackMileage;
    }

    /**
     * 충전 시 등급별 추가 지급되는 마일리지 금액 구하기
     * @param givenMileage : 충전 시 기본으로 지급되는 마일리지
     * @param memberGrade : 회원의 현재 등급
     * @return
     */
    public static int getPaymentMileage(int givenMileage, int memberGrade) {

        // 충전 시 추가 지급되는 마일리지 비율
        int paymentMileagePercent = 0;

        switch (memberGrade) {
            case 0, 1: paymentMileagePercent = 0; // 0(기본), 1(동단지)
                break;

            case 2 : paymentMileagePercent = 1; // 2(은단지)
                break;

            case 3 : paymentMileagePercent = 2; // 3(금단지)
                break;

            case 4 : paymentMileagePercent = 3; // 4(루비단지)
                break;

            case 5 : paymentMileagePercent = 5; // 5(다이아단지)
                break;
        }

        // 충전 시 추가 지급되는 마일리지 계산
        int paymentMileage = givenMileage * paymentMileagePercent / 100;
        return paymentMileage;
    }
}
