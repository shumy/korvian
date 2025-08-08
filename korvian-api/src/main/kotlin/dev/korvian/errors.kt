package dev.korvian

class RejectError(val code: UInt, val reason: String): RuntimeException()