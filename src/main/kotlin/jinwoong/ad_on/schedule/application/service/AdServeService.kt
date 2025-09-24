package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.presentation.dto.response.ServingAdDTO
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class AdServeService(
    private val scheduleSyncService: ScheduleSyncService,
    private val budgetService: BudgetService,
) {
    /**
     * 서빙할 광고를 Redis에서 선택
     */
    fun getServingAd(today: LocalDate, currentTime: LocalTime): ServingAdDTO? {
        val filteredCandidates = scheduleSyncService.getFilteredCandidatesFromRedis(currentTime)
        val servingAd = getServingAdFromRedis(filteredCandidates, currentTime)
        return servingAd
    }

    fun getServingAdFromRedis(candidates: List<Schedule>, currentTime: LocalTime): ServingAdDTO? {
        if (candidates.isEmpty()) return null
        return chooseRandomAd(candidates, currentTime)
    }

    fun chooseRandomAd(candidates: List<Schedule>, currentTime: LocalTime): ServingAdDTO {
        val servingAd = candidates.random()
        val budgetUpdatedAd = budgetService.updateBudgetAfterServe(servingAd, currentTime)
        sendHistoryToPlatform(budgetUpdatedAd)
        return ServingAdDTO.from(budgetUpdatedAd)
    }

    private fun sendHistoryToPlatform(schedule: Schedule) {

    }
}
