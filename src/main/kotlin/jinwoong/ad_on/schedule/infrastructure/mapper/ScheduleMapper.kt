package jinwoong.ad_on.schedule.infrastructure.mapper

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.infrastructure.jpa.entity.*
import jinwoong.ad_on.schedule.infrastructure.redis.SpentBudgets
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class ScheduleMapper(
    private val spentBudgetsRedisTemplate: RedisTemplate<String, SpentBudgets>,
    private val spentBudgetLongRedisTemplate: RedisTemplate<String, Long>
) {
    fun toDomain(entity: ScheduleEntity): Schedule {
        val (spentTotalBudget, spentDailyBudget) = getBudgets(entity.id!!)

        return Schedule(
            id = entity.id,
            campaign = entity.campaign.toDomain(spentTotalBudget),
            adSet = entity.adSet.toDomain(spentDailyBudget),
            creative = entity.creative.toDomain(),
        )
    }

    /**
     * Schedule ID 기준으로 Redis에서 budget 조회
     * - 우선 v1 key 조회
     * - 없으면 legacy key fallback
     * - 그래도 없으면 0L
     */
    private fun getBudgets(scheduleId: Long): Pair<Long, Long> {
        val spentTotalKey = "spentTotalBudget_v1:schedule:$scheduleId"
        val spentDailyKey = "spentDailyBudget_v1:schedule:$scheduleId"
        val legacyKey = "spentBudgets:schedule:$scheduleId"

        val spentTotal = spentBudgetLongRedisTemplate.opsForValue().get(spentTotalKey)
            ?: spentBudgetsRedisTemplate.opsForValue().get(legacyKey)?.spentTotalBudget
            ?: 0L

        val spentDaily = spentBudgetLongRedisTemplate.opsForValue().get(spentDailyKey)
            ?: spentBudgetsRedisTemplate.opsForValue().get(legacyKey)?.spentDailyBudget
            ?: 0L

        return spentTotal to spentDaily
    }

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
            imageURL = this.imageURL,
            movieURL = this.movieURL,
            logoURL = this.logoURL,
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
            imageURL = this.imageURL,
            movieURL = this.movieURL,
            logoURL = this.logoURL,
            copyrightingTitle = this.copyrightingTitle,
            copyrightingSubtitle = this.copyrightingSubtitle
        )
}