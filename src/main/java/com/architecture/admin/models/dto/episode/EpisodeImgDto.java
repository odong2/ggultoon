package com.architecture.admin.models.dto.episode;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class EpisodeImgDto {

    /**
     * episode_img
     **/
    private Integer episodeIdx;     //episode.idx
    private Long parent;            //원본 이미지 idx
    private String path;            //이미지 경로
    private String fileName;        //이미지 파일명
    private String url;             //이미지 url
    private Integer width;          //이미지 가로 사이즈
    private Integer height;         //이미지 세로 사이즈
    private String type;            //이미지 타입(width) : 회차는 가로 이미지만 존재
    private Integer sort;           //정렬순서
    private String lang;            //사용 언어
    private Integer state;          //상태값
    private String regdate;         //등록일
    private String regdateTz;       //등록일 타임존

}
