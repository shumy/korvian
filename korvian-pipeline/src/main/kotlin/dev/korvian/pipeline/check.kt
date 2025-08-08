package dev.korvian.pipeline

enum class CheckErrorCode(val code: UInt, reason: String) {
    Unknown(0U, "Unknown"),
    Unauthorized(1U, "Unauthorized"),

    Forbidden(3U, "Forbidden")
}

class CheckError(val error: CheckErrorCode): RuntimeException()

interface ICheck<H: Incoming> {
    @Throws(CheckError::class)
    fun check(header: H): Unit
}