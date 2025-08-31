package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class ScheduleSyncService(
    private val scheduleService: ScheduleService,
    private val scheduleRedisTemplate: RedisTemplate<String, Schedule>,
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleSyncService::class.java)
        const val CACHE_INTERVAL_IN_MILISEC = 5 * 60 * 1000L // 5분
        const val CRON_EXPRESSION_FOR_MIDNIGHT = "0 0 0 * * *"
    }
    /**
     * 서빙 가능한 광고를 Redis에 캐싱
     */
    @Scheduled(fixedRate = CACHE_INTERVAL_IN_MILISEC)
    fun cacheCandidates() {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()

        val candidates = scheduleService.getCandidatesFromDB(today)
        val filteredCandidates = filterCandidates(candidates, currentTime)

        // 기존 캐시 초기화
        scheduleRedisTemplate.delete(scheduleRedisTemplate.keys("candidate:schedule:*"))

        // budget 정보를 candidate에 반영
        filteredCandidates.forEach { schedule ->
            val id = requireNotNull(schedule.id)
            val budgetKey = "budget:schedule:$id"

            spentBudgetsRedisTemplate.opsForValue().get(budgetKey)?.let { b ->
                schedule.campaign.spentTotalBudget = b.spentTotalBudget
                schedule.adSet.spentDailyBudget = b.spentDailyBudget
            }

            val candidateKey = "candidate:schedule:$id"
            scheduleRedisTemplate.opsForValue().set(candidateKey, schedule)
        }
        log.info("캐시 갱신 완료, count: ${filteredCandidates.size}")
    }

    fun getCandidatesFromRedis(): List<Schedule> {
        val keys = scheduleRedisTemplate.keys("candidate:schedule:*")
        if (keys.isEmpty()) return emptyList()

        return keys.mapNotNull { scheduleRedisTemplate.opsForValue().get(it) }
    }

    /* BE: time, budget 등 실시간성이 필요한 값을 필터링 */
    fun filterCandidates(candidates: List<Schedule>, currentTime: LocalTime): List<Schedule> {
        return candidates
            .filter { it.hasRestBudget() && it.isActiveByTime(currentTime) }
    }

    @Scheduled(cron = CRON_EXPRESSION_FOR_MIDNIGHT)
    fun resetSpentDailyBudgetsInRedis() {
        val keys = scheduleRedisTemplate.keys("candidate:schedule:*")
        keys.forEach { key ->
            val schedule = scheduleRedisTemplate.opsForValue().get(key)
            if (schedule != null) {
                schedule.adSet.spentDailyBudget = 0L
                scheduleRedisTemplate.opsForValue().set(key, schedule)
            }
        }
        log.info("일 소진액 초기화 완료 (Redis)")
    }
}