package com.architecture.admin.models.dto.banner;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BannerDto {
    /** banner */
    // 배너 번호
    private Integer idx;
    // 배너 제목
    private String title;
    // 배너 코드
    private String code;
    // 이동 경로
    private String link;
    // 성인 여부
    private Integer adultPavilion;
    // 성인 여부
    private Integer pavilionIdx;

    // 배너 순서
    private Integer sort;
    // 상태값
    private Integer state;
    // 배너 시작일
    private String startDate;
    // 배너 종료일
    private String endDate;
    // 배너 등록일
    private String regdate;
    // 현재 시간
    private String nowDate;

    /** banner_mapping */
    // 배너 매핑 idx
    private Integer bannerMappingIdx;
    // 배너 위치
    private Integer type;
    // 배너 위치 - 카테고리
    private Integer categoryIdx;
    // 배너 위치 - 장르
    private Integer genreIdx;

    /** banner_img */
    // 1.메인/카테고리(720*364), 2.가로A버전(720*260), 3.가로B버전(720*160)
    private Integer imgType;
    // 이미지 경로
    private String path;
    // 이미지 파일명
    private String filename;
    // 이미지 url
    private String url;

    /** banner_click */
    // 클릭 날짜
    private String date;
    // 클릭 수
    private Integer clickCount;

}
