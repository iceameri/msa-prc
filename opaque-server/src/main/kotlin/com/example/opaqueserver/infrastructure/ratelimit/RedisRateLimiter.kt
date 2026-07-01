package com.example.opaqueserver.infrastructure.ratelimit

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

// 토큰 버킷 알고리즘 — Redis Lua 스크립트로 원자적 실행
@Component
class RedisRateLimiter(private val stringRedisTemplate: StringRedisTemplate) {

    private val script = RedisScript.of(
        """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local refill_rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])

        local data = redis.call('HMGET', key, 'tokens', 'ts')
        local tokens = tonumber(data[1]) or capacity
        local ts = tonumber(data[2]) or now

        local elapsed = math.max(0, (now - ts) / 1000)
        local new_tokens = math.min(capacity, tokens + elapsed * refill_rate)

        if new_tokens >= 1 then
            redis.call('HMSET', key, 'tokens', tostring(new_tokens - 1), 'ts', tostring(now))
            redis.call('EXPIRE', key, 60)
            return 1
        else
            redis.call('HMSET', key, 'tokens', tostring(new_tokens), 'ts', tostring(now))
            redis.call('EXPIRE', key, 60)
            return 0
        end
        """.trimIndent(),
        Long::class.java
    )

    fun tryConsume(key: String, capacity: Long, refillPerSecond: Long): Boolean {
        val result = stringRedisTemplate.execute(
            script,
            listOf(key),
            capacity.toString(),
            refillPerSecond.toString(),
            System.currentTimeMillis().toString()
        )
        return result == 1L
    }
}
