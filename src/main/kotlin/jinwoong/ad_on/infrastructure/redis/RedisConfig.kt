package jinwoong.ad_on.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig (
    private val objectMapper: ObjectMapper // ✅ Boot이 관리하는 mapper 주입
) {

    @Value("\${spring.data.redis.host}")
    lateinit var redisHost: String

    @Value("\${spring.data.redis.port}")
    lateinit var redisPort: String

    @Value("\${spring.data.redis.password}")
    lateinit var redisPassword: String

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = redisHost
        redisStandaloneConfiguration.port = redisPort.toInt()
        redisStandaloneConfiguration.setPassword(redisPassword)

        val clientConfig = LettuceClientConfiguration.builder().build()
        return LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig)
    }

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
}
