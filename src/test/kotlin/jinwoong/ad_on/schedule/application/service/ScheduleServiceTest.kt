package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleDTO
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleSaveRequest
import jinwoong.ad_on.schedule.presentation.dto.request.v1.CampaignDTO
import jinwoong.ad_on.schedule.presentation.dto.request.v1.ScheduleUpdateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceTest {
    private val scheduleRepository: ScheduleRepository = mock()
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long> = mock()
    private val scheduleSyncService: ScheduleSyncService = mock()
    private val valueOps: ValueOperations<String, Long> = mock()

    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService = ScheduleService(scheduleRepository, scheduleSyncService, spentBudgetLongRedisTemplate)
        scheduleService.scheduleServiceProxy = scheduleService

        // RedisTemplate.opsForValue() mocking
        whenever(spentBudgetLongRedisTemplate.opsForValue()).thenReturn(valueOps)
    }

    @Test
    fun createSchedulesTest() {
        // given
        val request = ScheduleSaveRequest(schedules = listOf(createScheduleDTO()))
        val savedSchedule = createSavedSchedule(id = 99L)

        whenever(scheduleRepository.save(any())).thenReturn(savedSchedule)

        // when
        val response = scheduleService.createSchedules(request)

        // then
        assertEquals(listOf(99L), response.savedIds)

        // Redis set 호출 확인 (verify: 1번 호출)
        verify(valueOps).set(eq("spentTotalBudget_v1:schedule:99"), any())
        verify(valueOps).set(eq("spentDailyBudget_v1:schedule:99"), any())
    }

    @Test
    fun updateSchedulesTest() {
        // given
        val existingSchedule = createSavedSchedule(id = 101L)
        whenever(scheduleRepository.findAllByCampaignId(1L)).thenReturn(listOf(existingSchedule))
        whenever(scheduleRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(scheduleSyncService.getCandidatesFromRedis()).thenReturn(listOf(existingSchedule))

        val request = ScheduleUpdateRequest(campaign = CampaignDTO(campaignId = 1L, totalBudget = 2000L))

        // when
        val response = scheduleService.updateSchedules(request)

        // then
        assertEquals(listOf(101L), response.updatedIds)

        // 업데이트된 객체 내용 확인
        val captor = argumentCaptor<Schedule>()
        verify(scheduleRepository).update(captor.capture()) // 넘어간 객체 기록
        val updatedSchedule = captor.firstValue // secondValue, allValues, ...

        // 실제 budget이 2000으로 업데이트되었는지 확인
        assertEquals(2000L, updatedSchedule.campaign.totalBudget)
        verify(scheduleSyncService).syncCandidatesInRedis(any())
    }

    /** Helper Methods **/

    private fun createSavedSchedule(id: Long): Schedule =
        Schedule(
            campaign = Campaign(
                id = 1L,
                totalBudget = 1000L,
                spentTotalBudget = 100L
            ),
            adSet = AdSet(
                id = 2L,
                startDate = LocalDate.parse("2025-09-01"),
                endDate = LocalDate.parse("2025-09-30"),
                startTime = LocalTime.parse("00:00"),
                endTime = LocalTime.parse("23:59"),
                status = Status.ON,
                dailyBudget = 500L,
                unitCost = 10L,
                paymentType = PaymentType.CPC,
                spentDailyBudget = 50L
            ),
            creative = Creative(
                id = 3L,
                status = Status.ON,
                landingUrl = "http://test.com",
                look = Look(
                    imageURL = "img.png",
                    movieURL = null,
                    logoURL = "logo.png",
                    copyrightingTitle = "title",
                    copyrightingSubtitle = "subtitle"
                )
            )
        ).apply { this.id = id }

    private fun createScheduleDTO(): ScheduleDTO =
        ScheduleDTO(
            campaignId = 1L,
            totalBudget = 1000L,
            spentTotalBudget = 100L,
            adSetId = 2L,
            adSetStartDate = LocalDate.parse("2025-09-01"),
            adSetEndDate = LocalDate.parse("2025-09-30"),
            adSetStartTime = LocalTime.parse("00:00"),
            adSetEndTime = LocalTime.parse("23:59"),
            adSetStatus = "ON",
            dailyBudget = 500L,
            unitCost = 10L,
            paymentType = "CPC",
            spentDailyBudget = 50L,
            creativeId = 3L,
            creativeStatus = "ON",
            landingUrl = "http://test.com",
            creativeImage = "img.png",
            creativeMovie = null,
            creativeLogo = "logo.png",
            copyrightingTitle = "title",
            copyrightingSubtitle = "subtitle"
        )
}
