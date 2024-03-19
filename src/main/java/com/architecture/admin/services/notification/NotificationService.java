package com.architecture.admin.services.notification;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.notification.NotificationDao;
import com.architecture.admin.models.daosub.board.BoardDaoSub;
import com.architecture.admin.models.daosub.coin.CoinDaoSub;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.daosub.notification.NotificationDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.board.BoardDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.architecture.admin.libraries.utils.NotificationUtils.*;
import static com.architecture.admin.libraries.utils.NotificationUtils.Notification.*;

@RequiredArgsConstructor
@Service
@Transactional
public class NotificationService extends BaseService {

    private final CoinDaoSub coinDaoSub;
    private final ContentDaoSub contentDaoSub;
    private final NotificationDaoSub notificationDaoSub;
    private final NotificationDao notificationDao;
    private final BoardDaoSub boardDaoSub;

    /********************************************************************************
     * SELECT
     ********************************************************************************/

    /**
     * 마이페이지 - 알림 목록
     *
     * @param searchDto (회원 idx)
     */
    public JSONObject getMemberNotificationList(SearchDto searchDto) {

        /** 1. 신규 등록 + 알림 미전송된 공지사항 조회 -> 있을 경우 회원 알림 전송 **/
        // 등록된 공지사항 리스트 조회
        List<BoardDto> noticeList = boardDaoSub.getAllNoticeIdxList();

        // 회원에게 전송된 공지사항 알림 idx 리스트 조회
        List<Long> noticeAlarmIdxList = notificationDaoSub.getNoticeAlarmIdxList(searchDto);

        // 회원에게 이미 전송된 공지사항은 알림 보낼 리스트에서 제거
        if (noticeAlarmIdxList != null && !noticeAlarmIdxList.isEmpty()) {
            for (Long alarmIdx : noticeAlarmIdxList) {
                noticeList.removeIf(item -> item.getIdx().equals(alarmIdx));
            }
        }

        // 중복 제거 후에도 전송할 공지사항 알림이 남은 경우 -> 알림 전송
        if (noticeList != null && !noticeList.isEmpty()) {

            // 알림 전송할 리스트 set
            List<NotificationDto> sendNoticeList = new ArrayList<>();
            for (BoardDto board : noticeList) {

                // dto set
                NotificationDto dto = NotificationDto.builder()
                        .memberIdx(searchDto.getMemberIdx()) // 회원 idx
                        .category(NOTICE) // 알림 카테고리
                        .type("notice") // 알림 보낼 테이블명
                        .typeIdx(board.getIdx()) // 알림 보낼 테이블 idx
                        .state(1)
                        .regdate(board.getRegdate()) // 공지사항 등록일을 알림 전송일로 설정(정책)
                        .build();

                // list add
                sendNoticeList.add(dto);
            }
            // 공지사항 리스트 알림 전송
            notificationDao.insertNoticeNotification(sendNoticeList);
        }

        /** 2. 회원에게 전송된 알림 목록 전체 조회 **/
        // 데이터를 담아서 리턴할 객체 생성
        JSONObject jsonData = new JSONObject();
        List<NotificationDto> notificationList = null;

        // 회원별 알림 개수 카운트
        int totalCnt = notificationDaoSub.getMemberNotificationCnt(searchDto);

        // 보여줄 알림이 있으면
        if (totalCnt > 0) {

            // 페이징 처리
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            // 회원별 알림 리스트 조회
            notificationList = notificationDaoSub.getMemberNotificationList(searchDto);

            // 회원 알림 세부 정보 세팅
            setNotificationInfo(searchDto, notificationList);

            // paging
            jsonData.put("params", new JSONObject(searchDto));
        }

        // list
        jsonData.put("notificationList", notificationList);

        return jsonData;
    }

    /********************************************************************************
     * UPDATE
     ********************************************************************************/

