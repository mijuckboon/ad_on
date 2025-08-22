package jinwoong.ad_on.schedule.presentation.dto.request

import java.time.LocalDate
import java.time.LocalTime

data class ScheduleDTO(
    val creativeId: Long,
    val adSetStartDate: LocalDate,
    val adSetEndDate: LocalDate,
    val adSetStartTime: LocalTime,
    val adSetEndTime: LocalTime,
    val totalBudget: Long,
    val dailyBudget: Long,
    val paymentType: String,
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
