package com.architecture.admin.models.dto.episode;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpisodeLastViewDto {

    /**
     * member_last_view
     */
    private Long idx;                   // 웹툰 회차 idx
    private Long memberIdx;             // 회원 idx
    private Integer contentIdx;         // contents.idx
    private Integer episodeNumber;      // 마지막으로 본 회차 번호
    private Integer episodeIdx;         // 마지막으로 본 회차 IDX
}
