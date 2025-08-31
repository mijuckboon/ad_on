package jinwoong.ad_on.schedule.presentation.dto.request

import jinwoong.ad_on.schedule.domain.aggregate.PaymentType
import jinwoong.ad_on.schedule.domain.aggregate.Status
import java.time.LocalDate
import java.time.LocalTime

data class AdSetDTO (
    var adSetId: Long,
    var adSetStartDate: LocalDate?,
    var adSetEndDate: LocalDate?,
    var adSetStartTime: LocalTime?,
    var adSetEndTime: LocalTime?,
    var adSetStatus: Status?,
    var dailyBudget: Long?,
    var paymentType: PaymentType?,
    var unitCost: Long?,
)
