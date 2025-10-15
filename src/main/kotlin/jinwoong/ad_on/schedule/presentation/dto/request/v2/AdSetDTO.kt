package jinwoong.ad_on.schedule.presentation.dto.request.v2

import jinwoong.ad_on.schedule.domain.aggregate.PaymentType
import jinwoong.ad_on.schedule.domain.aggregate.Status
import java.time.LocalDate
import java.time.LocalTime

// default 지정 없으면 백엔드 코드에서 전부 입력해줘야 함...
data class AdSetDTO (
    var id: Long,
    var startDate: LocalDate?,
    var endDate: LocalDate?,
    var startTime: LocalTime?,
    var endTime: LocalTime?,
    var status: Status?,
    var dailyBudget: Long?,
    var paymentType: PaymentType?,
    var unitCost: Long?,
)
