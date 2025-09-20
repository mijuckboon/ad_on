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

    private lateinit var scheduleSyncService: ScheduleSyncService

    @BeforeEach
    fun setup() {
        whenever(scheduleRedisTemplate.opsForValue()).thenReturn(scheduleValueOps)
        whenever(spentBudgetsRedisTemplate.opsForValue()).thenReturn(spentValueOps)
        scheduleSyncService = ScheduleSyncService(scheduleRedisTemplate, spentBudgetsRedisTemplate, scheduleRepository)
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
        verify(scheduleValueOps).set(eq("candidate:schedule:1"), check {
            assertEquals(50L, it.campaign.spentTotalBudget)
            assertEquals(5L, it.adSet.spentDailyBudget)
        })
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

        verify(scheduleValueOps).set(eq("candidate:schedule:4"), eq(schedule1))
        verify(scheduleValueOps).set(eq("candidate:schedule:5"), eq(schedule2))
    }

    @Test
    fun resetSpentDailyBudgetsInRedisTest() {
        val schedule = createSchedule(6)
        val spent = SpentBudgets(6, spentTotalBudget = 100, spentDailyBudget = 50)

        whenever(scheduleRedisTemplate.keys("candidate:schedule:*")).thenReturn(setOf("candidate:schedule:6"))
        whenever(scheduleValueOps.get("candidate:schedule:6")).thenReturn(schedule)
        whenever(spentBudgetsRedisTemplate.keys("spentBudgets:schedule:*")).thenReturn(setOf("spentBudgets:schedule:6"))
        whenever(spentValueOps.get("spentBudgets:schedule:6")).thenReturn(spent)

        scheduleSyncService.resetSpentDailyBudgetsInRedis()

        assertEquals(0L, schedule.adSet.spentDailyBudget)
        verify(scheduleValueOps).set(eq("candidate:schedule:6"), eq(schedule))
        verify(spentValueOps).set(eq("spentBudgets:schedule:6"), check {
            assertEquals(0L, it.spentDailyBudget)
        })
    }

    private fun createSchedule(id: Long, totalBudget: Long = 1000L, dailyBudget: Long = 100L): Schedule {
        val campaign = Campaign(
            campaignId = id,
            totalBudget = totalBudget,
            spentTotalBudget = 0L
        )

        val adSet = AdSet(
            adSetId = id,
            adSetStartDate = LocalDate.now(),
            adSetEndDate = LocalDate.now(),
            adSetStartTime = LocalTime.MIN,
            adSetEndTime = LocalTime.MAX,
            adSetStatus = Status.ON,
            dailyBudget = dailyBudget,
            unitCost = 10L,
            paymentType = PaymentType.CPC,
            spentDailyBudget = 0L
        )

        val creative = Creative(
            creativeId = id,
            creativeStatus = Status.ON,
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