    /**
     * 마이페이지 - 알림 읽음 표시
     *
     * @param dto (회원 idx, 알림 idx)
     */
    public void updateCheckDate(NotificationDto dto) {

        // 선택한 알림 유효성 검사
        notificationValidate(dto);

        // 아직 확인하지 않은 알림인 경우에만
        if (dto.getCheckDate() == null || dto.getCheckDate().isEmpty()) {
            dto.setCheckDate(dateLibrary.getDatetime()); // 확인일(현재 시간) 세팅

            // 확인일 업데이트
            int result = notificationDao.updateCheckDate(dto);

            // 알림 확인을 실패한 경우
            if (result < 1) {
                throw new CustomException(CustomError.NOTIFICATION_CHECK_FAIL); // 알림을 확인할 수 없습니다.
            }
        }
    }

    /********************************************************************************
     * DELETE
     ********************************************************************************/

    /**
     * 마이페이지 - 선택한 알림 목록 삭제
     *
     * @param list (삭제할 알림 목록)
     * @return
     */
    public void deleteNotification(List<NotificationDto> list) {

        // 알림 idx 유효성 검사
        for (NotificationDto dto : list) {
            notificationValidate(dto);
        }

        // 선택한 알림 목록 삭제
        int result = notificationDao.deleteNotification(list);

        // 알림 삭제를 실패한 경우
        if (result < 1) {
            throw new CustomException(CustomError.NOTIFICATION_DELETE_FAIL); // 알림을 삭제할 수 없습니다.
        }
    }

