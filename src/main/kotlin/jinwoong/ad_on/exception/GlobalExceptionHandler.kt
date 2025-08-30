package jinwoong.ad_on.exception

import jinwoong.ad_on.api.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Void>> {
        val errorCode = e.errorCode

        val response: ApiResponse<Void> = ApiResponse.failure(
            errorCode.code,
            e.message ?: errorCode.message // 없으면 기본 메시지
        )

        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(response)
    }

}
