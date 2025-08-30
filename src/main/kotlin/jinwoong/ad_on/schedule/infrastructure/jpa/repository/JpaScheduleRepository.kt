package jinwoong.ad_on.schedule.infrastructure.jpa.repository

import jinwoong.ad_on.schedule.infrastructure.jpa.entity.ScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JpaScheduleRepository: JpaRepository<ScheduleEntity, Long> {

    @Query(
        """
    SELECT s FROM ScheduleEntity s
    WHERE s.adSet.adSetStartDate <= :today
      AND s.adSet.adSetEndDate >= :today
      AND s.adSet.adSetStatus = 'ON'
      AND s.creative.creativeStatus = 'ON'
"""
    )
    fun findCandidates(today: LocalDate): List<ScheduleEntity>

}