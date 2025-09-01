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
        val filteredCandidates = scheduleSyncService.getFilteredCandidatesFromRedis(currentTime)
        val servingAd = getServingAdFromRedis(filteredCandidates, currentTime)
        if (servingAd != null) return servingAd

        log.info("Redis 조회 실패. DB에서 조회")
        return getServingAdFromDB(today, currentTime)
    }

    fun getServingAdFromRedis(candidates: List<Schedule>, currentTime: LocalTime): ServingAdDTO? {
        if (candidates.isEmpty()) return null
        return chooseRandomAd(candidates, currentTime)
    }

    fun getServingAdFromDB(today: LocalDate, currentTime: LocalTime): ServingAdDTO? {
        val filteredCandidates = scheduleSyncService.getFilteredCandidatesFromDB(currentTime, today)

        if (filteredCandidates.isEmpty()) {
            return null
        }

        return chooseRandomAd(filteredCandidates, currentTime)
    }

    fun chooseRandomAd(candidates: List<Schedule>, currentTime: LocalTime): ServingAdDTO {
        val servingAd = candidates.random()
        val budgetUpdatedAd = updateBudgetAfterServe(servingAd, currentTime)
        sendHistoryToPlatform(budgetUpdatedAd)
        return ServingAdDTO.from(budgetUpdatedAd)
    }

    /**
     * 광고 서빙 후 budget 업데이트
     */
    fun updateBudgetAfterServe(schedule: Schedule, currentTime: LocalTime): Schedule {
        if (!schedule.hasToPay()) {
            return schedule
        }
        val candidateIds = scheduleSyncService.getFilteredCandidatesFromRedis(currentTime).mapNotNull { it.id }.toSet()
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
        val candidatesToUpdate = schedulesToUpdate.filter { candidateIds.contains(it.id) }
        scheduleSyncService.updateBudgetsOfCandidates(candidatesToUpdate)

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
            .filter { it.hasToPay() }
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

    private fun sendHistoryToPlatform(schedule: Schedule) {

    }
}
