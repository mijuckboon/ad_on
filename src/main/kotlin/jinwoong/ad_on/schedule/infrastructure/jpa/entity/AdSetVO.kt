package jinwoong.ad_on.schedule.infrastructure.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jinwoong.ad_on.schedule.domain.aggregate.PaymentType
import jinwoong.ad_on.schedule.domain.aggregate.Status
import java.time.LocalDate
import java.time.LocalTime

@Embeddable
data class AdSetVO(
    @Column(name = "ad_set_id", nullable = false)
    var adSetId: Long,

    @Column(name = "ad_set_start_date", nullable = false)
    var adSetStartDate: LocalDate,

    @Column(name = "ad_set_end_date", nullable = false)
    var adSetEndDate: LocalDate,

    @Column(name = "ad_set_start_time", nullable = false)
    var adSetStartTime: LocalTime,

    @Column(name = "ad_set_end_time", nullable = false)
    var adSetEndTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_set_status", nullable = false)
    var adSetStatus: Status,

    @Column(name = "daily_budget", nullable = false)
    var dailyBudget: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    var paymentType: PaymentType,

    @Column(name = "unit_cost", nullable = false)
    var unitCost: Long = 0L,
)