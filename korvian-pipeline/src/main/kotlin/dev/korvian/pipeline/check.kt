package dev.korvian.pipeline

import dev.korvian.di.store.EndpointSpec

enum class ErrorCode(val code: UInt, val reason: String) {
    Unknown(0U, "Unknown"),
    Unauthorized(1U, "Unauthorized"),
    Forbidden(2U, "Forbidden"),
    Invalid(3U, "Invalid")
}

class CheckError(val error: ErrorCode): RuntimeException()

fun interface ICheck<T: Annotation> {
    @Throws(CheckError::class)
    fun check(anno: T, spec: EndpointSpec): Unit
}