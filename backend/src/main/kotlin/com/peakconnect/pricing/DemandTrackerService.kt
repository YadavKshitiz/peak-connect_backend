package com.peakconnect.pricing

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class DemandTrackerService(
    private val stringRedisTemplate: StringRedisTemplate
) {

    /**
     * Increments the view count for a slot.
     * If the key doesn't exist, it sets it with a 24-hour TTL.
     */
    fun trackSlotView(slotId: UUID) {
        val key = "slot:views:\$slotId"
        val count = stringRedisTemplate.opsForValue().increment(key)
        
        // If this is the first view (count == 1), set the TTL
        if (count == 1L) {
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS)
        } else {
            // Also handle the edge case where increment happens but TTL was lost/not set
            val ttl = stringRedisTemplate.getExpire(key)
            if (ttl == -1L) {
                stringRedisTemplate.expire(key, 24, TimeUnit.HOURS)
            }
        }
    }

    /**
     * Reads the current view count for a slot.
     */
    fun getSlotViews(slotId: UUID): Long {
        val key = "slot:views:\$slotId"
        val countStr = stringRedisTemplate.opsForValue().get(key)
        return countStr?.toLongOrNull() ?: 0L
    }
}
