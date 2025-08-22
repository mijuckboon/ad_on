package jinwoong.ad_on.schedule.domain.aggregate

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "schedule")
class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "creative_id", nullable = false)
    var creativeId: Long,

    @Column(name = "ad_set_start_date", nullable = false)
    var adSetStartDate: LocalDate,

    @Column(name = "ad_set_end_date", nullable = false)
    var adSetEndDate: LocalDate,

    @Column(name = "ad_set_start_time", nullable = false)
    var adSetStartTime: LocalTime,

    @Column(name = "ad_set_end_time", nullable = false)
    var adSetEndTime: LocalTime,

    @Column(name = "total_budget", nullable = false)
    var totalBudget: Long,

    @Column(name = "daily_budget", nullable = false)
    var dailyBudget: Long,

    @Column(name = "spent_total_budget", nullable = false)
    var spentTotalBudget: Long = 0L,

    @Column(name = "spent_daily_budget", nullable = false)
    var spentDailyBudget: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    var paymentType: PaymentType,

    @Column(name = "unit_cost", nullable = false)
    var unitCost: Long = 0L,

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

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_set_status", nullable = false)
    var adSetStatus: Status,

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_status", nullable = false)
    var creativeStatus: Status,

    @Column(name = "landing_url", nullable = false, length = 2000)
    var landingUrl: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

) {
    fun hasRestBudget(): Boolean = when {
        hasToPay() -> {
            totalBudget >= spentTotalBudget + unitCost &&
            dailyBudget >= spentDailyBudget + unitCost
        }

        else -> {
            totalBudget > spentTotalBudget &&
            dailyBudget > spentDailyBudget
        }
    }

    /**
     * 과금 처리 여부 판단
     */
    fun hasToPay(): Boolean {
        /* 로직 작성 (CPM은 1000회 단위, CPC, CPA는 클릭 혹은 원하는 동작이 실행됐을 때) */
        return true
    }

    fun isActiveByTime(currentTime: LocalTime): Boolean {
        return currentTime in adSetStartTime..adSetEndTime
    }
}
