package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jinwoong.ad_on.schedule.presentation.dto.request.AdSetDTO
import jinwoong.ad_on.schedule.presentation.dto.request.CampaignDTO
import jinwoong.ad_on.schedule.presentation.dto.request.CreativeDTO
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
class Schedule(
    var id: Long? = null,
    var campaign: Campaign,
    var adSet: AdSet,
    var creative: Creative,
) {
    fun hasRestBudget(): Boolean = when {
        hasToPay() -> {
            campaign.totalBudget >= campaign.spentTotalBudget + adSet.unitCost &&
            adSet.dailyBudget >= adSet.spentDailyBudget + adSet.unitCost
        }

        else -> {
            campaign.totalBudget > campaign.spentTotalBudget &&
            adSet.dailyBudget > adSet.spentDailyBudget
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
        return currentTime in adSet.adSetStartTime..adSet.adSetEndTime
    }

    /* update */
    fun updateCampaign(campaignDTO: CampaignDTO) {
        campaignDTO.totalBudget?.let {
            campaign.updateTotalBudget(it)
        }
    }

    fun updateAdSet(adSetDTO: AdSetDTO) {
        adSetDTO.dailyBudget?.let {
            adSet.updateDailyBudget(it)
        }

        adSet.adSetStartDate = adSetDTO.adSetStartDate ?: adSet.adSetStartDate
        adSet.adSetStartTime = adSetDTO.adSetStartTime ?: adSet.adSetStartTime
        adSet.adSetEndDate = adSetDTO.adSetEndDate ?: adSet.adSetEndDate
        adSet.adSetEndTime = adSetDTO.adSetEndTime ?: adSet.adSetEndTime
        adSet.adSetStatus = adSetDTO.adSetStatus ?: adSet.adSetStatus
        adSet.paymentType = adSetDTO.paymentType ?: adSet.paymentType
        adSet.unitCost = adSetDTO.unitCost ?: adSet.unitCost
    }

    fun updateCreative(creativeDTO: CreativeDTO) {
        creative.landingUrl = creativeDTO.landingUrl ?: creative.landingUrl
        creative.creativeStatus = creativeDTO.creativeStatus ?: creative.creativeStatus
        creative.look.creativeImage = creativeDTO.creativeImage
        creative.look.creativeMovie = creativeDTO.creativeMovie
        creative.look.creativeLogo = creativeDTO.creativeLogo
        creative.look.copyrightingTitle = creativeDTO.copyrightingTitle
        creative.look.copyrightingSubtitle = creativeDTO.copyrightingSubtitle
    }

}
