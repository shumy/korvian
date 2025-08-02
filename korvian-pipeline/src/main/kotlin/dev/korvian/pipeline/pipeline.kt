package dev.korvian.pipeline

import dev.korvian.IReply
import dev.korvian.IStream
import dev.korvian.ISubscription
import dev.korvian.di.store.EndpointParam
import dev.korvian.di.store.EndpointType
import kotlin.reflect.safeCast

class PipeException(msg: String): RuntimeException(msg)

interface IPipeline {
    fun process(msg: ByteArray, onReply: (ByteArray) -> Unit)
}

interface IDecoder<T: Any> {
    fun preDecode(msg: ByteArray): IncomingMessage<T>
    fun bodyDecode(body: T, params: List<EndpointParam>): Array<Any?>
}

interface IEncoder<T> {
    fun encode(header: Outgoing, data: T? = null): ByteArray
}


class Pipeline<T: Any>(val decoder: IDecoder<T>, val encoder: IEncoder<T>, val router: Router): IPipeline {
    override fun process(msg: ByteArray, onReply: (ByteArray) -> Unit) {
        val dMsg = decoder.preDecode(msg)
        try {
            val route = router.resolve(dMsg.header)
            // TODO: header checks (security, custom)

            val paramValues = decoder.bodyDecode(dMsg.body, route.endpoint.params)
            // TODO: body checks (validation, mandatory, custom)

            val result = route.endpoint.exec.process(route.srv, *paramValues)
            when (route.endpoint.type) {
                EndpointType.REQUEST -> processRequestResult(result)
                EndpointType.PUBLISH -> processPublishResult()
                EndpointType.SUBSCRIBE -> processSubscription(result as ISubscription<*>)
            }
        } catch (ex: PipeException) {
            val error = Outgoing.RefOutgoing.Error(dMsg.header.ref, ex.message!!)
            val data = encoder.encode(error)
            onReply.invoke(data)
        } catch (ex: Exception) {
            println("Unexpected Exception!")
            ex.printStackTrace()
        }
    }
}

private fun processRequestResult(result: Any?) {
    val reply = IReply::class.safeCast(result)
    if (reply !== null) {
        processRequestReply(reply)
        return
    }

    val stream = IStream::class.safeCast(result)
    if (stream !== null) {
        processRequestStream(stream)
        return
    }

    // TODO: send direct reply
}

private fun processRequestReply(reply: IReply<*>) {
    // TODO: ack (ACCEPT, REJECT)

}

private fun processRequestStream(stream: IStream<*>) {
    // TODO: ack (ACCEPT, REJECT)
}

private fun processPublishResult() {
    // TODO: ack (ACCEPT, REJECT)
}

private fun processSubscription(subscription: ISubscription<*>) {
    // TODO: ack (ACCEPT, REJECT)
}