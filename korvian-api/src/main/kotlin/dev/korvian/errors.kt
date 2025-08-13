package dev.korvian

open class RejectError(val code: UInt, val reason: String): RuntimeException()