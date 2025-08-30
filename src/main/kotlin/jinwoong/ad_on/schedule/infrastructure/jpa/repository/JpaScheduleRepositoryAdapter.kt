package jinwoong.ad_on.schedule.infrastructure.jpa.repository

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.jpa.entity.ScheduleEntity
import jinwoong.ad_on.schedule.infrastructure.mapper.ScheduleMapper
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class JpaScheduleRepositoryAdapter(
    private val jpaRepository: JpaScheduleRepository
) : ScheduleRepository {

    override fun save(schedule: Schedule): Schedule {
        val entity: ScheduleEntity = ScheduleMapper.toEntity(schedule)
        val saved: ScheduleEntity = jpaRepository.save(entity)
        return ScheduleMapper.toDomain(saved)
    }

    override fun findCandidates(today: LocalDate): List<Schedule> {
        val entities: List<ScheduleEntity> = jpaRepository.findCandidates(today)
        return entities.map { ScheduleMapper.toDomain(it) }
    }
}
