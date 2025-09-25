package jinwoong.ad_on.schedule.application.mapper

import jinwoong.ad_on.schedule.domain.aggregate.*
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleDTO

fun ScheduleDTO.toDomain(): Schedule =
    Schedule(
        campaign = Campaign(
            id = campaignId,
            totalBudget = totalBudget,
            spentTotalBudget = spentTotalBudget ?: 0L
        ),
        adSet = AdSet(
            id = adSetId,
            startDate = adSetStartDate,
            endDate = adSetEndDate,
            startTime = adSetStartTime,
            endTime = adSetEndTime,
            status = Status.valueOf(adSetStatus),
            dailyBudget = dailyBudget,
            unitCost = unitCost,
            spentDailyBudget = spentDailyBudget ?: 0L,
            paymentType = PaymentType.valueOf(paymentType),
        ),
        creative = Creative(
            id = creativeId,
            status = Status.valueOf(creativeStatus),
            landingUrl = landingUrl,
            look = Look(
                imageURL = creativeImage,
                movieURL = creativeMovie,
                logoURL = creativeLogo,
                copyrightingTitle = copyrightingTitle,
                copyrightingSubtitle = copyrightingSubtitle
            )
        )
    )