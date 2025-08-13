package dev.korvian.pipeline

import dev.korvian.RejectError
import dev.korvian.di.store.EndpointSpec

enum class ErrorCode(val code: UInt, val reason: String) {
    Unknown(0U, "Unknown"),
    Unauthorized(1U, "Unauthorized"),
    Forbidden(2U, "Forbidden"),
    Invalid(3U, "Invalid")
}

class CheckError(error: ErrorCode): RejectError(error.code, error.reason)

fun interface ICheck<T: Annotation> {
    @Throws(CheckError::class)
    fun check(anno: T, spec: EndpointSpec)
}