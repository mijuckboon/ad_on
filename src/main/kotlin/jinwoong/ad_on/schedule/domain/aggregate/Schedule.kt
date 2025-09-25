package jinwoong.ad_on.schedule.domain.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jinwoong.ad_on.schedule.presentation.dto.request.v1.AdSetDTO as AdSetDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v1.CampaignDTO as CampaignDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v1.CreativeDTO as CreativeDTOv1
import jinwoong.ad_on.schedule.presentation.dto.request.v2.AdSetDTO as AdSetDTOv2
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CampaignDTO as CampaignDTOv2
import jinwoong.ad_on.schedule.presentation.dto.request.v2.CreativeDTO as CreativeDTOv2
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
        return currentTime in adSet.startTime..adSet.endTime
    }

    /* update */
    fun updateCampaign(campaignDTO: CampaignDTOv1) {
        campaignDTO.totalBudget?.let {
            campaign.updateTotalBudget(it)
        }
    }

    fun updateAdSet(adSetDTO: AdSetDTOv1) {
        adSetDTO.dailyBudget?.let {
            adSet.updateDailyBudget(it)
        }

        adSet.startDate = adSetDTO.adSetStartDate ?: adSet.startDate
        adSet.startTime = adSetDTO.adSetStartTime ?: adSet.startTime
        adSet.endDate = adSetDTO.adSetEndDate ?: adSet.endDate
        adSet.endTime = adSetDTO.adSetEndTime ?: adSet.endTime
        adSet.status = adSetDTO.adSetStatus ?: adSet.status
        adSet.paymentType = adSetDTO.paymentType ?: adSet.paymentType
        adSet.unitCost = adSetDTO.unitCost ?: adSet.unitCost
    }

    fun updateCreative(creativeDTO: CreativeDTOv1) {
        creative.landingUrl = creativeDTO.landingUrl ?: creative.landingUrl
        creative.status = creativeDTO.creativeStatus ?: creative.status
        creative.look.imageURL = creativeDTO.creativeImage
        creative.look.movieURL = creativeDTO.creativeMovie
        creative.look.logoURL = creativeDTO.creativeLogo
        creative.look.copyrightingTitle = creativeDTO.copyrightingTitle
        creative.look.copyrightingSubtitle = creativeDTO.copyrightingSubtitle
    }

    fun updateCampaign(campaignDTO: CampaignDTOv2) {
        campaignDTO.totalBudget?.let {
            campaign.updateTotalBudget(it)
        }
    }

    fun updateAdSet(adSetDTO: AdSetDTOv2) {
        adSetDTO.dailyBudget?.let {
            adSet.updateDailyBudget(it)
        }

        adSet.startDate = adSetDTO.startDate ?: adSet.startDate
        adSet.startTime = adSetDTO.startTime ?: adSet.startTime
        adSet.endDate = adSetDTO.endDate ?: adSet.endDate
        adSet.endTime = adSetDTO.endTime ?: adSet.endTime
        adSet.status = adSetDTO.status ?: adSet.status
        adSet.paymentType = adSetDTO.paymentType ?: adSet.paymentType
        adSet.unitCost = adSetDTO.unitCost ?: adSet.unitCost
    }

    fun updateCreative(creativeDTO: CreativeDTOv2) {
        creative.landingUrl = creativeDTO.landingUrl ?: creative.landingUrl
        creative.status = creativeDTO.creativeStatus ?: creative.status
        creative.look.imageURL = creativeDTO.imageURL
        creative.look.movieURL = creativeDTO.movieURL
        creative.look.logoURL = creativeDTO.logoURL
        creative.look.copyrightingTitle = creativeDTO.copyrightingTitle
        creative.look.copyrightingSubtitle = creativeDTO.copyrightingSubtitle
    }

}
