package jinwoong.ad_on.schedule.domain.aggregate

import java.time.LocalTime

class Schedule(
    var id: Long? = null,
    var campaign: Campaign,
    var adSet: AdSet,
    var creative: Creative,
) {
    fun hasRestBudget(): Boolean = when {
        hasToPay() -> {
            campaign.totalBudget >= campaign.spentTotalBudget + adSet.unitCost &&
            adSet.dailyBudget >= adSet.spentDailyBudget + adSet.unitCost
        }

        else -> {
            campaign.totalBudget > campaign.spentTotalBudget &&
            adSet.dailyBudget > adSet.spentDailyBudget
        }
    }

    /**
     * 과금 처리 여부 판단
     */
    fun hasToPay(): Boolean {
        /* 로직 작성 (CPM은 1000회 단위, CPC, CPA는 클릭 혹은 원하는 동작이 실행됐을 때) */
        return true
    }

    fun isActiveByTime(currentTime: LocalTime): Boolean {
        return currentTime in adSet.adSetStartTime..adSet.adSetEndTime
    }

}
