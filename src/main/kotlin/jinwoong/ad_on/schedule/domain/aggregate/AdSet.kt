package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class AdSet(
    var id: Long,
    var startDate: LocalDate,
    var endDate: LocalDate,
    var startTime: LocalTime,
    var endTime: LocalTime,
    var status: Status,
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
