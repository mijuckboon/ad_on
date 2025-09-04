package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class LookVO(
    @Column(name = "image_url", length = 2000)
    var imageURL: String? = null,

    @Column(name = "movie_url", length = 2000)
    var movieURL: String? = null,

    @Column(name = "logo_url", length = 2000)
    var logoURL: String? = null,

    @Column(name = "copyrighting_title")
    var copyrightingTitle: String? = null,

    @Column(name = "copyrighting_subtitle")
    var copyrightingSubtitle: String? = null,

    )
