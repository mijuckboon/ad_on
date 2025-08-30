package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Creative(
    var creativeId: Long,
    var look: Look,
    var landingUrl: String,
    var creativeStatus: Status,
)
