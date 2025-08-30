package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "schedule")
@SecondaryTables(
    SecondaryTable(name = "campaign", pkJoinColumns = [PrimaryKeyJoinColumn(name = "schedule_id")]),
    SecondaryTable(name = "ad_set", pkJoinColumns = [PrimaryKeyJoinColumn(name = "schedule_id")]),
    SecondaryTable(name = "creative", pkJoinColumns = [PrimaryKeyJoinColumn(name = "schedule_id")])
)
class ScheduleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "campaignId", column = Column(table = "campaign", name = "campaign_id")),
        AttributeOverride(name = "totalBudget", column = Column(table = "campaign", name = "total_budget")),
    )
    var campaign: CampaignVO,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "adSetId", column = Column(table = "ad_set", name = "ad_set_id")),
        AttributeOverride(name = "adSetStartDate", column = Column(table = "ad_set", name = "ad_set_start_date")),
        AttributeOverride(name = "adSetEndDate", column = Column(table = "ad_set", name = "ad_set_end_date")),
        AttributeOverride(name = "adSetStartTime", column = Column(table = "ad_set", name = "ad_set_start_time")),
        AttributeOverride(name = "adSetEndTime", column = Column(table = "ad_set", name = "ad_set_end_time")),
        AttributeOverride(name = "adSetStatus", column = Column(table = "ad_set", name = "ad_set_status")),
        AttributeOverride(name = "dailyBudget", column = Column(table = "ad_set", name = "daily_budget")),
        AttributeOverride(name = "paymentType", column = Column(table = "ad_set", name = "payment_type")),
        AttributeOverride(name = "unitCost", column = Column(table = "ad_set", name = "unit_cost"))
    )
    var adSet: AdSetVO,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "creativeId", column = Column(table = "creative", name = "creative_id")),
        AttributeOverride(name = "landingUrl", column = Column(table = "creative", name = "landing_url")),
        AttributeOverride(name = "creativeStatus", column = Column(table = "creative", name = "creative_status")),
        AttributeOverride(
            name = "look.creativeImage",
            column = Column(table = "creative", name = "creative_image")
        ),
        AttributeOverride(
            name = "look.creativeMovie",
            column = Column(table = "creative", name = "creative_movie")
        ),
        AttributeOverride(name = "look.creativeLogo", column = Column(table = "creative", name = "creative_logo")),
        AttributeOverride(
            name = "look.copyrightingTitle",
            column = Column(table = "creative", name = "copyrighting_title")
        ),
        AttributeOverride(
            name = "look.copyrightingSubtitle",
            column = Column(table = "creative", name = "copyrighting_subtitle")
        )
    )
    var creative: CreativeVO,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

)