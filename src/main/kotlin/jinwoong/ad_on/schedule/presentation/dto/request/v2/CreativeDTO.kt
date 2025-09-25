package jinwoong.ad_on.schedule.presentation.dto.request.v2

import jinwoong.ad_on.schedule.domain.aggregate.Status

data class CreativeDTO(
    var id: Long,
    var landingUrl: String?,
    var creativeStatus: Status?,
    /* Look */
    var imageURL: String?,
    var movieURL: String?,
    var logoURL: String?,
    var copyrightingTitle: String?,
    var copyrightingSubtitle: String?,
)