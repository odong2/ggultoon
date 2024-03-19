package com.architecture.admin.models.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenreDto {

    // 장르 idx
    private Integer idx;
    // 카테고리 idx
    private Integer categoryIdx;
    // 장르 이름
    private String name;
    // 장르 순서
    private Integer sort;

}
