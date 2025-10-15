package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.exception.BusinessException
import jinwoong.ad_on.exception.ErrorCode
import jinwoong.ad_on.schedule.application.mapper.toDomain
import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleDTO
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleSaveRequest
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleSaveResponse
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleUpdateResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jinwoong.ad_on.schedule.presentation.dto.request.v1.AdSetDTO as AdSetDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v1.CampaignDTO as CampaignDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v1.CreativeDTO as CreativeDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v1.ScheduleUpdateRequest as ScheduleUpdateRequestv1
import jinwoong.ad_on.schedule.presentation.dto.request.v2.AdSetDTO as AdSetDTOv2
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CampaignDTO as CampaignDTOv2
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CreativeDTO as CreativeDTOv2
import jinwoong.ad_on.schedule.presentation.dto.request.v2.ScheduleUpdateRequest as ScheduleUpdateRequestv2

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSyncService: ScheduleSyncService,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ScheduleService::class.java)
    }

    @Lazy
    @Autowired
    lateinit var scheduleServiceProxy: ScheduleService

    /**
     * 광고 플랫폼 서버가 전파하는 광고 정보를 저장
     */
    fun createSchedules(scheduleSaveRequest: ScheduleSaveRequest): ScheduleSaveResponse {
        val savedIds = scheduleServiceProxy.createSchedulesInDB(scheduleSaveRequest)
        createBudgetCache(scheduleSaveRequest.schedules, savedIds)
        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    @Transactional
    fun createSchedulesInDB(scheduleSaveRequest: ScheduleSaveRequest): List<Long> {
        val savedIds = mutableListOf<Long>()

        scheduleSaveRequest.schedules.forEach { scheduleDTO ->
            val schedule = scheduleDTO.toDomain()
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

    fun updateSchedules(scheduleUpdateRequest: ScheduleUpdateRequestv1): ScheduleUpdateResponse {
        val schedulesToUpdate = scheduleServiceProxy.updateSchedulesInDB(scheduleUpdateRequest)

        val schedulesToSync = getSchedulesToSync(schedulesToUpdate)
        scheduleSyncService.syncCandidatesInRedis(schedulesToSync)

        val updatedIds = schedulesToUpdate.mapNotNull { it.id }
        log.info("스케줄 수정 완료, count: ${updatedIds.size}")
        return ScheduleUpdateResponse(updatedIds = updatedIds.toList())
    }

    fun updateSchedules(scheduleUpdateRequest: ScheduleUpdateRequestv2): ScheduleUpdateResponse {
        val schedulesToUpdate = scheduleServiceProxy.updateSchedulesInDB(scheduleUpdateRequest)

        val schedulesToSync = getSchedulesToSync(schedulesToUpdate)
        scheduleSyncService.syncCandidatesInRedis(schedulesToSync)

        val updatedIds = schedulesToUpdate.mapNotNull { it.id }
        log.info("스케줄 수정 완료, count: ${updatedIds.size}")
        return ScheduleUpdateResponse(updatedIds = updatedIds.toList())
    }

    @Transactional
    fun updateSchedulesInDB(scheduleUpdateRequest: ScheduleUpdateRequestv1): List<Schedule> {
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

    @Transactional
    fun updateSchedulesInDB(scheduleUpdateRequest: ScheduleUpdateRequestv2): List<Schedule> {
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

    /* 업데이트 로직 (v1) */
    private fun updateSchedulesByCampaign(campaignDTO: CampaignDTOv1): List<Schedule> {
        val schedules = scheduleRepository.findAllByCampaignId(campaignDTO.campaignId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateCampaign(campaignDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByAdSet(adSetDTO: AdSetDTOv1): List<Schedule> {
        val schedules = scheduleRepository.findAllByAdSetId(adSetDTO.adSetId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateAdSet(adSetDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByCreative(creativeDTO: CreativeDTOv1): List<Schedule> {
        val schedules = scheduleRepository.findAllByCreativeId(creativeDTO.creativeId)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateCreative(creativeDTO)
            scheduleRepository.update(it)
        }
    }

    /* 업데이트 로직 (v2) */
    private fun updateSchedulesByCampaign(campaignDTO: CampaignDTOv2): List<Schedule> {
        val schedules = scheduleRepository.findAllByCampaignId(campaignDTO.id)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateCampaign(campaignDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByAdSet(adSetDTO: AdSetDTOv2): List<Schedule> {
        val schedules = scheduleRepository.findAllByAdSetId(adSetDTO.id)
            .ifEmpty { throw BusinessException(ErrorCode.SCHEDULES_NOT_FOUND) }

        return schedules.map {
            it.updateAdSet(adSetDTO)
            scheduleRepository.update(it)
        }
    }

    private fun updateSchedulesByCreative(creativeDTO: CreativeDTOv2): List<Schedule> {
        val schedules = scheduleRepository.findAllByCreativeId(creativeDTO.id)
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