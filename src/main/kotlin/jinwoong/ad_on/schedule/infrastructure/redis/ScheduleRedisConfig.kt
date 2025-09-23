package jinwoong.ad_on.schedule.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class ScheduleRedisConfig (
    private val objectMapper: ObjectMapper // ✅ Boot이 관리하는 mapper 주입
) {

    @Bean
    fun scheduleRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Schedule> {
        val typedObjectMapper = objectMapper.copy() // Boot에서 주입된 ObjectMapper 그대로 복사
            .activateDefaultTyping( // ✅ 타입 정보 추가
                com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
            )

        val redisTemplate = RedisTemplate<String, Schedule>()
        redisTemplate.connectionFactory = connectionFactory
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = GenericJackson2JsonRedisSerializer(typedObjectMapper) // ✅ 타입 정보 포함된 ObjectMapper
        return redisTemplate
    }

    @Bean
    fun spentBudgetsRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, SpentBudgets> {
        val typedObjectMapper = objectMapper.copy()
            .activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
            )

        val redisTemplate = RedisTemplate<String, SpentBudgets>()
        redisTemplate.connectionFactory = connectionFactory
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = GenericJackson2JsonRedisSerializer(typedObjectMapper)
        return redisTemplate
    }

    @Bean
    fun spentBudgetLongRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Long> {
        val template = RedisTemplate<String, Long>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericToStringSerializer(Long::class.java)
        return template
    }


}
