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
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdServeServiceTest {

 @Mock lateinit var scheduleSyncService: ScheduleSyncService
 @Mock lateinit var spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>
 @Mock lateinit var scheduleRepository: ScheduleRepository
 @Mock lateinit var valueOperations: ValueOperations<String, SpentBudgets>

 private lateinit var adServeService: AdServeService

 @BeforeEach
 fun setup() {
  whenever(spentBudgetsRedisTemplate.opsForValue()).thenReturn(valueOperations)
  adServeService = AdServeService(scheduleSyncService, spentBudgetsRedisTemplate, scheduleRepository)
 }

 /** Mock Schedule 생성 */
 private fun createMockSchedule(id: Long, unitCost: Long = 100L): Schedule {
  val campaign = mock<Campaign> { on { spentTotalBudget } doReturn 0L }
  val adSet = mock<AdSet> { on { spentDailyBudget } doReturn 0L; on { this.unitCost } doReturn unitCost }
  val look = mock<Look> { on { imageURL } doReturn "http://image.com" }
  val creative = mock<Creative> { on { landingUrl } doReturn "http://example.com"; on { this.look } doReturn look }
  return mock<Schedule> {
   on { this.id } doReturn id
   on { this.campaign } doReturn campaign
   on { this.adSet } doReturn adSet
   on { this.creative } doReturn creative
   on { this.hasToPay() } doReturn true
  }
 }

 /** ---------------- TEST CASES ---------------- */

 @Test
 fun getServingAd_FromRedis() {
  // given
  val candidates = listOf(createMockSchedule(1L), createMockSchedule(2L))
  whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(candidates)

  // when
  val result = adServeService.getServingAdFromRedis(candidates, LocalTime.now())

  // then
  assertNotNull(result)
  assert(result.scheduleId in listOf(1L, 2L))
 }

 @Test
 fun getServingAd_Fallback() {
  // given
  whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(emptyList())
  val dbCandidates = listOf(createMockSchedule(3L), createMockSchedule(4L))
  whenever(scheduleSyncService.getFilteredCandidatesFromDB(any(), any())).thenReturn(dbCandidates)

  // when
  val result = adServeService.getServingAdFromDB(LocalDate.now(), LocalTime.now())

  // then
  assertNotNull(result)
  assert(result.scheduleId in listOf(3L, 4L))
  verify(scheduleSyncService).getFilteredCandidatesFromDB(any(), any())
 }

 @Test
 /* mock 객체이므로 테스트 로그에는 total=0, daily=0으로 나옴 */
 fun updateSpentBudgets() {
  // given
  val schedules = listOf(
   createMockSchedule(1L, 100L),
   createMockSchedule(2L, 200L)
  )
  val initialSpents = listOf(
   SpentBudgets(1L, 50L, 20L),
   SpentBudgets(2L, 30L, 10L)
  )
  val redisStore = mutableMapOf<String, SpentBudgets>().apply {
   initialSpents.forEach { this["spentBudgets:schedule:${it.scheduleId}"] = it }
  }

  mockRedis(redisStore)
  mockRepositoryForSchedules(schedules)
  whenever(scheduleSyncService.getFilteredCandidatesFromRedis(any())).thenReturn(schedules)

  // when
  adServeService.updateBudgetAfterServe(schedules[0], LocalTime.now())

  // then
  schedules.forEachIndexed { index, schedule ->
   val initial = initialSpents[index]
   val updated = redisStore["spentBudgets:schedule:${schedule.id}"]!!
   val expectedTotal = initial.spentTotalBudget + schedule.adSet.unitCost
   val expectedDaily = initial.spentDailyBudget + schedule.adSet.unitCost
   assertEquals(expectedTotal, updated.spentTotalBudget, "Schedule ${schedule.id} totalBudget mismatch")
   assertEquals(expectedDaily, updated.spentDailyBudget, "Schedule ${schedule.id} dailyBudget mismatch")
  }
 }

 /** Redis get/set을 map으로 모킹 */
 private fun mockRedis(store: MutableMap<String, SpentBudgets>) {
  whenever(valueOperations.get(any())).thenAnswer { store[it.arguments[0]] }
  whenever(valueOperations.set(any(), any())).thenAnswer {
   val key = it.arguments[0] as String
   val value = it.arguments[1] as SpentBudgets
   store[key] = value
   null
  }
 }

 /** Repository 모킹 (여러 스케줄 반환) */
 private fun mockRepositoryForSchedules(schedules: List<Schedule>) {
  whenever(scheduleRepository.findAllByCampaignId(any())).thenReturn(schedules)
  whenever(scheduleRepository.findAllByAdSetId(any())).thenReturn(schedules)
 }

}
