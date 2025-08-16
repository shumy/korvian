package dev.korvian.pipeline

import dev.korvian.RejectError
import dev.korvian.di.store.EndpointSpec

enum class ErrorCode(val code: UInt, val reason: String) {
    ConnectionRejected(0U, "ConnectionRejected"),
    Unauthorized(1U, "Unauthorized"),
    Forbidden(2U, "Forbidden"),
    Invalid(3U, "Invalid")
}

class CheckError(error: ErrorCode): RejectError(error.code, error.reason)

fun interface IEndpointCheck<T: Annotation> {
    @Throws(RejectError::class)
    fun check(anno: T, spec: EndpointSpec)
}

fun interface IConnectionCheck {
    @Throws(RejectError::class)
    fun check(info: ConnectionInfo)
}