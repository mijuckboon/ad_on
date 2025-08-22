package jinwoong.ad_on.schedule.application.service

import jinwoong.ad_on.schedule.domain.aggregate.PaymentType
import jinwoong.ad_on.schedule.domain.aggregate.Schedule
import jinwoong.ad_on.schedule.domain.aggregate.Status
import jinwoong.ad_on.schedule.domain.repository.ScheduleRepository
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleSaveRequest
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleSaveResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val redisTemplate: RedisTemplate<String, Schedule>,
) {
    private val updatedSchedules: MutableList<Schedule> = mutableListOf()

    companion object {
        private val log = LoggerFactory.getLogger(ScheduleService::class.java)
        const val CACHE_INTERVAL_IN_MILISEC = 5 * 60 * 1000L // 5분
    }

    /**
     * 광고 플랫폼 서버가 전파하는 광고 정보를 저장
     */
    @Transactional
    fun createSchedules(request: ScheduleSaveRequest): ScheduleSaveResponse {
        val savedIds = mutableListOf<Long>()

        request.schedules.forEach { dto ->
            val schedule = Schedule(
                creativeId = dto.creativeId,
                adSetStartDate = dto.adSetStartDate,
                adSetEndDate = dto.adSetEndDate,
                adSetStartTime = dto.adSetStartTime,
                adSetEndTime = dto.adSetEndTime,
                totalBudget = dto.totalBudget,
                dailyBudget = dto.dailyBudget,
                spentTotalBudget = 0L,
                spentDailyBudget = 0L,
                paymentType = PaymentType.valueOf(dto.paymentType),
                unitCost = dto.unitCost,
                creativeImage = dto.creativeImage,
                creativeMovie = dto.creativeMovie,
                creativeLogo = dto.creativeLogo,
                copyrightingTitle = dto.copyrightingTitle,
                copyrightingSubtitle = dto.copyrightingSubtitle,
                adSetStatus = Status.valueOf(dto.adSetStatus),
                creativeStatus = Status.valueOf(dto.creativeStatus),
                landingUrl = dto.landingUrl,
                createdAt = LocalDateTime.now(),
                updatedAt = null,
            )

            val savedSchedule = scheduleRepository.save(schedule)
            savedIds.add(savedSchedule.id!!)
        }

        log.info("스케줄 생성 완료, count: ${savedIds.size}")
        return ScheduleSaveResponse(savedIds)
    }

    /**
     * 예산 동기화
     */
    fun syncBudgetToDB() {
        if (updatedSchedules.isEmpty()) return

        updatedSchedules.forEach { scheduleRepository.save(it) }
        log.info("DB에 예산 동기화 완료, count=${updatedSchedules.size}")
        /* 플랫폼 서버에 쏴주는 로직도 필요 */

        // 동기화 후 리스트 초기화
        updatedSchedules.clear()
    }

    /**
     * 서빙 가능한 광고를 Redis에 캐싱
     */
    fun cacheCandidates() {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()

        val candidates = getCandidatesFromDB(today)
        val filteredCandidates = filterCandidates(candidates, currentTime)

        // 기존 캐시 초기화
        redisTemplate.delete(redisTemplate.keys("candidate:schedule:*"))

        // Redis에 저장
        filteredCandidates.forEach {
            val key = "candidate:schedule:${it.id}"
            redisTemplate.opsForValue().set(key, it)
        }

        log.info("캐시 갱신 완료, count: ${filteredCandidates.size}")
    }

    /**
     * 서빙할 광고 선택
     * Redis 조회 -> DB 조회 (fallback)
     */
    fun getServingAd(today: LocalDate, currentTime: LocalTime): Schedule? {
        val candidates = getCandidatesFromRedis()
        val servingAd = getServingAdFromRedis(candidates)
        if (servingAd != null) return servingAd

        log.info("Redis 조회 실패. DB에서 조회")
        return getServingAdFromDB(today, currentTime)
    }

    fun getServingAdFromRedis(candidates: List<Schedule>): Schedule? {
        if (candidates.isEmpty()) return null

        val servingAd = candidates.random()
        updateBudgetAfterServe(servingAd)
        return servingAd
    }

    fun getServingAdFromDB(today: LocalDate, currentTime: LocalTime): Schedule? {
        val fallbackCandidates = getCandidatesFromDB(today)
        val filteredCandidates = filterCandidates(fallbackCandidates, currentTime)

        val servingAd = filteredCandidates.randomOrNull()
        updateBudgetAfterServe(servingAd)
        return servingAd
    }

    /**
     * 광고 서빙 후 budget 업데이트
     */
    fun updateBudgetAfterServe(schedule: Schedule?) {
        if (schedule == null) return
        if (!schedule.hasToPay()) return

        schedule.spentTotalBudget += schedule.unitCost
        schedule.spentDailyBudget += schedule.unitCost

        // Redis 캐시 업데이트
        val key = "candidate:schedule:${schedule.id}"
        redisTemplate.opsForValue().set(key, schedule)

        // DB 동기화용 리스트 추가
        if (!updatedSchedules.contains(schedule)) updatedSchedules.add(schedule)

        log.info("광고 ${schedule.id} 소진액 수정: total=${schedule.spentTotalBudget}, daily=${schedule.spentDailyBudget}")

    }

    fun getCandidatesFromRedis(): List<Schedule> {
        val keys = redisTemplate.keys("candidate:schedule:*")
        if (keys.isEmpty()) return emptyList()

        return keys.mapNotNull { redisTemplate.opsForValue().get(it) }
    }

    fun getCandidatesFromDB(today: LocalDate): List<Schedule> {
        /* DB 조회: 비교적 실시간성이 낮은 값을 미리 필터링 */
        return scheduleRepository.findCandidates(today)
    }

    /* BE: time, budget 등 실시간성이 필요한 값을 필터링 */
    fun filterCandidates(candidates: List<Schedule>, currentTime: LocalTime): List<Schedule> {
        return candidates
            .filter { it.hasRestBudget() && it.isActiveByTime(currentTime) }
    }

    @Scheduled(fixedRate = CACHE_INTERVAL_IN_MILISEC)
    fun refreshCache() {
        syncBudgetToDB()
        cacheCandidates()
    }

}