    /**************************************************************************
     * SUB
     **************************************************************************/
    /**
     * 안 읽은 알림 개수 + 알림 카테고리 + 알림 내용 + 알림 클릭 시 이동할 페이지 url 세팅
     *
     * @param searchDto        (안 읽은 알림 개수 세팅용)
     * @param notificationList (회원 idx, 알림 idx)
     */
    private void setNotificationInfo(SearchDto searchDto, List<NotificationDto> notificationList) {

        // 안 읽은 알림 개수(기본값 = 전체 개수)
        int unreadCnt = notificationList.size();

        // 알림 내용 세팅용 변수
        String title = "";

        for (NotificationDto dto : notificationList) {
            if (dto != null) {

                // 알림 읽음 여부 기본값 set
                dto.setIsChecked(false);

                // 알림 등록일 텍스트 변환
                dto.setRegdate(dateLibrary.getConvertRegdate(dto.getRegdate()));

                /************ 안 읽은 알림 개수 세팅 ************/
                // 읽고 삭제하지 않은 경우
                if (dto.getCheckDate() != null && !dto.getCheckDate().isEmpty() && dto.getState() == 1) {
                    dto.setIsChecked(true);
                    unreadCnt--; // 안 읽은 알림 개수 -1

                    // 읽지 않고 삭제하지 않은 경우
                } else if (dto.getCheckDate() == null && dto.getCheckDate().isEmpty() && dto.getState() == 1) {
                    dto.setIsChecked(false); // 안 읽음 표시
                }

                /************ 카테고리 & 내용 & 이동 URL 세팅 ************/
                // 충전 알림
                if (dto.getCategory().equals(CHARGE)) {
                    dto.setCategory(CHARGE_TEXT); // 충전
                    dto.setTitle(super.langMessage(CHARGE_COMPLETE.getText())); // 충전이 정상적으로 완료됐어요.
                    dto.setUrl(CHARGE_COMPLETE.getUrl());
                }

                // 소멸 알림
                if (dto.getCategory().equals(EXPIRE)) {
                    dto.setCategory(EXPIRE_TEXT); // 소멸
                    dto.setUrl(EXPIRE_WEEK_LATER.getUrl());

                    // 소멸될 코인 또는 마일리지의 지급일 조회 -> 00년 00월 00일로 변환
                    String regdate = dateLibrary.formatDay(coinDaoSub.getCoinOrMileageRegdate(dto));

                    // 소멸 구분값 set
                    String type = "";
                    if (dto.getType().equals("member_coin_used")) {
                        type = COIN_TEXT; // 코인

                        // 마일리지 소멸 알림
                    } else if (dto.getType().equals("member_mileage_used")) {
                        type = MILEAGE_TEXT; // 마일리지

                    }
                    // 알림 내용 set
                    title = super.langMessage(EXPIRE_WEEK_LATER.getText())
                            .replace("regdate", regdate)
                            .replace("00", type + " " + dto.getTitle());
                    dto.setTitle(title); // 7일 후 00년 00월 00일에 지급된 마일리지 00개가 소멸 예정이에요.
                }

                // 작품 알림
                if (dto.getCategory().equals(CONTENT)) {
                    dto.setCategory(CONTENT_TEXT); // 작품

                    // 신규 업데이트 회차 알림
                    if (dto.getType().equals("episode")) {

                        // 신규 회차가 업로드된 작품 및 회차 번호 조회
                        HashMap<String, Object> contentInfo = contentDaoSub.getNewEpisodeInfo(dto);
                        String content = contentInfo.get("content").toString(); // 작품 이름
                        String episodeNumber = contentInfo.get("episodeNumber").toString(); // 회차 번호

                        // 작품 구분값 set
                        String type = "";
                        if (!contentInfo.get("categoryIdx").equals(2)) { // 웹툰 OR 소설
                            type = "화";

                        } else { // 만화
                            type = "권";
                        }

                        // 알림 내용 set
                        title = super.langMessage(NEW_EPISODE_UPDATE.getText())
                                .replace("content", content)
                                .replace("episodeNumber", episodeNumber + type);
                        dto.setTitle(title); // content의 episodeNumber이(가) 새롭게 업데이트됐어요.

                        // 알림 url set
                        String idx = dto.getTypeIdx().toString();
                        String url = NEW_EPISODE_UPDATE.getUrl().replace("idx", idx);
                        dto.setUrl(url);
                    }
                }

                // 공지사항 알림
                if (dto.getCategory().equals(NOTICE)) {
                    dto.setCategory(NOTICE_TEXT); // 공지

                    // 신규 결제 수단 오픈 알림
                    if (dto.getType().equals("payment_method")) {
                        dto.setTitle(super.langMessage(NEW_PAY_METHOD_OPEN.getText())); // 새로운 결제 수단이 오픈했어요.
                        dto.setUrl(NEW_PAY_METHOD_OPEN.getUrl());

                        // 그 외 일반 공지 알림
                    } else {
                        dto.setTitle(super.langMessage(NEW_NOTICE_UPDATE.getText())); // 새로운 공지사항이 등록됐어요.
                        dto.setUrl(NEW_NOTICE_UPDATE.getUrl());
                    }
                }

                // 결제 취소 알림
                if (dto.getCategory().equals(CANCEL)) {
                    dto.setCategory(CANCEL_TEXT); // 취소
                    dto.setTitle(super.langMessage(PAYMENT_CANCEL.getText())); // 결제 취소가 완료됐어요.
                    dto.setUrl(PAYMENT_CANCEL.getUrl());
                }

                // 앞단에서 사용하지 않는 데이터 null 처리
                dto.setMemberIdx(null);
                dto.setType(null);
                dto.setTypeIdx(null);
            }
        }
        // 전체 알림 중 안 읽은 알림 개수 set
        searchDto.setUnreadCount(unreadCnt);
    }

    /**************************************************************************
     * Validation
     **************************************************************************/

    /**
     * 알림 유효성 검사
     *
     * @param dto (회원 idx, 알림 idx)
     * @throws Exception
     */
    private void notificationValidate(NotificationDto dto) {

        // 선택한 알림 idx가 없는 경우
        if (dto.getIdx() == null || dto.getIdx() < 1) {
            throw new CustomException(CustomError.NOTIFICATION_IDX_ERROR); // 알림을 선택해주세요.
        }

        // 선택한 알림 정보 가져오기
        NotificationDto getNotification = notificationDaoSub.getNotification(dto);

        // 회원에게 전송된 알림이 아닐 경우
        if (getNotification == null) {
            throw new CustomException(CustomError.NOTIFICATION_IDX_NOT_EXIST); // 요청하신 알림 정보를 찾을 수 없습니다.
        }

        // 이미 삭제된 알림일 경우
        if (getNotification.getDelDate() != null) {
            throw new CustomException(CustomError.NOTIFICATION_DELETED); // 이미 삭제된 알림입니다.
        }
    }
}
