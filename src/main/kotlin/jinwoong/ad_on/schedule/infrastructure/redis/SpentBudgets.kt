package jinwoong.ad_on.schedule.infrastructure.redis

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class SpentBudgets (
    val scheduleId: Long,
    val spentTotalBudget: Long,
    val spentDailyBudget: Long,
)