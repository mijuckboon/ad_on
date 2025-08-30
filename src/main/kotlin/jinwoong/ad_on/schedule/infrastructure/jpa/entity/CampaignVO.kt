package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class CampaignVO(
    @Column(name = "campaign_id", nullable = false)
    var campaignId: Long,

    @Column(name = "total_budget", nullable = false)
    var totalBudget: Long,
)
