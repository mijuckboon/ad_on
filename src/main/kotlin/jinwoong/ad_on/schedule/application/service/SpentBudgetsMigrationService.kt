package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service

/**
 * 레거시 Redis Budget 데이터를 새로운 Redis 키로 마이그레이션
 */
@Service
class SpentBudgetsMigrationService(
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SpentBudgetsMigrationService::class.java)
    }

    /**
     * 레거시 budget 데이터를 새로운 Redis 키로 이전
     */
    fun migrateSpentBudgets(batchSize: Int = 100) {
        val scanOptions = ScanOptions.scanOptions()
            .match(ScheduleRedisKey.LEGACY_SPENT_BUDGETS.scanPattern)
            .count(batchSize.toLong())
            .build()

        spentBudgetsRedisTemplate.execute { connection ->
            connection.keyCommands().scan(scanOptions).use { cursor ->
                while (cursor.hasNext()) {
                    val currentCursor = cursor.next()
                    val legacyKey = String(currentCursor) // ByteArray → String 변환
                    val legacyBudget = spentBudgetsRedisTemplate.opsForValue().get(legacyKey) ?: return@use

                    val scheduleId = legacyBudget.scheduleId
                    val newTotalKey = ScheduleRedisKey.SPENT_TOTAL_BUDGET_V1.key(scheduleId)
                    val newDailyKey = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.key(scheduleId)

                    spentBudgetLongRedisTemplate.opsForValue().setIfAbsent(newTotalKey, legacyBudget.spentTotalBudget)
                    spentBudgetLongRedisTemplate.opsForValue().setIfAbsent(newDailyKey, legacyBudget.spentDailyBudget)
                }
            }
        }

        log.info("기존 spent budgets 마이그레이션 완료 (batch)")
    }
}
