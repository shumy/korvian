package dev.korvian.pipeline

import dev.korvian.di.store.EndpointParam
import kotlin.reflect.KType

interface IDecoder<I: Any, T: Any> {
    fun preDecode(msg: I): IncomingMessage<T>
    fun bodyDecode(body: T, params: List<EndpointParam>): Array<Any?>
}

interface IEncoder<R: Any> {
    fun encode(header: Outgoing, rType: KType? = null, result: Any? = null): R
}