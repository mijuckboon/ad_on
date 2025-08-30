package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleSaveRequest
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleSaveResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleService::class.java)
    }

    /**
     * 광고 플랫폼 서버가 전파하는 광고 정보를 저장
     */
    @Transactional
    fun createSchedules(request: ScheduleSaveRequest): ScheduleSaveResponse {
        val savedIds = mutableListOf<Long>()

        request.schedules.forEach { dto ->
            val spentTotalBudget = dto.spentTotalBudget ?: 0L
            val spentDailyBudget = dto.spentDailyBudget ?: 0L

            val campaign = Campaign(
                campaignId = dto.campaignId,
                totalBudget = dto.totalBudget,
                spentTotalBudget = spentTotalBudget,
            )

            val adSet = AdSet(
                adSetId = dto.adSetId,
                adSetStartDate = dto.adSetStartDate,
                adSetEndDate = dto.adSetEndDate,
                adSetStartTime = dto.adSetStartTime,
                adSetEndTime = dto.adSetEndTime,
                adSetStatus = Status.valueOf(dto.adSetStatus),
                dailyBudget = dto.dailyBudget,
                unitCost = dto.unitCost,
                paymentType = PaymentType.valueOf(dto.paymentType),
                spentDailyBudget = spentDailyBudget,
            )

            val creative = Creative(
                creativeId = dto.creativeId,
                creativeStatus = Status.valueOf(dto.creativeStatus),
                landingUrl = dto.landingUrl,
                look = Look(
                    creativeImage = dto.creativeImage,
                    creativeMovie = dto.creativeMovie,
                    creativeLogo = dto.creativeLogo,
                    copyrightingTitle = dto.copyrightingTitle,
                    copyrightingSubtitle = dto.copyrightingSubtitle,
                )
            )

            val schedule = Schedule(
                campaign = campaign,
                adSet = adSet,
                creative = creative,
            )

            val savedSchedule = scheduleRepository.save(schedule)
            savedIds.add(savedSchedule.id!!)

            /* Redis에 예산 정보 저장 */
            val budgetKey = "budget:schedule:${savedSchedule.id}"
            val initialBudget = SpentBudgets(
                scheduleId = savedSchedule.id!!,
                spentTotalBudget = spentTotalBudget,
                spentDailyBudget = spentDailyBudget
            )
            spentBudgetsRedisTemplate.opsForValue().set(budgetKey, initialBudget)
        }

        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    fun getCandidatesFromDB(today: LocalDate): List<Schedule> {
        /* DB 조회: 비교적 실시간성이 낮은 값을 미리 필터링 */
        return scheduleRepository.findCandidates(today)
    }

}