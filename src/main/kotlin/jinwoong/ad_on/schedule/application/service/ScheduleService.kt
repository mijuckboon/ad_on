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
            val schedule = buildSchedule(scheduleDTO)

            val savedSchedule = scheduleRepository.save(schedule)
            savedIds.add(savedSchedule.id!!)

            /* Redis에 예산 정보 저장 */
            val spentBudgetsKey = "spentBudgets:schedule:${savedSchedule.id}"
            val initialBudget = SpentBudgets(
                scheduleId = savedSchedule.id!!,
                spentTotalBudget = scheduleDTO.spentTotalBudget ?: 0L,
                spentDailyBudget = scheduleDTO.spentDailyBudget ?: 0L
            )
            spentBudgetsRedisTemplate.opsForValue().set(spentBudgetsKey, initialBudget)
        }

        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    private fun buildSchedule(scheduleDTO: ScheduleDTO): Schedule {
        val spentTotalBudget = scheduleDTO.spentTotalBudget ?: 0L
        val spentDailyBudget = scheduleDTO.spentDailyBudget ?: 0L

        val campaign = Campaign(
            campaignId = scheduleDTO.campaignId,
            totalBudget = scheduleDTO.totalBudget,
            spentTotalBudget = spentTotalBudget
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
            spentDailyBudget = spentDailyBudget
        )

        val creative = Creative(
            creativeId = scheduleDTO.creativeId,
            creativeStatus = Status.valueOf(scheduleDTO.creativeStatus),
            landingUrl = scheduleDTO.landingUrl,
            look = Look(
                imageURL = scheduleDTO.creativeImage,
                movieURL = scheduleDTO.creativeMovie,
                logoURL = scheduleDTO.creativeLogo,
                copyrightingTitle = scheduleDTO.copyrightingTitle,
                copyrightingSubtitle = scheduleDTO.copyrightingSubtitle
            )
        )

        return Schedule(campaign = campaign, adSet = adSet, creative = creative)
    }

    @Transactional
    fun updateSchedules(scheduleUpdateRequest: ScheduleUpdateRequest): ScheduleUpdateResponse {
        val candidateMap = scheduleSyncService.getCandidatesFromRedis().associateBy { it.id }

        val schedulesToUpdate = mutableListOf<Schedule>()

        scheduleUpdateRequest.campaign?.let { campaignDTO ->
            schedulesToUpdate += updateSchedulesByCampaign(campaignDTO)
        }
        scheduleUpdateRequest.adSet?.let { adSetDTO ->
            schedulesToUpdate += updateSchedulesByAdSet(adSetDTO)
        }
        scheduleUpdateRequest.creative?.let { creativeDTO ->
            schedulesToUpdate += updateSchedulesByCreative(creativeDTO)
        }

        val schedulesToSync = schedulesToUpdate.filter {
            candidateMap.containsKey(it.id)
        }
        val updatedIds = schedulesToUpdate.mapNotNull { it.id }

        scheduleSyncService.syncCandidatesInRedis(schedulesToSync)
        log.info("스케줄 수정 완료, count: ${updatedIds.size}")
        return ScheduleUpdateResponse(updatedIds = updatedIds.toList())
    }

    /* 업데이트 로직 */
    private fun updateSchedulesByCampaign(campaignDTO: CampaignDTO): List<Schedule> {
        val schedules = scheduleRepository.findAllByCampaignId(campaignDTO.campaignId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateCampaign(campaignDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByAdSet(adSetDTO: AdSetDTO): List<Schedule> {
        val schedules = scheduleRepository.findAllByAdSetId(adSetDTO.adSetId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateAdSet(adSetDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByCreative(creativeDTO: CreativeDTO): List<Schedule> {
        val schedules = scheduleRepository.findAllByCreativeId(creativeDTO.creativeId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateCreative(creativeDTO)
            scheduleRepository.update(it)
        }
    }



}