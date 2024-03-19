package com.architecture.admin.controllers.v1.content;


import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.content.GenreDto;
import com.architecture.admin.services.content.GenreService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/genre")
public class GenreV1Controller extends BaseController {

    private final GenreService genreService;


    @GetMapping("")
    public String getGenreList(HttpServletRequest request) {
        String requestParam = request.getParameter("categoryIdx");
        Integer categoryIdx = null;
        if (requestParam == null || requestParam.equals("")) {
            categoryIdx = 1;
        } else {
            if (isNumeric(requestParam.trim())) {
                categoryIdx = Integer.parseInt(requestParam.trim());
            } else {
                throw new CustomException(CustomError.GENRE_NOT_EXIST);
            }
        }

        System.out.println("categoryIdx  : " + categoryIdx);

        List<GenreDto> genreList = genreService.getGenreList(categoryIdx);
        if (genreList.size() == 0) {
            throw new CustomException(CustomError.GENRE_NOT_EXIST);
        }

        JSONObject data = new JSONObject();
        data.put("list", genreList);

        return displayJson(true, "1000", "", data);

    }


    /**
     * 정수 확인
     * @param s
     * @return
     */
    private static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}
