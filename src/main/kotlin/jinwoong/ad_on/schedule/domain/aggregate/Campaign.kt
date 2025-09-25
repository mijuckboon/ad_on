package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Campaign(
    var id: Long,
    var totalBudget: Long,
    var spentTotalBudget: Long,
) {
    init {
        require(totalBudget >= spentTotalBudget) {
            ("총 예산은 사용한 소진액보다 작을 수 없습니다. spentTotal=$spentTotalBudget, total=$totalBudget")
        }
    }

    /* 예산 업데이트 */
    fun updateTotalBudget(newTotalBudget: Long) {
        require(newTotalBudget >= spentTotalBudget) {
            "총 예산은 사용한 소진액보다 작을 수 없습니다. spentTotal=$spentTotalBudget, newTotal=$newTotalBudget"
        }
        totalBudget = newTotalBudget
    }
}
