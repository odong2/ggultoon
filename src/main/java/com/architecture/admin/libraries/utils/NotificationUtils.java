package com.architecture.admin.libraries.utils;

/**
 * 알림 상수 클래스
 */
public class NotificationUtils {

    private NotificationUtils() {}

    // 알림 타입 및 카테고리 상수
    public static final String CHARGE = "charge"; // 충전
    public static final String CHARGE_TEXT = "충전";
    public static final String EXPIRE = "expire"; // 소멸
    public static final String EXPIRE_TEXT = "소멸";
    public static final String CONTENT = "content"; // 작품
    public static final String CONTENT_TEXT = "작품";
    public static final String NOTICE = "notice"; // 공지
    public static final String NOTICE_TEXT = "공지";
    public static final String EVENT = "event"; // 이벤트
    public static final String EVENT_TEXT = "이벤트";
    public static final String CANCEL = "cancel"; // 취소
    public static final String CANCEL_TEXT = "취소";
    public static final String COIN_TEXT = "코인";
    public static final String MILEAGE_TEXT = "마일리지";

    // 알림 이동 URL 상수
    public static final String LIBRARY_VIEW_URL = "/my/lib/view"; // 내 서재 > 최근 본 내역
    public static final String LIBRARY_RENT_URL = "/my/lib/rent"; // 내 서재 > 대여한 내역
    public static final String LIBRARY_HAVE_URL = "/my/lib/have"; // 내 서재 > 소장한 내역
    public static final String LIBRARY_FAVORITE_URL = "/my/lib/favorite"; // 내 서재 > 찜한 내역
    public static final String CHARGE_URL = "/my/history/charged"; // 이용 내역 > 충전 페이지
    public static final String USE_URL = "/my/history/used"; // 이용 내역 > 사용 페이지
    public static final String EXPIRE_URL = "/my/history/expired"; // 이용 내역 > 소멸 페이지
    public static final String CONTENT_URL = "/contents/idx/episode"; // 회차 리스트 페이지
    public static final String NOTICE_URL = "/help/notice"; // 공지사항 페이지
    public static final String PRODUCT_URL = "/charging"; // 충전소 페이지
    public static final String EVENT_URL = ""; // 이벤트 페이지

    // 알림 내용 상수
    public enum Notification {
        PAYMENT_CANCEL("lang.notification.cancel.payment", CHARGE_URL) // 결제 취소가 완료됐어요.
        , CHARGE_COMPLETE("lang.notification.charge", CHARGE_URL) // 충전이 정상적으로 완료됐어요.
        , EXPIRE_WEEK_LATER("lang.notification.expire", EXPIRE_URL) // 7일 후 regdate에 지급된 00개가 소멸 예정이에요.
        , NEW_EPISODE_UPDATE("lang.notification.episode.update", CONTENT_URL) // content의 episodeNumber이(가) 새롭게 업데이트됐어요.
        , SERVICE_INSPECTION("lang.notification.service.inspection", NOTICE_URL) // 꿀툰이 서비스 정기 점검 예정이에요.
        , NEW_PAY_METHOD_OPEN("lang.notification.pay.method.open", PRODUCT_URL) // 새로운 결제 수단이 오픈했어요.
        , NEW_NOTICE_UPDATE("lang.notification.notice.update", NOTICE_URL) // 새로운 공지사항이 등록됐어요.
        , NEW_EVENT_OPEN("lang.notification.event.open", NOTICE_URL) // 00이벤트가 새롭게 시작됐어요.
        , EVENT_CLOSE("lang.notification.event.close", NOTICE_URL) // 00이벤트가 종료됐어요.
        ;

        private String text;
        private String url;

        public String getText() {
            return text;
        }
        public String getUrl() {
            return url;
        }
        Notification(String text, String url) {
            this.text = text;
            this.url = url;
        }
    }
}