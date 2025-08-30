package jinwoong.ad_on.exception

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
): RuntimeException(message) {
    constructor(errorCode: ErrorCode, vararg args: Any?) :
            this(errorCode, errorCode.formatMessage(*args))
}