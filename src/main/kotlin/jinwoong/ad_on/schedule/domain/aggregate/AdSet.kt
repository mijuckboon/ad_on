package jinwoong.ad_on.schedule.domain.aggregate

import java.time.LocalDate
import java.time.LocalTime

data class AdSet(
    var adSetId: Long,
    var adSetStartDate: LocalDate,
    var adSetEndDate: LocalDate,
    var adSetStartTime: LocalTime,
    var adSetEndTime: LocalTime,
    var adSetStatus: Status,
    var dailyBudget: Long,
    var paymentType: PaymentType,
    var unitCost: Long = 0L,
    var spentDailyBudget: Long,
) {
    init {
        require(dailyBudget > spentDailyBudget) {
            ("일 예산은 오늘 사용한 소진액보다 커야합니다.")
        }
    }
}
