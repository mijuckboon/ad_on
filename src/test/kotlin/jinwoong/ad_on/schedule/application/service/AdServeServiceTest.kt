package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import jinwoong.ad_on.schedule.infrastructure.redis.ScheduleRedisKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdServeServiceTest {

    @Mock lateinit var scheduleSyncService: ScheduleSyncService
    @Mock lateinit var spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>
    @Mock lateinit var spentBudgetLongRedisTemplate: RedisTemplate<String, Long>
    @Mock lateinit var spentBudgetsValueOps: ValueOperations<String, SpentBudgets>
    @Mock lateinit var longValueOps: ValueOperations<String, Long>
    @Mock lateinit var scheduleService: ScheduleService

    private lateinit var budgetService: BudgetService
    private lateinit var adServeService: AdServeService

    private val longStore = mutableMapOf<String, Long>()

    @BeforeEach
    fun setup() {
        whenever(spentBudgetsRedisTemplate.opsForValue()).thenReturn(spentBudgetsValueOps)
        whenever(spentBudgetLongRedisTemplate.opsForValue()).thenReturn(longValueOps)

        whenever(longValueOps.setIfAbsent(any(), any())).thenAnswer { inv ->
            val key = inv.arguments[0] as String
            val value = inv.arguments[1] as Long
            longStore.putIfAbsent(key, value)
            longStore[key] == value
        }

        whenever(longValueOps.increment(any(), any<Long>())).thenAnswer { inv ->
            val key = inv.arguments[0] as String
            val delta = inv.arguments[1] as Long
            val newVal = (longStore[key] ?: 0L) + delta
            longStore[key] = newVal
            newVal
        }

        whenever(scheduleService.getSchedulesToUpdate(any())).thenReturn(emptyList())
        whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(emptyList())

        budgetService = BudgetService(
            spentBudgetsRedisTemplate = spentBudgetsRedisTemplate,
            spentBudgetLongRedisTemplate = spentBudgetLongRedisTemplate,
            scheduleSyncService = scheduleSyncService,
            scheduleService = scheduleService
        )

        adServeService = AdServeService(
            scheduleSyncService = scheduleSyncService,
            budgetService = budgetService
        )
    }

    /** 실제 도메인 엔티티 생성 */
    private fun createSchedule(id: Long, unitCost: Long = 100L): Schedule {
        val campaign = Campaign(
            id = id,
            totalBudget = 1000L,
            spentTotalBudget = 0L
        )
        val adSet = AdSet(
            id = id,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            startTime = LocalTime.MIN,
            endTime = LocalTime.MAX,
            status = Status.ON,
            dailyBudget = 500L,
            unitCost = unitCost,
            paymentType = PaymentType.CPC,
            spentDailyBudget = 0L
        )
        val creative = Creative(
            id = id,
            status = Status.ON,
            landingUrl = "http://example.com",
            look = Look(
                imageURL = "http://image.com",
                movieURL = null,
                logoURL = "logo.png",
                copyrightingTitle = "title",
                copyrightingSubtitle = "subtitle"
            )
        )
        return Schedule(id, campaign, adSet, creative)
    }

    /** ---------------- TEST CASES ---------------- */

    @Test
    fun getServingAd_FromRedis() {
        // given
        val candidates = listOf(createSchedule(1L), createSchedule(2L))
        whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(candidates)

        // when
        val result = adServeService.getServingAdFromRedis(candidates, LocalTime.now())

        // then
        assertNotNull(result)
        assert(result.scheduleId in listOf(1L, 2L))
    }

    @Test
    fun updateSpentBudgets_ShouldUpdateRedisValues() {
        // given
        val schedule = createSchedule(1L, 100L)
        whenever(scheduleService.getSchedulesToUpdate(any())).thenReturn(listOf(schedule))

        val totalKey = ScheduleRedisKey.SPENT_TOTAL_BUDGET_V1.key(schedule.id!!)
        val dailyKey = ScheduleRedisKey.SPENT_DAILY_BUDGET_V1.key(schedule.id!!)
        longStore[totalKey] = 50L
        longStore[dailyKey] = 20L

        budgetService.updateBudgetAfterServe(schedule, LocalTime.now())

        // then
        assertEquals(150L, longStore[totalKey])
        assertEquals(120L, longStore[dailyKey])
    }
}
