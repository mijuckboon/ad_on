package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class LookVO(
    @Column(name = "creative_image", length = 2000)
    var creativeImage: String? = null,

    @Column(name = "creative_movie", length = 2000)
    var creativeMovie: String? = null,

    @Column(name = "creative_logo", length = 2000)
    var creativeLogo: String? = null,

    @Column(name = "copyrighting_title")
    var copyrightingTitle: String? = null,

    @Column(name = "copyrighting_subtitle")
    var copyrightingSubtitle: String? = null,

)
