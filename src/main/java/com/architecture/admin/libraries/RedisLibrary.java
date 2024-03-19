package com.architecture.admin.libraries;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/*****************************************************
 * Redis 라이브러리
 ****************************************************/
@Component
public class RedisLibrary {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLibrary(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setData(String key, String value, Integer expiredTime) {
        redisTemplate.opsForValue().set(key, value, expiredTime, TimeUnit.SECONDS);
    }

    // redis 조회
    public String getData(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // redis 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }
}
