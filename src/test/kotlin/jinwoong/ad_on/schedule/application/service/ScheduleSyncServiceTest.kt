package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleSyncServiceTest {

    @Mock
    lateinit var scheduleRedisTemplate: RedisTemplate<String, Schedule>
    @Mock
    lateinit var spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>
    @Mock
    lateinit var scheduleRepository: ScheduleRepository
    @Mock
    lateinit var scheduleValueOps: ValueOperations<String, Schedule>
    @Mock
    lateinit var spentValueOps: ValueOperations<String, SpentBudgets>
    @Mock
    lateinit var spentBudgetLongRedisTemplate: RedisTemplate<String, Long>
    @Mock
    lateinit var scheduleSyncService: ScheduleSyncService


    @BeforeEach
    fun setup() {
        whenever(scheduleRedisTemplate.opsForValue()).thenReturn(scheduleValueOps)
        whenever(spentBudgetsRedisTemplate.opsForValue()).thenReturn(spentValueOps)

        val longValueOps: ValueOperations<String, Long> = mock()
        whenever(longValueOps.get(any())).thenReturn(null)
        whenever(spentBudgetLongRedisTemplate.opsForValue()).thenReturn(longValueOps)

        scheduleSyncService = ScheduleSyncService(
                scheduleRedisTemplate,
                spentBudgetsRedisTemplate,
                scheduleRepository,
                spentBudgetLongRedisTemplate
            )

        // self-invocation용 proxy 설정
        scheduleSyncService.scheduleSyncServiceProxy = scheduleSyncService
    }

    /**------------------ TESTS ------------------*/

    @Test
    fun cacheCandidateTest() {
        // given
        val schedule = createSchedule(1)
        val spentBudgets = SpentBudgets(
            scheduleId = 1,
            spentTotalBudget = 50L,
            spentDailyBudget = 5L
        )
        whenever(scheduleRepository.findCandidates(any())).thenReturn(listOf(schedule))
        whenever(spentValueOps.get("spentBudgets:schedule:1")).thenReturn(spentBudgets)

        // when
        scheduleSyncService.cacheCandidates(LocalDate.now(), LocalTime.now())

        // then
        verify(scheduleValueOps).set(
            eq("candidate:schedule:1"),
            check {
                assertEquals(50L, it.campaign.spentTotalBudget)
                assertEquals(5L, it.adSet.spentDailyBudget)
            },
            any<Long>(),        // TTL
            any()               // TimeUnit
        )
    }

    @Test
    fun getCandidatesFromRedisTest() {
        // given
        val schedule = createSchedule(2)
        whenever(scheduleRedisTemplate.keys("candidate:schedule:*")).thenReturn(setOf("candidate:schedule:2"))
        whenever(scheduleValueOps.get("candidate:schedule:2")).thenReturn(schedule)

        // when
        val result = scheduleSyncService.getCandidatesFromRedis()

        // then
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    @Test
    fun filterCandidatesTest() {
        val schedule = createSchedule(3)
        // spentTotalBudget < totalBudget && spentDailyBudget < dailyBudget
        schedule.campaign.spentTotalBudget = 50
        schedule.adSet.spentDailyBudget = 5

        val filtered = scheduleSyncService.filterCandidates(listOf(schedule), LocalTime.now())
        assertTrue(filtered.contains(schedule))
    }

    @Test
    fun filterCandidatesTest_Filters() {
        val schedule = createSchedule(3)
        // spentTotalBudget < totalBudget && spentDailyBudget < dailyBudget
        schedule.campaign.spentTotalBudget = 1100
        schedule.adSet.spentDailyBudget = 5
        val filtered = scheduleSyncService.filterCandidates(listOf(schedule), LocalTime.now())
        assertTrue(!filtered.contains(schedule))
    }

    @Test
    fun updateBudgetsOfCandidatesTest() {
        val schedule1 = createSchedule(4)
        val schedule2 = createSchedule(5)
        val list = listOf(schedule1, schedule2)

        scheduleSyncService.updateBudgetsOfCandidates(list)

        verify(scheduleValueOps).set(
            eq("candidate:schedule:4"),
            eq(schedule1),
            any<Long>(),
            any()
        )
        verify(scheduleValueOps).set(
            eq("candidate:schedule:5"),
            eq(schedule2),
            any<Long>(),
            any()
        )
    }

    private fun createSchedule(id: Long, totalBudget: Long = 1000L, dailyBudget: Long = 100L): Schedule {
        val campaign = Campaign(
            id = id,
            totalBudget = totalBudget,
            spentTotalBudget = 0L
        )

        val adSet = AdSet(
            id = id,
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            startTime = LocalTime.MIN,
            endTime = LocalTime.MAX,
            status = Status.ON,
            dailyBudget = dailyBudget,
            unitCost = 10L,
            paymentType = PaymentType.CPC,
            spentDailyBudget = 0L
        )

        val creative = Creative(
            id = id,
            status = Status.ON,
            landingUrl = "http://example.com",
            look = Look(
                imageURL = "img.png",
                movieURL = null,
                logoURL = "logo.png",
                copyrightingTitle = "title",
                copyrightingSubtitle = "subtitle"
            )
        )

        return Schedule(id, campaign, adSet, creative)
    }
}
