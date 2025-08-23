package jinwoong.ad_on.schedule.domain.repository

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import java.time.LocalDate

interface ScheduleRepository {
    fun save(schedule: Schedule): Schedule

    fun findCandidates(today: LocalDate): List<Schedule>

    fun resetSpentDailyBudgets()
}