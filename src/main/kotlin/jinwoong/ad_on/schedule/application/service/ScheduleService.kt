package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.exception.BusinessException
import jinwoong.ad_on.exception.ErrorCode
import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import jinwoong.ad_on.schedule.presentation.dto.request.*
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleSaveResponse
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleUpdateResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val scheduleRedisTemplate: RedisTemplate<String, Schedule>,
    private val scheduleSyncService: ScheduleSyncService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleService::class.java)
    }

    /**
     * 광고 플랫폼 서버가 전파하는 광고 정보를 저장
     */
    @Transactional
    fun createSchedules(scheduleSaveRequest: ScheduleSaveRequest): ScheduleSaveResponse {
        val savedIds = mutableListOf<Long>()

        scheduleSaveRequest.schedules.forEach { scheduleDTO ->
            val spentTotalBudget = scheduleDTO.spentTotalBudget ?: 0L
            val spentDailyBudget = scheduleDTO.spentDailyBudget ?: 0L

            val campaign = Campaign(
                campaignId = scheduleDTO.campaignId,
                totalBudget = scheduleDTO.totalBudget,
                spentTotalBudget = spentTotalBudget,
            )

            val adSet = AdSet(
                adSetId = scheduleDTO.adSetId,
                adSetStartDate = scheduleDTO.adSetStartDate,
                adSetEndDate = scheduleDTO.adSetEndDate,
                adSetStartTime = scheduleDTO.adSetStartTime,
                adSetEndTime = scheduleDTO.adSetEndTime,
                adSetStatus = Status.valueOf(scheduleDTO.adSetStatus),
                dailyBudget = scheduleDTO.dailyBudget,
                unitCost = scheduleDTO.unitCost,
                paymentType = PaymentType.valueOf(scheduleDTO.paymentType),
                spentDailyBudget = spentDailyBudget,
            )

            val creative = Creative(
                creativeId = scheduleDTO.creativeId,
                creativeStatus = Status.valueOf(scheduleDTO.creativeStatus),
                landingUrl = scheduleDTO.landingUrl,
                look = Look(
                    creativeImage = scheduleDTO.creativeImage,
                    creativeMovie = scheduleDTO.creativeMovie,
                    creativeLogo = scheduleDTO.creativeLogo,
                    copyrightingTitle = scheduleDTO.copyrightingTitle,
                    copyrightingSubtitle = scheduleDTO.copyrightingSubtitle,
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
            val spentBudgetsKey = "spentBudgets:schedule:${savedSchedule.id}"
            val initialBudget = SpentBudgets(
                scheduleId = savedSchedule.id!!,
                spentTotalBudget = spentTotalBudget,
                spentDailyBudget = spentDailyBudget
            )
            spentBudgetsRedisTemplate.opsForValue().set(spentBudgetsKey, initialBudget)
        }

        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    @Transactional
    fun updateSchedules(scheduleUpdateRequest: ScheduleUpdateRequest): ScheduleUpdateResponse {
        val updatedIds = mutableListOf<Long>()
        val candidateMap = scheduleSyncService.getCandidatesFromRedis().associateBy { it.id }

        scheduleUpdateRequest.campaign?.let { campaignDTO ->
            updatedIds += updateSchedulesByCampaign(campaignDTO, candidateMap)
        }
        scheduleUpdateRequest.adSet?.let { adSetDTO ->
            updatedIds += updateSchedulesByAdSet(adSetDTO, candidateMap)
        }
        scheduleUpdateRequest.creative?.let { creativeDTO ->
            updatedIds += updateSchedulesByCreative(creativeDTO, candidateMap)
        }

        return ScheduleUpdateResponse(updatedIds = updatedIds.toList())
    }

    /* 업데이트 로직 */
    private fun updateSchedulesByCampaign(campaignDTO: CampaignDTO, candidateMap: Map<Long?, Schedule>): Set<Long> {
        val schedules = scheduleRepository.findAllByCampaignId(campaignDTO.campaignId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map { schedule ->
            schedule.updateCampaign(campaignDTO)
            saveScheduleAndSyncRedis(schedule, candidateMap)
            schedule.id!!
        }.toSet()
    }

    private fun updateSchedulesByAdSet(adSetDTO: AdSetDTO, candidateMap: Map<Long?, Schedule>): Set<Long> {
        val schedules = scheduleRepository.findAllByAdSetId(adSetDTO.adSetId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map { schedule ->
            schedule.updateAdSet(adSetDTO)
            saveScheduleAndSyncRedis(schedule, candidateMap)
            schedule.id!!
        }.toSet()
    }

    private fun updateSchedulesByCreative(creativeDTO: CreativeDTO, candidateMap: Map<Long?, Schedule>): Set<Long> {
        val schedules = scheduleRepository.findAllByCreativeId(creativeDTO.creativeId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map { schedule ->
            schedule.updateCreative(creativeDTO)
            saveScheduleAndSyncRedis(schedule, candidateMap)
            schedule.id!!
        }.toSet()
    }

    /* 저장 */
    private fun saveScheduleAndSyncRedis(schedule: Schedule, candidateMap: Map<Long?, Schedule>) {
        updateScheduleInDB(schedule)
        if (schedule.id != null && candidateMap.containsKey(schedule.id)) {
            syncRedis(schedule)
        }
    }

    private fun updateScheduleInDB(schedule: Schedule) {
        scheduleRepository.update(schedule)
    }

    private fun syncRedis(schedule: Schedule) {
        val candidateKey = "candidate:schedule:${schedule.id}"
        scheduleRedisTemplate.opsForValue().set(candidateKey, schedule)
    }

}