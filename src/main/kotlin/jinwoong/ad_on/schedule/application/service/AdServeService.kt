package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import jinwoong.ad_on.schedule.presentation.dto.response.ServingAdDTO
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class AdServeService(
    private val scheduleSyncService: ScheduleSyncService,
    private val scheduleRedisTemplate: RedisTemplate<String, Schedule>,
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val scheduleService: ScheduleService,
) {
    private val updatedSchedules: MutableList<Schedule> = mutableListOf()

    companion object {
        private val log = LoggerFactory.getLogger(AdServeService::class.java)
    }
    /**
     * 서빙할 광고 선택
     * Redis 조회 -> DB 조회 (fallback)
     */
    fun getServingAd(today: LocalDate, currentTime: LocalTime): ServingAdDTO? {
        val candidates = scheduleSyncService.getCandidatesFromRedis()
        val servingAd = getServingAdFromRedis(candidates)
        if (servingAd != null) return servingAd

        log.info("Redis 조회 실패. DB에서 조회")
        return getServingAdFromDB(today, currentTime)
    }

    fun getServingAdFromRedis(candidates: List<Schedule>): ServingAdDTO? {
        if (candidates.isEmpty()) return null

        val servingAd = candidates.random()
        updateBudgetAfterServe(servingAd)
        val servingAdDTO = ServingAdDTO.from(servingAd)
        return servingAdDTO
    }

    fun getServingAdFromDB(today: LocalDate, currentTime: LocalTime): ServingAdDTO? {
        val fallbackCandidates = scheduleService.getCandidatesFromDB(today)
        val filteredCandidates = scheduleSyncService.filterCandidates(fallbackCandidates, currentTime)

        if (filteredCandidates.isEmpty()) {
            return null
        }

        val servingAd = filteredCandidates.random()
        updateBudgetAfterServe(servingAd)
        val servingAdDTO = ServingAdDTO.from(servingAd)
        return servingAdDTO
    }

    /**
     * 광고 서빙 후 budget 업데이트
     */
    fun updateBudgetAfterServe(schedule: Schedule?) {
        if (schedule == null) return
        if (!schedule.hasToPay()) return

        val scheduleId = schedule.id!!

        schedule.campaign.spentTotalBudget += schedule.adSet.unitCost
        schedule.adSet.spentDailyBudget += schedule.adSet.unitCost

        // Redis 캐시 업데이트
        val candidateKey = "candidate:schedule:${scheduleId}"
        val spentBudgetKey = "budget:schedule:${scheduleId}"
        val spentBudgets = SpentBudgets(
            scheduleId = scheduleId,
            spentTotalBudget = schedule.campaign.spentTotalBudget,
            spentDailyBudget = schedule.adSet.spentDailyBudget,
        )

        scheduleRedisTemplate.opsForValue().set(candidateKey, schedule)
        spentBudgetsRedisTemplate.opsForValue().set(spentBudgetKey, spentBudgets)

        // 플랫폼 발송용 리스트 추가
        if (!updatedSchedules.contains(schedule)) updatedSchedules.add(schedule)

        log.info("광고 ${schedule.id} 소진액 수정: total=${schedule.campaign.spentTotalBudget}, daily=${schedule.adSet.spentDailyBudget}")
    }


}
