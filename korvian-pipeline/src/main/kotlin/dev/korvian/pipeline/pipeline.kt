package dev.korvian.pipeline

import dev.korvian.IReply
import dev.korvian.IStream
import dev.korvian.ISubscription
import dev.korvian.di.store.EndpointParam
import dev.korvian.di.store.EndpointType
import kotlin.reflect.KType
import kotlin.reflect.safeCast
import dev.korvian.pipeline.OutgoingHeaderType.*

class PipeException(msg: String): RuntimeException(msg)

interface IPipeline<I: Any, R: Any> {
    fun process(msg: I, onReply: (R) -> Unit)
}

interface IDecoder<I: Any, T: Any> {
    fun preDecode(msg: I): IncomingMessage<T>
    fun bodyDecode(body: T, params: List<EndpointParam>): Array<Any?>
}

interface IEncoder<R: Any> {
    fun encode(header: Outgoing, kType: KType? = null, body: Any? = null): R
}

typealias ReplyCallBack<R> = (R) -> Unit
class Pipeline<I: Any, B: Any, R: Any>(val decoder: IDecoder<I, B>, val encoder: IEncoder<R>, val router: Router): IPipeline<I, R> {
    override fun process(msg: I, onReply: ReplyCallBack<R>) {
        val dMsg = decoder.preDecode(msg)
        try {
            val route = router.resolve(dMsg.header)
            // TODO: header checks (security, custom)

            val paramValues = decoder.bodyDecode(dMsg.body, route.endpoint.params)
            // TODO: body checks (validation, mandatory, custom)

            val rType = route.endpoint.retType.kType
            val result = route.endpoint.exec.process(route.srv, paramValues)
            when (route.endpoint.type) {
                EndpointType.REQUEST -> processRequestResult(dMsg.header.ref, rType, result, onReply)
                EndpointType.PUBLISH -> processPublishResult(dMsg.header.ref, rType, onReply)
                EndpointType.SUBSCRIBE -> processSubscription(dMsg.header.ref, rType, result as ISubscription<*>, onReply)
            }
        } catch (ex: PipeException) {
            val error = Outgoing.RefOutgoing.Error(dMsg.header.ref, ex.message!!)
            val data = encoder.encode(error)
            onReply.invoke(data)
        } catch (ex: Throwable) {
            println("Unexpected Exception!")
            ex.printStackTrace()
        }
    }

    private fun processRequestResult(ref: String, rType: KType, result: Any?, onReply: ReplyCallBack<R>) {
        val reply = IReply::class.safeCast(result)
        if (reply !== null) {
            processRequestReply(ref, rType, reply)
            return
        }

        val stream = IStream::class.safeCast(result)
        if (stream !== null) {
            processRequestStream(ref, rType, stream)
            return
        }

        val directReply = Outgoing.RefOutgoing.Accept(ref)
        val data = encoder.encode(directReply, rType, result)
        onReply.invoke(data)
    }


    private fun processRequestReply(ref: String, rType: KType, reply: IReply<*>) {
        // TODO: ack (ACCEPT, REJECT)

    }

    private fun processRequestStream(ref: String, rType: KType, stream: IStream<*>) {
        // TODO: ack (ACCEPT, REJECT)
    }

    private fun processPublishResult(ref: String, rType: KType, onReply: ReplyCallBack<R>) {
        // TODO: ack (ACCEPT, REJECT)
    }

    private fun processSubscription(ref: String, rType: KType, subscription: ISubscription<*>, onReply: ReplyCallBack<R>) {
        // TODO: ack (ACCEPT, REJECT)
    }
}