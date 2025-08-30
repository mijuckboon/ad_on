package jinwoong.ad_on.schedule.infrastructure.mapper

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.infrastructure.jpa.entity.*
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets

class ScheduleMapper {
    companion object {
        fun toDomain(entity: ScheduleEntity, spentBudgets: SpentBudgets? = null): Schedule = Schedule(
            id = entity.id,
            campaign = entity.campaign.toDomain(spentBudgets?.spentTotalBudget ?: 0L),
            adSet = entity.adSet.toDomain(spentBudgets?.spentDailyBudget ?: 0L),
            creative = entity.creative.toDomain(),
        )

        fun toEntity(domain: Schedule): ScheduleEntity = ScheduleEntity(
            id = domain.id,
            campaign = domain.campaign.toEntity(),
            adSet = domain.adSet.toEntity(),
            creative = domain.creative.toEntity(),
        )

        /* domain to JPA */
        private fun Campaign.toEntity(): CampaignVO = CampaignVO(
            campaignId = this.campaignId,
            totalBudget = this.totalBudget
        )

        private fun AdSet.toEntity(): AdSetVO = AdSetVO(
            adSetId = this.adSetId,
            adSetStartDate = this.adSetStartDate,
            adSetEndDate = this.adSetEndDate,
            adSetStartTime = this.adSetStartTime,
            adSetEndTime = this.adSetEndTime,
            adSetStatus = this.adSetStatus,
            dailyBudget = this.dailyBudget,
            paymentType = this.paymentType,
            unitCost = this.unitCost
        )

        private fun Creative.toEntity(): CreativeVO = CreativeVO(
            creativeId = this.creativeId,
            look = this.look.toEntity(),
            landingUrl = this.landingUrl,
            creativeStatus = this.creativeStatus
        )

        private fun Look.toEntity(): LookVO = LookVO(
            creativeImage = this.creativeImage,
            creativeMovie = this.creativeMovie,
            creativeLogo = this.creativeLogo,
            copyrightingTitle = this.copyrightingTitle,
            copyrightingSubtitle = this.copyrightingSubtitle
        )

        /* JPA to domain */
        private fun CampaignVO.toDomain(spentTotalBudget: Long = 0L): Campaign = Campaign(
            campaignId = this.campaignId,
            totalBudget = this.totalBudget,
            spentTotalBudget = spentTotalBudget
        )

        private fun AdSetVO.toDomain(spentDailyBudget: Long = 0L): AdSet = AdSet(
            adSetId = this.adSetId,
            adSetStartDate = this.adSetStartDate,
            adSetEndDate = this.adSetEndDate,
            adSetStartTime = this.adSetStartTime,
            adSetEndTime = this.adSetEndTime,
            adSetStatus = this.adSetStatus,
            dailyBudget = this.dailyBudget,
            paymentType = this.paymentType,
            unitCost = this.unitCost,
            spentDailyBudget = spentDailyBudget
        )

        private fun CreativeVO.toDomain(): Creative = Creative(
            creativeId = this.creativeId,
            look = this.look.toDomain(),
            landingUrl = this.landingUrl,
            creativeStatus = this.creativeStatus
        )

        private fun LookVO.toDomain(): Look = Look(
            creativeImage = this.creativeImage,
            creativeMovie = this.creativeMovie,
            creativeLogo = this.creativeLogo,
            copyrightingTitle = this.copyrightingTitle,
            copyrightingSubtitle = this.copyrightingSubtitle
        )
    }
}