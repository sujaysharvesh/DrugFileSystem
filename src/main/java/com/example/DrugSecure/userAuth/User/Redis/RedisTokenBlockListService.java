package com.example.DrugSecure.userAuth.User.Redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class RedisTokenBlockListService {

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlockListService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addTokenToBlockList(String token, Long expirationTime) {
         redisTemplate.opsForValue().set(token, "BLOCKED", expirationTime, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlocked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}
