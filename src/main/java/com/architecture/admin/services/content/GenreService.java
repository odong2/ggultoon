package com.architecture.admin.services.content;

import com.architecture.admin.models.daosub.content.GenreDaoSub;
import com.architecture.admin.models.dto.content.GenreDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class GenreService extends BaseService {

    private final GenreDaoSub genreDaoSub;

    public List<GenreDto> getGenreList(Integer categoryIdx) {
        return genreDaoSub.getGenreList(categoryIdx);
    }
}
