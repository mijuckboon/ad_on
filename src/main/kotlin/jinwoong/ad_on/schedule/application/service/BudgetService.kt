package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class BudgetService(
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>,
    private val scheduleSyncService: ScheduleSyncService,
    private val scheduleService: ScheduleService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BudgetService::class.java)
        const val CRON_EXPRESSION_FOR_MIDNIGHT = "0 0 0 * * *"
    }

    /**
     * 광고 서빙 후 budget 업데이트
     */
    fun updateBudgetAfterServe(schedule: Schedule, currentTime: LocalTime): Schedule {
        if (!schedule.hasToPay()) {
            return schedule
        }
        val candidateIds = scheduleSyncService.getFilteredCandidatesFromRedis(currentTime).mapNotNull { it.id }.toSet()
        val schedulesToUpdate = scheduleService.getSchedulesToUpdate(schedule)

        schedulesToUpdate.forEach {
            val updatedBudgets = fetchAndUpdateBudgets(it)
            it.campaign.spentTotalBudget = updatedBudgets.spentTotalBudget
            it.adSet.spentDailyBudget = updatedBudgets.spentDailyBudget

            // 서빙 스케줄이면 Domain 객체도 갱신
            if (it.id == schedule.id) {
                schedule.campaign.spentTotalBudget = updatedBudgets.spentTotalBudget
                schedule.adSet.spentDailyBudget = updatedBudgets.spentDailyBudget
            }
        }

        // Candidate Redis 갱신
        val candidatesToUpdate = schedulesToUpdate.filter { candidateIds.contains(it.id) }
        scheduleSyncService.updateBudgetsOfCandidates(candidatesToUpdate)

        log.info(
            "광고 ${schedule.id} 소진액 수정: total=${schedule.campaign.spentTotalBudget}, "
                + "daily=${schedule.adSet.spentDailyBudget}"
        )

        return schedule
    }


    /**
     * Redis에서 기존 Budget 조회 후 계산 + 저장
     */
    private fun fetchAndUpdateBudgets(schedule: Schedule): SpentBudgets {
        val scheduleId = schedule.id!!
        val spentTotalBudgetKey = ScheduleRedisKey.SPENT_TOTAL_BUDGET_V1.key(scheduleId)
        val spentDailyBudgetKey = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.key(scheduleId)
        val legacySpentBudgetsKey = ScheduleRedisKey.LEGACY_SPENT_BUDGETS.key(scheduleId)

        // 1. 초기화
        initializeSpentTotalBudget(spentTotalBudgetKey, legacySpentBudgetsKey)
        initializeSpentDailyBudget(spentDailyBudgetKey, legacySpentBudgetsKey)

        // 2. atomic increment
        val spentTotal = increaseSpentBudget(spentTotalBudgetKey, schedule.adSet.unitCost)
        val spentDaily = increaseSpentBudget(spentDailyBudgetKey, schedule.adSet.unitCost)

        return SpentBudgets(scheduleId, spentTotal, spentDaily)
    }

    private fun initializeSpentTotalBudget(totalKey: String, legacyKey: String) {
        val legacy = spentBudgetsRedisTemplate.opsForValue().get(legacyKey)
        val initialTotal = legacy?.spentTotalBudget ?: 0L
        spentBudgetLongRedisTemplate.opsForValue().setIfAbsent(totalKey, initialTotal)
    }

    private fun initializeSpentDailyBudget(dailyKey: String, legacyKey: String) {
        val legacy = spentBudgetsRedisTemplate.opsForValue().get(legacyKey)
        val initialDaily = legacy?.spentDailyBudget ?: 0L
        spentBudgetLongRedisTemplate.opsForValue().setIfAbsent(dailyKey, initialDaily)
    }

    private fun increaseSpentBudget(key: String, amount: Long): Long {
        return spentBudgetLongRedisTemplate.opsForValue().increment(key, amount)!!
    }

    @Scheduled(cron = CRON_EXPRESSION_FOR_MIDNIGHT)
    fun resetSpentDailyBudgetsInRedis() {
        resetSpentDailyBudgetValues()
        scheduleSyncService.resetSpentDailyBudgetsOfCandidates()
        log.info("일 소진액 초기화 완료 (Redis)")
    }

    private fun resetSpentDailyBudgetValues() {
        val spentDailyBudgetsScanPattern = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.scanPattern
        val spentDailyBudgetKeys = spentBudgetLongRedisTemplate.keys(spentDailyBudgetsScanPattern)
        spentDailyBudgetKeys.forEach { key ->
            spentBudgetLongRedisTemplate.opsForValue().set(key, 0L)
        }
    }

}