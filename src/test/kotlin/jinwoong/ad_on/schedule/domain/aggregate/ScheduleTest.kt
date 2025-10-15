package jinwoong.ad_on.schedule.domain.aggregate

import jinwoong.ad_on.schedule.presentation.dto.request.v2.AdSetDTO
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CampaignDTO
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CreativeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTest {

 private fun sampleCampaign() = Campaign(id = 1L, totalBudget = 1000L, spentTotalBudget = 200L)

 private fun sampleAdSet() = AdSet(
  id = 1L,
  startDate = LocalDate.now(),
  endDate = LocalDate.now().plusDays(1),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(18, 0),
  status = Status.ON,
  dailyBudget = 500L,
  paymentType = PaymentType.CPM,
  unitCost = 100L,
  spentDailyBudget = 100L
 )

 private fun sampleCreative() = Creative(
  id = 1L,
  look = Look(imageURL = "img.png"),
  landingUrl = "https://example.com",
  status = Status.ON
 )

 @Test
 fun adSetUpdateDailyBudget_shouldThrowExceptionWhenLessThanSpentDailyBudget() {
  val adSet = sampleAdSet()
  assertThrows<IllegalArgumentException> {
   adSet.updateDailyBudget(50L)
  }
 }

 @Test
 fun campaignUpdateTotalBudget_shouldThrowExceptionWhenLessThanSpentTotalBudget() {
  val campaign = sampleCampaign()
  assertThrows<IllegalArgumentException> {
   campaign.updateTotalBudget(100L)
  }
 }

 @Test
 fun isActiveByTime_shouldReturnTrueWhenCurrentTimeWithinAdSetTime() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  val currentTime = LocalTime.of(10, 0)
  assertTrue(schedule.isActiveByTime(currentTime))
 }

 @Test
 fun isActiveByTime_shouldReturnFalseWhenCurrentTimeOutsideAdSetTime() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  val currentTime = LocalTime.of(20, 0)
  assertFalse(schedule.isActiveByTime(currentTime))
 }

 @Test
 fun hasRestBudget_shouldReturnTrueWhenBudgetIsAvailable() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  assertTrue(schedule.hasRestBudget())
 }

 @Test
 fun updateCampaign_shouldUpdateTotalBudget() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  schedule.updateCampaign(CampaignDTO(id = 1, totalBudget = 1500L))
  assertEquals(1500L, schedule.campaign.totalBudget)
 }

 @Test
 fun updateAdSet_shouldUpdateDailyBudgetAndTime() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  val newStartTime = LocalTime.of(8, 0)
  schedule.updateAdSet(
   AdSetDTO(
    id = 1,
    startDate = null,
    endDate = null,
    startTime = newStartTime,
    endTime = null,
    status = null,
    dailyBudget = 300L,
    paymentType = null,
    unitCost = null
   )
  )
  assertEquals(300L, schedule.adSet.dailyBudget)
  assertEquals(newStartTime, schedule.adSet.startTime)
 }

 @Test
 fun updateCreative_shouldUpdateLookAndLandingUrl() {
  val schedule = Schedule(1L, sampleCampaign(), sampleAdSet(), sampleCreative())
  schedule.updateCreative(
   CreativeDTO(
    id = 1L,
    landingUrl = "https://new.com",
    creativeStatus = null,
    imageURL = "new.png",
    movieURL = null,
    logoURL = null,
    copyrightingTitle = null,
    copyrightingSubtitle = null
   )
  )
  assertEquals("new.png", schedule.creative.look.imageURL)
  assertEquals("https://new.com", schedule.creative.landingUrl)
 }
}
