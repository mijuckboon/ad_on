package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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
        require(dailyBudget >= spentDailyBudget) {
            ("일 예산은 오늘 사용한 소진액보다 작을 수 없습니다. spentDaily=$spentDailyBudget, daily=$dailyBudget")
        }
    }

    /* 예산 업데이트 */
    fun updateDailyBudget(newDailyBudget: Long) {
        require(newDailyBudget >= spentDailyBudget) {
            "일 예산은 사용한 소진액보다 작을 수 없습니다. spentDaily=$spentDailyBudget, newDaily=$newDailyBudget"
        }
        dailyBudget = newDailyBudget
    }
}
