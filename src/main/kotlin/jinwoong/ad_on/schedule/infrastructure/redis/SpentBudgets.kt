package jinwoong.ad_on.schedule.infrastructure.redis

data class SpentBudgets (
    val scheduleId: Long,
    val spentTotalBudget: Long,
    val spentDailyBudget: Long,
)