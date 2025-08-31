package jinwoong.ad_on.schedule.domain.repository

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ScheduleRepository {
    fun save(schedule: Schedule): Schedule

    fun update(schedule: Schedule): Schedule

    fun findCandidates(today: LocalDate): List<Schedule>

    fun findAllByCampaignId(campaignId: Long): List<Schedule>

    fun findAllByAdSetId(adSetId: Long): List<Schedule>

    fun findAllByCreativeId(creativeId: Long): List<Schedule>
}