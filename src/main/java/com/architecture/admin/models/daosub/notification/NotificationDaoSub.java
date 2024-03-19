package com.architecture.admin.models.daosub.notification;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.notification.NotificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface NotificationDaoSub {

    /*****************************************************
     * Select
     ****************************************************/

    /**
     * 마이페이지 - 알림 개수 카운트
     *
     * @param searchDto (회원 idx 세팅)
     * @return
     */
    int getMemberNotificationCnt(SearchDto searchDto);

    /**
     * 마이페이지 - 알림 목록
     *
     * @param searchDto (회원 idx 세팅)
     * @return
     */
    List<NotificationDto> getMemberNotificationList(SearchDto searchDto);

    /**
     * 알림 정보 조회
     *
     * @param dto (회원 idx, 알림 idx, 확인 날짜)
     */
    NotificationDto getNotification(NotificationDto dto);

    /**
     * 회원에게 전송된 공지사항 알림 idx 리스트 조회
     *
     * @param searchDto (회원 idx)
     */
    List<Long> getNoticeAlarmIdxList(SearchDto searchDto);

    /**
     * 회원이 읽지 않은 알림 개수 카운트
     *
     * @param memberIdx (회원 idx)
     */
    int getUnreadNotiCnt(Long memberIdx);
}
