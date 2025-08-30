package jinwoong.ad_on.schedule.presentation.dto.response

import jinwoong.ad_on.schedule.domain.aggregate.Schedule

data class ServingAdDTO(
    val scheduleId: Long,
    val creativeImage: String? = null,
    val creativeMovie: String? = null,
    val creativeLogo: String? = null,
    val copyrightingTitle: String? = null,
    val copyrightingSubtitle: String? = null,
    val landingUrl: String
) {
    companion object {
        fun from(schedule: Schedule): ServingAdDTO {
            return ServingAdDTO(
                scheduleId = schedule.id!!,
                creativeImage = schedule.creative.look.creativeImage,
                creativeMovie = schedule.creative.look.creativeMovie,
                creativeLogo = schedule.creative.look.creativeLogo,
                copyrightingTitle = schedule.creative.look.copyrightingTitle,
                copyrightingSubtitle = schedule.creative.look.copyrightingSubtitle,
                landingUrl = schedule.creative.landingUrl
            )
        }
    }
}

