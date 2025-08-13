package dev.korvian.pipeline

import dev.korvian.di.store.EndpointParam
import kotlin.reflect.KType

interface ISerializer<I: Any, R: Any> {
    fun preDecode(msg: I): IncomingMessage
    fun bodyDecode(body: Any?, params: List<EndpointParam>): Array<Any?>
    fun encode(header: Outgoing, rType: KType? = null, result: Any? = null): R
}