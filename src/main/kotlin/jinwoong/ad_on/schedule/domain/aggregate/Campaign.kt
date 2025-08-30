package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Campaign(
    var campaignId: Long,
    var totalBudget: Long,
    var spentTotalBudget: Long,
) {
    init {
        require(totalBudget > spentTotalBudget) {
            ("총 예산은 사용한 소진액보다 커야합니다.")
        }
    }
}
