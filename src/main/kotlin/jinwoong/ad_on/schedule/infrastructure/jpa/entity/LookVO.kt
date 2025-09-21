package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class LookVO(
    @Column(table = "creative", name = "image_url", length = 2000)
    var imageURL: String? = null,

    @Column(table = "creative", name = "movie_url", length = 2000)
    var movieURL: String? = null,

    @Column(table = "creative", name = "logo_url", length = 2000)
    var logoURL: String? = null,

    @Column(table = "creative", name = "copyrighting_title")
    var copyrightingTitle: String? = null,

    @Column(table = "creative", name = "copyrighting_subtitle")
    var copyrightingSubtitle: String? = null,

)