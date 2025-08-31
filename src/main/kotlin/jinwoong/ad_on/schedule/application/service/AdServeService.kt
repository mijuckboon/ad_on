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
        updateBudgetAfterServe(servingAd)
        val servingAdDTO = ServingAdDTO.from(servingAd)
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
    fun updateBudgetAfterServe(schedule: Schedule?) {
        if (schedule == null) return
        if (!schedule.hasToPay()) return

        val schedulesByCampaign = updateCampaignBudgets(schedule)
        val schedulesByAdSet = updateAdSetBudgets(schedule)

        // Redis 캐시 동기화 (모든 관련 Schedule)
        val allSchedulesToSync = (schedulesByCampaign + schedulesByAdSet).distinctBy { it.id }
        syncSchedulesToRedis(allSchedulesToSync)

        /* 플랫폼 서버로 이력 발송 */
        sendHistoryToPlatform(schedule)
        log.info("광고 ${schedule.id} 소진액 수정: total=${schedule.campaign.spentTotalBudget}, daily=${schedule.adSet.spentDailyBudget}")
    }

    private fun updateCampaignBudgets(schedule: Schedule): List<Schedule> {
        val schedulesByCampaign = scheduleRepository.findAllByCampaignId(schedule.campaign.campaignId)
        schedulesByCampaign.forEach { it.campaign.spentTotalBudget += it.adSet.unitCost }
        return schedulesByCampaign
    }

    private fun updateAdSetBudgets(schedule: Schedule): List<Schedule> {
        val schedulesByAdSet = scheduleRepository.findAllByAdSetId(schedule.adSet.adSetId)
        schedulesByAdSet.forEach { it.adSet.spentDailyBudget += it.adSet.unitCost }
        return schedulesByAdSet
    }

    private fun syncSchedulesToRedis(schedules: List<Schedule>) {
        schedules.forEach { scheduleToSync ->
            val candidateKey = "candidate:schedule:${scheduleToSync.id}"
            scheduleRedisTemplate.opsForValue().set(candidateKey, scheduleToSync)

            val spentBudgetKey = "spentBudgets:schedule:${scheduleToSync.id}"
            val spentBudgets = SpentBudgets(
                scheduleId = scheduleToSync.id!!,
                spentTotalBudget = scheduleToSync.campaign.spentTotalBudget,
                spentDailyBudget = scheduleToSync.adSet.spentDailyBudget
            )
            spentBudgetsRedisTemplate.opsForValue().set(spentBudgetKey, spentBudgets)
        }
    }

    private fun sendHistoryToPlatform(schedule: Schedule) {

    }


}
