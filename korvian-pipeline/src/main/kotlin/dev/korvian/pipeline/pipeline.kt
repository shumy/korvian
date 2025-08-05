package dev.korvian.pipeline

import dev.korvian.IReply
import dev.korvian.ISink
import dev.korvian.IStream
import dev.korvian.ISubscription
import dev.korvian.di.Store
import dev.korvian.di.store.EndpointParam
import dev.korvian.di.store.EndpointType
import kotlin.reflect.KType
import kotlin.reflect.safeCast
import kotlin.reflect.typeOf

class PipeException(msg: String): RuntimeException(msg)

interface IPipeline<I: Any, R: Any> {
    fun process(msg: I, onEvent: (R) -> Unit)
}

interface IDecoder<I: Any, T: Any> {
    fun preDecode(msg: I): IncomingMessage<T>
    fun bodyDecode(body: T, params: List<EndpointParam>): Array<Any?>
}

interface IEncoder<R: Any> {
    fun encode(header: Outgoing, rType: KType? = null, result: Any? = null): R
}

typealias EventCallBack<R> = (R) -> Unit
class Pipeline<I: Any, B: Any, R: Any>(val decoder: IDecoder<I, B>, val encoder: IEncoder<R>): IPipeline<I, R> {
    val router = Router(Store.Service)

    override fun process(msg: I, onEvent: EventCallBack<R>) {
        val dMsg = decoder.preDecode(msg)
        try {
            val route = router.resolve(dMsg.header)
            // TODO: header checks (security, custom)

            val paramValues = decoder.bodyDecode(dMsg.body, route.endpoint.params)
            // TODO: body checks (validation, mandatory, custom)


            // TODO: how to reject?
            val result = route.endpoint.exec.process(route.srv, paramValues)


            val rType = route.endpoint.retType.kType
            when (route.endpoint.type) {
                EndpointType.REQUEST -> processRequestResult(dMsg.header.ref, rType, result, onEvent)
                EndpointType.PUBLISH -> processPublishResult(dMsg.header.ref, rType, onEvent)
                EndpointType.SUBSCRIBE -> processSubscription(dMsg.header.ref, rType, result as ISubscription<*>, onEvent)
            }
        } catch (ex: PipeException) {
            val error = Outgoing.RefOutgoing.Error(dMsg.header.ref, ex.message!!)
            val data = encoder.encode(error)
            onEvent.invoke(data)
        } catch (ex: Throwable) {
            println("Unexpected Exception!")
            ex.printStackTrace()
        }
    }

    private fun processRequestResult(ref: String, rType: KType, result: Any?, onEvent: EventCallBack<R>) {
        val reply = IReply::class.safeCast(result)
        if (reply !== null) {
            processRequestReply(ref, rType, reply, onEvent)
            return
        }

        val stream = IStream::class.safeCast(result)
        if (stream !== null) {
            processRequestStream(ref, rType, stream, onEvent)
            return
        }

        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptEvt = encoder.encode(acceptHeader, rType, result)
        onEvent.invoke(acceptEvt)
    }

    private fun processRequestReply(ref: String, rType: KType, reply: IReply<*>, onEvent: EventCallBack<R>) {
        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptEvt = encoder.encode(acceptHeader)
        onEvent.invoke(acceptEvt)

        // TODO: how to handle multi-threading - structured concurrency?
        val result = reply.invoke()
        val replyHeader = Outgoing.RefOutgoing.Reply(ref)
        val replyEvt = encoder.encode(replyHeader, rType, result)
        onEvent.invoke(replyEvt)
    }

    private fun processRequestStream(ref: String, rType: KType, stream: IStream<*>, onEvent: EventCallBack<R>) {
        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptEvt = encoder.encode(acceptHeader)
        onEvent.invoke(acceptEvt)

        // TODO: how to handle multi-threading - structured concurrency?
        var seq = 0UL
        stream.invoke(ISink<Any> {
            val nextHeader = Outgoing.RefOutgoing.Next(ref, seq)
            val nextEvt = encoder.encode(nextHeader, rType, it)
            onEvent.invoke(nextEvt)
            seq++
        })

        val endHeader = Outgoing.RefOutgoing.End(ref, seq)
        val endEvt = encoder.encode(endHeader)
        onEvent.invoke(endEvt)
    }

    private fun processPublishResult(ref: String, rType: KType, onEvent: EventCallBack<R>) {
        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptEvt = encoder.encode(acceptHeader)
        onEvent.invoke(acceptEvt)
    }

    private fun processSubscription(ref: String, rType: KType, subscription: ISubscription<*>, onEvent: EventCallBack<R>) {
        // TODO: subscribe and send events to connection? - subscription.on {  }

        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptEvt = encoder.encode(acceptHeader, typeOf<String>(), subscription.id)
        onEvent.invoke(acceptEvt)
    }
}