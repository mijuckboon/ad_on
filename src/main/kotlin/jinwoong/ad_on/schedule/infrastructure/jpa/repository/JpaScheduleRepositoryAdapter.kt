package jinwoong.ad_on.schedule.infrastructure.jpa.repository

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.jpa.entity.ScheduleEntity
import jinwoong.ad_on.schedule.infrastructure.mapper.ScheduleMapper
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class JpaScheduleRepositoryAdapter(
    private val jpaRepository: JpaScheduleRepository,
    private val scheduleMapper: ScheduleMapper
) : ScheduleRepository {

    override fun save(schedule: Schedule): Schedule {
        val entity: ScheduleEntity = scheduleMapper.toEntity(schedule)
        val saved: ScheduleEntity = jpaRepository.save(entity)
        return scheduleMapper.toDomain(saved)
    }

    override fun update(schedule: Schedule): Schedule {
        val entity: ScheduleEntity = scheduleMapper.toEntity(schedule)
        entity.updatedAt = LocalDateTime.now()
        val saved: ScheduleEntity = jpaRepository.save(entity)
        return scheduleMapper.toDomain(saved)
    }

    override fun findCandidates(today: LocalDate): List<Schedule> {
        val entities: List<ScheduleEntity> = jpaRepository.findCandidates(today)
        return entities.map { scheduleMapper.toDomain(it) }
    }

    override fun findAllByCampaignId(campaignId: Long): List<Schedule> {
        val entities: List<ScheduleEntity> = jpaRepository.findAllByCampaignId(campaignId)
        return entities.map { scheduleMapper.toDomain(it) }
    }

    override fun findAllByAdSetId(adSetId: Long): List<Schedule> {
        val entities: List<ScheduleEntity> = jpaRepository.findAllByAdSetId(adSetId)
        return entities.map { scheduleMapper.toDomain(it) }
    }

    override fun findAllByCreativeId(creativeId: Long): List<Schedule> {
        val entities: List<ScheduleEntity> = jpaRepository.findAllByCreativeId(creativeId)
        return entities.map { scheduleMapper.toDomain(it) }
    }
}
