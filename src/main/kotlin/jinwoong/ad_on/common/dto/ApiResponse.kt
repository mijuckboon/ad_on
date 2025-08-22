package jinwoong.ad_on.common.dto

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val errorCode: String? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
                timestamp = LocalDateTime.now()
            )

        fun <T> failure(errorCode: String, message: String): ApiResponse<T> =
            ApiResponse(
                success = false,
                errorCode = errorCode,
                message = message,
                timestamp = LocalDateTime.now()
            )
    }
}