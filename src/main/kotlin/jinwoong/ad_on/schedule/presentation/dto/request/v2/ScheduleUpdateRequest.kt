package jinwoong.ad_on.schedule.presentation.dto.request.v2

data class ScheduleUpdateRequest(
    val campaign: CampaignDTO? = null,
    val adSet: AdSetDTO? = null,
    val creative: CreativeDTO? = null,
)