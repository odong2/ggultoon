package com.architecture.admin.models.daosub.content;

import com.architecture.admin.models.dto.content.GenreDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GenreDaoSub {
    List<GenreDto> getGenreList(Integer categoryIdx);
}
