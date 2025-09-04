package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Look(
    var imageURL: String? = null,
    var movieURL: String? = null,
    var logoURL: String? = null,
    var copyrightingTitle: String? = null,
    var copyrightingSubtitle: String? = null,
)
