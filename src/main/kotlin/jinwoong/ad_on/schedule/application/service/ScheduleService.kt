package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.exception.BusinessException
import jinwoong.ad_on.exception.ErrorCode
import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
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
    private val scheduleSyncService: ScheduleSyncService,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleService::class.java)
    }

    /**
     * 광고 플랫폼 서버가 전파하는 광고 정보를 저장
     */
    fun createSchedules(scheduleSaveRequest: ScheduleSaveRequest): ScheduleSaveResponse {
        val savedIds = createSchedulesInDB(scheduleSaveRequest)
        createBudgetCache(scheduleSaveRequest.schedules, savedIds)
        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    @Transactional
    fun createSchedulesInDB(scheduleSaveRequest: ScheduleSaveRequest): List<Long> {
        val savedIds = mutableListOf<Long>()

        scheduleSaveRequest.schedules.forEach { scheduleDTO ->
            val schedule = buildSchedule(scheduleDTO)
            val savedSchedule = scheduleRepository.save(schedule)
            savedIds.add(savedSchedule.id!!)
        }

        return savedIds
    }

    fun createBudgetCache(savedSchedules: List<ScheduleDTO>, savedIds: List<Long>) {
        savedSchedules.zip(savedIds).forEach { (scheduleDTO, id) ->
            val spentTotalBudgetKey = ScheduleRedisKey.SPENT_TOTAL_BUDGET_V1.key(id)
            val spentDailyBudgetKey = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.key(id)

            spentBudgetLongRedisTemplate.opsForValue().set(spentTotalBudgetKey, scheduleDTO.spentTotalBudget ?: 0L)
            spentBudgetLongRedisTemplate.opsForValue().set(spentDailyBudgetKey, scheduleDTO.spentDailyBudget ?: 0L)
        }
    }

    private fun buildSchedule(scheduleDTO: ScheduleDTO): Schedule {
        val spentTotalBudget = scheduleDTO.spentTotalBudget ?: 0L
        val spentDailyBudget = scheduleDTO.spentDailyBudget ?: 0L

        val campaign = Campaign(
            id = scheduleDTO.campaignId,
            totalBudget = scheduleDTO.totalBudget,
            spentTotalBudget = spentTotalBudget
        )

        val adSet = AdSet(
            id = scheduleDTO.adSetId,
            startDate = scheduleDTO.adSetStartDate,
            endDate = scheduleDTO.adSetEndDate,
            startTime = scheduleDTO.adSetStartTime,
            endTime = scheduleDTO.adSetEndTime,
            status = Status.valueOf(scheduleDTO.adSetStatus),
            dailyBudget = scheduleDTO.dailyBudget,
            unitCost = scheduleDTO.unitCost,
            paymentType = PaymentType.valueOf(scheduleDTO.paymentType),
            spentDailyBudget = spentDailyBudget
        )

        val creative = Creative(
            id = scheduleDTO.creativeId,
            status = Status.valueOf(scheduleDTO.creativeStatus),
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

    fun updateSchedules(scheduleUpdateRequest: ScheduleUpdateRequest): ScheduleUpdateResponse {
        val schedulesToUpdate = updateSchedulesInDB(scheduleUpdateRequest)

        val schedulesToSync = getSchedulesToSync(schedulesToUpdate)
        scheduleSyncService.syncCandidatesInRedis(schedulesToSync)

        val updatedIds = schedulesToUpdate.mapNotNull { it.id }
        log.info("스케줄 수정 완료, count: ${updatedIds.size}")
        return ScheduleUpdateResponse(updatedIds = updatedIds.toList())
    }

    @Transactional
    fun updateSchedulesInDB(scheduleUpdateRequest: ScheduleUpdateRequest): List<Schedule> {
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

        return schedulesToUpdate
    }


    /**
     * Campaign/AdSet 기준으로 갱신 대상 스케줄 조회
     */
    @Transactional(readOnly = true)
    fun getSchedulesToUpdate(schedule: Schedule): List<Schedule> =
        scheduleRepository.findAllByCampaignId(schedule.campaign.id)
            .union(scheduleRepository.findAllByAdSetId(schedule.adSet.id))
            .distinctBy { it.id }
            .filter { it.hasToPay() }
            .toList()

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

    private fun getSchedulesToSync(schedules: List<Schedule>): List<Schedule> {
        val candidateMap = scheduleSyncService.getCandidatesFromRedis().associateBy { it.id }
        return schedules.filter { candidateMap.containsKey(it.id) }
    }

}