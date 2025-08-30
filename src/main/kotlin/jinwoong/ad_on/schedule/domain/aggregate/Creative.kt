package jinwoong.ad_on.schedule.domain.aggregate

data class Creative(
    var creativeId: Long,
    var look: Look,
    var landingUrl: String,
    var creativeStatus: Status,
)
