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
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdServeServiceTest {

 @Mock
 lateinit var scheduleSyncService: ScheduleSyncService

 @Mock
 lateinit var spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>

 @Mock
 lateinit var scheduleRepository: ScheduleRepository

 @Mock
 lateinit var valueOperations: ValueOperations<String, SpentBudgets>

 private lateinit var adServeService: AdServeService

 @BeforeEach
 fun setup() {
  // Redis opsForValue()가 valueOperations mock을 반환
  whenever(spentBudgetsRedisTemplate.opsForValue()).thenReturn(valueOperations)
  whenever(valueOperations.get(any())).thenReturn(SpentBudgets(1L, 0L, 0L))
  adServeService = AdServeService(scheduleSyncService, spentBudgetsRedisTemplate, scheduleRepository)
 }

 private fun createSampleSchedule(id: Long): Schedule {
  val campaign = Campaign(campaignId = id, totalBudget = 1000L, spentTotalBudget = 0L)
  val adSet = AdSet(
   adSetId = id,
   adSetStartDate = LocalDate.now(),
   adSetEndDate = LocalDate.now().plusDays(1),
   adSetStartTime = LocalTime.MIN,
   adSetEndTime = LocalTime.MAX,
   adSetStatus = Status.ON,
   dailyBudget = 500L,
   unitCost = 100L,
   paymentType = PaymentType.CPM,
   spentDailyBudget = 0L
  )
  val creative = Creative(
   creativeId = id,
   creativeStatus = Status.ON,
   landingUrl = "http://example.com",
   look = Look(
    imageURL = "http://image.com",
    movieURL = "http://movie.com",
    logoURL = "http://logo.com",
    copyrightingTitle = "Title",
    copyrightingSubtitle = "Subtitle"
   )
  )
  return Schedule(id = id, campaign = campaign, adSet = adSet, creative = creative)
 }

 @Test
 fun getServingAd_FromRedis() {
  // given
  val schedules = listOf(createSampleSchedule(1L), createSampleSchedule(2L))
  whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(schedules)

  // when
  val result = adServeService.getServingAd(LocalDate.now(), LocalTime.now())

  // then
  assertNotNull(result)
  verify(scheduleSyncService, never()).getFilteredCandidatesFromDB(any(), any())
 }

 @Test
 fun getServingAd_Fallback() {
  // given
  whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(emptyList())
  whenever(scheduleSyncService.getFilteredCandidatesFromDB(any(), any()))
   .thenReturn(listOf(createSampleSchedule(3L), createSampleSchedule(4L)))

  // when
  val result = adServeService.getServingAd(LocalDate.now(), LocalTime.now())

  // then
  assertNotNull(result)
  verify(scheduleSyncService).getFilteredCandidatesFromDB(any(), any())
 }
 
}
