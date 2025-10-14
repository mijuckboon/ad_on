package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Service
class ScheduleSyncService(
    private val scheduleRedisTemplate: RedisTemplate<String, Schedule>,
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val scheduleRepository: ScheduleRepository,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleSyncService::class.java)
        const val CACHE_INTERVAL_IN_MILISEC = 5 * 60 * 1000L // 5분
        const val TTL_IN_MINUTE = 6L // Time To Live
    }

    /**
     * 서빙 가능한 광고를 Redis에 캐싱
     */
    @Scheduled(fixedRate = CACHE_INTERVAL_IN_MILISEC)
    fun cacheCandidates() {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        cacheCandidates(today, currentTime)
    }

    fun cacheCandidates(today: LocalDate, currentTime: LocalTime) {
        val candidates = getCandidatesFromDB(today)
        val filteredCandidates = filterCandidates(candidates, currentTime)

        val ttlInMinute = TTL_IN_MINUTE

        // budget 정보를 candidate에 반영
        filteredCandidates.forEach { schedule ->
            val id = requireNotNull(schedule.id)
            val spentTotalBudgetKey = ScheduleRedisKey.SPENT_TOTAL_BUDGET_V1.key(id)
            val spentDailyBudgetKey = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.key(id)
            val legacySpentBudgetsKey = ScheduleRedisKey.LEGACY_SPENT_BUDGETS.key(id)

            val spentTotalBudget = spentBudgetLongRedisTemplate.opsForValue()
                .get(spentTotalBudgetKey) ?: spentBudgetsRedisTemplate.opsForValue()
                .get(legacySpentBudgetsKey)?.spentTotalBudget ?: 0L
            val spentDailyBudget = spentBudgetLongRedisTemplate.opsForValue()
                .get(spentDailyBudgetKey) ?: spentBudgetsRedisTemplate.opsForValue()
                .get(legacySpentBudgetsKey)?.spentDailyBudget ?: 0L

            schedule.campaign.spentTotalBudget = spentTotalBudget
            schedule.adSet.spentDailyBudget = spentDailyBudget

            val candidateKey = ScheduleRedisKey.CANDIDATE_V1.key(id)
            scheduleRedisTemplate.opsForValue().set(candidateKey, schedule, ttlInMinute, TimeUnit.MINUTES)
        }
        log.info("캐시 갱신 완료, count: ${filteredCandidates.size}")
    }

    fun getCandidatesFromRedis(): List<Schedule> {
        val keys = scheduleRedisTemplate.keys(ScheduleRedisKey.CANDIDATE_V1.scanPattern)
        if (keys.isEmpty()) return emptyList()

        return keys.mapNotNull { scheduleRedisTemplate.opsForValue().get(it) }
    }

    @Transactional(readOnly = true)
    fun getCandidatesFromDB(today: LocalDate): List<Schedule> {
        /* DB 조회: 비교적 실시간성이 낮은 값을 미리 필터링 */
        return scheduleRepository.findCandidates(today)
    }

    /* BE: time, budget 등 실시간성이 필요한 값을 필터링 */
    fun filterCandidates(candidates: List<Schedule>, currentTime: LocalTime): List<Schedule> {
        return candidates
            .filter { it.hasRestBudget() && it.isActiveByTime(currentTime) }
    }

    fun getFilteredCandidatesFromRedis(currentTime: LocalTime): List<Schedule> {
        val candidates = getCandidatesFromRedis()
        return filterCandidates(candidates, currentTime)
    }

    /**
     * Candidate Redis 캐싱
     */
    fun updateBudgetsOfCandidates(schedules: List<Schedule>) {
        schedules.forEach {
            scheduleRedisTemplate.opsForValue().set(
                ScheduleRedisKey.CANDIDATE_V1.key(it.id!!), it, TTL_IN_MINUTE, TimeUnit.MINUTES
            )
        }
    }

    private fun syncCandidateInRedis(schedule: Schedule) {
        val candidateKey = ScheduleRedisKey.CANDIDATE_V1.key(schedule.id!!)
        scheduleRedisTemplate.opsForValue().set(
            candidateKey, schedule, TTL_IN_MINUTE, TimeUnit.MINUTES
        )
    }

    fun syncCandidatesInRedis(schedules: List<Schedule>) {
        schedules.forEach {
            syncCandidateInRedis(it)
        }
    }

    fun resetSpentDailyBudgetsOfCandidates() {
        val candidateScanPattern = ScheduleRedisKey.CANDIDATE_V1.scanPattern
        val candidateKeys = scheduleRedisTemplate.keys(candidateScanPattern)
        candidateKeys.forEach { key ->
            val schedule = scheduleRedisTemplate.opsForValue().get(key)
            if (schedule != null) {
                schedule.adSet.spentDailyBudget = 0L
                scheduleRedisTemplate.opsForValue().set(key, schedule, TTL_IN_MINUTE, TimeUnit.MINUTES)
            }
        }
    }

}