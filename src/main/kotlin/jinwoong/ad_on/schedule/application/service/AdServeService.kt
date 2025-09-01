package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
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
    private val scheduleRepository: ScheduleRepository,
) {
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
        val budgetUpdatedAd = updateBudgetAfterServe(servingAd) ?: servingAd
        val servingAdDTO = ServingAdDTO.from(budgetUpdatedAd)
        return servingAdDTO
    }

    fun getServingAdFromDB(today: LocalDate, currentTime: LocalTime): ServingAdDTO? {
        val fallbackCandidates = scheduleSyncService.getCandidatesFromDB(today)
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
    fun updateBudgetAfterServe(schedule: Schedule): Schedule {
        val candidateIds = scheduleSyncService.getCandidatesFromRedis().mapNotNull { it.id }.toSet()
        val schedulesToUpdate = getSchedulesToUpdate(schedule)

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
        cacheCandidatesToRedis(schedulesToUpdate.filter { candidateIds.contains(it.id) })

        // 플랫폼 서버 이력 전송
        sendHistoryToPlatform(schedule)

        log.info(
            "광고 ${schedule.id} 소진액 수정: total=${schedule.campaign.spentTotalBudget}, daily=${schedule.adSet.spentDailyBudget}"
        )

        return schedule
    }

    /**
     * Campaign/AdSet 기준으로 갱신 대상 스케줄 조회
     */
    private fun getSchedulesToUpdate(schedule: Schedule): List<Schedule> =
        scheduleRepository.findAllByCampaignId(schedule.campaign.campaignId)
            .union(scheduleRepository.findAllByAdSetId(schedule.adSet.adSetId))
            .distinctBy { it.id }
            .toList()

    /**
     * Redis에서 기존 Budget 조회 후 계산 + 저장
     */
    private fun fetchAndUpdateBudgets(schedule: Schedule): SpentBudgets {
        val spent = spentBudgetsRedisTemplate.opsForValue().get("spentBudgets:schedule:${schedule.id}")
        val newTotal = (spent?.spentTotalBudget ?: 0L) + schedule.adSet.unitCost
        val newDaily = (spent?.spentDailyBudget ?: 0L) + schedule.adSet.unitCost

        // Redis 저장
        val updated = SpentBudgets(schedule.id!!, newTotal, newDaily)
        spentBudgetsRedisTemplate.opsForValue().set("spentBudgets:schedule:${schedule.id}", updated)

        return updated
    }

    /**
     * Candidate Redis 캐싱
     */
    private fun cacheCandidatesToRedis(schedules: List<Schedule>) {
        schedules.forEach { scheduleRedisTemplate.opsForValue().set("candidate:schedule:${it.id}", it) }
    }


    private fun sendHistoryToPlatform(schedule: Schedule) {

    }


}
