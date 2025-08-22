package jinwoong.ad_on.schedule.infrastructure.jpa

import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JpaScheduleRepository: ScheduleRepository, JpaRepository<Schedule, Long> {

    @Query("""SELECT s FROM Schedule s
            WHERE s.adSetStartDate <= :today 
            AND s.adSetEndDate >= :today
            AND s.adSetStatus = 'ON'
            AND s.creativeStatus = 'ON'
            """)
    override fun findCandidates(today: LocalDate): List<Schedule>
}