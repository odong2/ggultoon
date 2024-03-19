package com.architecture.admin.models.dao.jwt;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;


@Mapper
@Repository
public interface JwtDao {

    /**
     * jwt refresh토큰 저장
     * @param tokenMap
     * @return
     */
    Integer insertRefreshToken(HashMap tokenMap);
    /**
     * jwt refresh토큰 검증
     * @param tokenMap
     * @return
     */
    Integer verifyRefreshToken(HashMap tokenMap);
}


