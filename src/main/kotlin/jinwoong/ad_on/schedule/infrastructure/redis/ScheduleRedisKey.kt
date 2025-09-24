package jinwoong.ad_on.schedule.infrastructure.redis

enum class ScheduleRedisKey(private val pattern: String) {
    SPENT_TOTAL_BUDGET_V1("spentTotalBudget_v1:schedule:%d"),
    SPENT_DAILY_BUDGET_V1("spentDailyBudget_v1:schedule:%d"),
    LEGACY_SPENT_BUDGETS("spentBudgets:schedule:%d"),

    CANDIDATE_V1("candidate:schedule:%d"),
    ;

    fun key(scheduleId: Long) = pattern.format(scheduleId)
    val scanPattern: String get() = pattern.replace("%d", "*")
}