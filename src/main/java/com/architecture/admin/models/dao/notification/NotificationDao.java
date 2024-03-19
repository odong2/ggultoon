package com.architecture.admin.models.dao.notification;

import com.architecture.admin.models.dto.notification.NotificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface NotificationDao {

    /*****************************************************
     * INSERT
     ****************************************************/

    /**
     * 충전 완료 알림 전송
     *
     * @param dto (전송할 알림 정보)
     */
    void insertChargeNotification(NotificationDto dto);

    /**
     * 공지사항 알림 전송
     *
     * @param sendNoticeList (전송할 공지사항 리스트)
     */
    void insertNoticeNotification(List<NotificationDto> sendNoticeList);


    /*****************************************************
     * UPDATE
     ****************************************************/

    /**
     * 마이페이지 - 알림 읽음 표시
     *
     * @param dto (회원 idx, 알림 idx, 확인일)
     */
    int updateCheckDate(NotificationDto dto);

    /*****************************************************
     * DELETE
     ****************************************************/

    /**
     * 마이페이지 - 알림 삭제
     *
     * @param list 삭제할 알림 목록
     */
    int deleteNotification(List<NotificationDto> list);


}
