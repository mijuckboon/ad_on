package jinwoong.ad_on.schedule.presentation.dto.request

import jinwoong.ad_on.schedule.domain.aggregate.Status

data class CreativeDTO(
    var creativeId: Long,
    var landingUrl: String?,
    var creativeStatus: Status?,
    /* Look */
    var creativeImage: String?,
    var creativeMovie: String?,
    var creativeLogo: String?,
    var copyrightingTitle: String?,
    var copyrightingSubtitle: String?,
)