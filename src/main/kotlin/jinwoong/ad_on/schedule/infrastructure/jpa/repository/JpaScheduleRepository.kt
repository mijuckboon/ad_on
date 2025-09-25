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
    WHERE s.adSet.startDate <= :today
      AND s.adSet.endDate >= :today
      AND s.adSet.status = 'ON'
      AND s.creative.status = 'ON'
"""
    )
    fun findCandidates(today: LocalDate): List<ScheduleEntity>

    @Query(
        """
    SELECT s FROM ScheduleEntity s
    WHERE s.campaign.id = :campaignId
"""
    )
    fun findAllByCampaignId(campaignId: Long): List<ScheduleEntity>

    @Query(
        """
    SELECT s FROM ScheduleEntity s
    WHERE s.adSet.id = :adSetId
"""
    )
    fun findAllByAdSetId(adSetId: Long): List<ScheduleEntity>

    @Query(
        """
    SELECT s FROM ScheduleEntity s
    WHERE s.creative.id = :creativeId
"""
    )
    fun findAllByCreativeId(creativeId: Long): List<ScheduleEntity>

}