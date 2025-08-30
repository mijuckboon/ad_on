package jinwoong.ad_on.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val code: String, val message: String, val httpStatus: HttpStatus) {
    SCHEDULE_NOT_FOUND("00000", "Schedule을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CAMPAIGN_NOT_FOUND("00001", "캠페인을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AD_SET_NOT_FOUND("00002", "광고 세트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CREATIVE_NOT_FOUND("00003", "소재를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    fun formatMessage(vararg args: Any?): String =
        message.format(*args)
}