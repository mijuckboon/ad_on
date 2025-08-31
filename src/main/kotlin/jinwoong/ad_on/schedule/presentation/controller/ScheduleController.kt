package jinwoong.ad_on.schedule.presentation.controller

import jinwoong.ad_on.api.ApiResponse
import jinwoong.ad_on.schedule.application.service.AdServeService
import jinwoong.ad_on.schedule.application.service.ScheduleService
import jinwoong.ad_on.schedule.presentation.dto.request.ScheduleSaveRequest
import jinwoong.ad_on.schedule.presentation.dto.response.AdServeResponse
import jinwoong.ad_on.schedule.presentation.dto.response.ScheduleSaveResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalTime

@RestController
@RequestMapping("/schedules")
class ScheduleController(
    private val scheduleService: ScheduleService,
    private val adServeService: AdServeService,
) {
    @PostMapping
    fun createSchedules(@RequestBody scheduleSaveRequest: ScheduleSaveRequest): ResponseEntity<ApiResponse<ScheduleSaveResponse>> {
        val response = scheduleService.createSchedules(request = scheduleSaveRequest)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data = response))
    }

    @GetMapping("/serve-ad")
    fun getServingAd(): ResponseEntity<ApiResponse<AdServeResponse>> {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()

        val servingAdDTO = adServeService.getServingAd(today, currentTime)
        val response = AdServeResponse(servingAd = servingAdDTO)

        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }

}