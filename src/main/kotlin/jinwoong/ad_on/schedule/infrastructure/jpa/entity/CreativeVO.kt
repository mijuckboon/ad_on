package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.*
import jinwoong.ad_on.schedule.domain.aggregate.Status

@Embeddable
data class CreativeVO(
    @Column(name = "creative_id", nullable = false)
    var creativeId: Long,

    @Embedded
    var look: LookVO,

    @Column(name = "landing_url", nullable = false, length = 2000)
    var landingUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_status", nullable = false)
    var creativeStatus: Status,
)
