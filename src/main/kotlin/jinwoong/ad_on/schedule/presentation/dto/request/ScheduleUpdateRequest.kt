package jinwoong.ad_on.schedule.presentation.dto.request

data class ScheduleUpdateRequest(
    val campaign: CampaignDTO? = null,
    val adSet: AdSetDTO? = null,
    val creative: CreativeDTO? = null,
)