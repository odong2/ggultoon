package com.architecture.admin.models.dto.community;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class CommunityImageDto {

    private Integer idx;                  // idx
    private Integer communityContentsIdx; // 커뮤니티 컨텐츠 idx
    private Integer parent;               // origin idx
    private String url;                   // 이미지 url
    private String path;                  // 경로
    private String filename;              // 이미지 파일명
    private String type;                  // 이미지 타입(가로, 세로 구분)
    private String device;                // 디바이스 (리사이즈 구분 : origin, pc, mobile, tablet)
    private Integer width;                // resize 가로 사이즈
    private Integer height;               // resize 높이 사이즈
    private Integer sort;                 // 순서
    private Integer state;                // 상태값
    private String regdate;               // 등록일
}
