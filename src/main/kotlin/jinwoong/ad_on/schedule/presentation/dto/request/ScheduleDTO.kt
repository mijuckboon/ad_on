package jinwoong.ad_on.schedule.presentation.dto.request

import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import java.time.LocalDate
import java.time.LocalTime

@Validated
data class ScheduleDTO(
    val campaignId: Long,
    val adSetId: Long,
    val creativeId: Long,
    val adSetStartDate: LocalDate,
    val adSetEndDate: LocalDate,
    val adSetStartTime: LocalTime,
    val adSetEndTime: LocalTime,
    @Min(0)
    val totalBudget: Long,
    @Min(0)
    val spentTotalBudget: Long? = null,
    @Min(0)
    val dailyBudget: Long,
    @Min(0)
    val spentDailyBudget: Long? = null,
    val paymentType: String,
    @Min(0)
    val unitCost: Long,
    val creativeImage: String? = null,
    val creativeMovie: String? = null,
    val creativeLogo: String? = null,
    val copyrightingTitle: String? = null,
    val copyrightingSubtitle: String? = null,
    val adSetStatus: String,
    val creativeStatus: String,
    val landingUrl: String
)
