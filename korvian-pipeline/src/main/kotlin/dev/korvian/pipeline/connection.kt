package dev.korvian.pipeline

import dev.korvian.IReply
import dev.korvian.IReplyTask
import dev.korvian.IStream
import dev.korvian.IStreamTask
import dev.korvian.ISubscription
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KType
import kotlin.reflect.safeCast
import kotlin.reflect.typeOf

@OptIn(ExperimentalAtomicApi::class)
class Connection<I: Any, R: Any>(private val pipeline: Pipeline<I, *, R>, private val onMsg: MsgCallback<R>) {
    private val subscriptions = ConcurrentHashMap<String, ISubscription<*>>()

    fun process(msg: I) {
        pipeline.process(this, msg)
    }

    fun close() {
        subscriptions.values.forEach { it.unsubscribe() }
    }

    internal fun processPublish(ref: String) {
        sendAccept(ref)
    }

    internal fun processSubscribe(ref: String, rType: KType, result: Any) {
        val sub = result as ISubscription<*>

        subscriptions[sub.id] = sub
        sendAccept(ref, typeOf<String>(), sub.id)

        val seq = AtomicLong(-1L)
        sub.on {
            sendEvent(sub.id, seq.addAndFetch(1L), rType, it)
        }
    }

    internal fun processUnSubscribe(ref: String, id: String) {
        subscriptions[id]?.unsubscribe()
        subscriptions.remove(id)
        sendAccept(ref, typeOf<String>(), id)
    }

    internal fun processRequest(ref: String, rType: KType, result: Any?) {
        val reply = IReply::class.safeCast(result)
        if (reply !== null) {
            processRequestReply(ref, rType, reply)
            return
        }

        val replyTask = IReplyTask::class.safeCast(result)
        if (replyTask !== null) {
            processRequestReplyTask(ref, rType, replyTask)
            return
        }

        val stream = IStream::class.safeCast(result)
        if (stream !== null) {
            processRequestStream(ref, rType, stream)
            return
        }

        val streamTask = IStreamTask::class.safeCast(result)
        if (streamTask !== null) {
            processRequestStreamTask(ref, rType, streamTask)
            return
        }

        // any other supported result type, send accept/result in same msg
        sendAccept(ref, rType, result)
    }

    internal fun processRequestReply(ref: String, rType: KType, reply: IReply<*>) {
        sendAccept(ref)
        val result = reply.invoke()
        sendReply(ref, rType, result)
    }

    internal fun processRequestReplyTask(ref: String, rType: KType, reply: IReplyTask<*>) {
        sendAccept(ref)
        val result = runBlocking { reply.invoke() }
        sendReply(ref, rType, result)
    }

    internal fun processRequestStream(ref: String, rType: KType, stream: IStream<*>) {
        sendAccept(ref)

        val seq = AtomicLong(-1L)
        stream.invoke { sendNext(ref, seq.addAndFetch(1L), rType, it) }

        sendEnd(ref, seq.addAndFetch(1L))
    }

    internal fun processRequestStreamTask(ref: String, rType: KType, stream: IStreamTask<*>) {
        sendAccept(ref)

        val seq = AtomicLong(-1L)
        runBlocking {
            stream.invoke { sendNext(ref, seq.addAndFetch(1L), rType, it) }
        }

        sendEnd(ref, seq.addAndFetch(1L))
    }


    internal fun sendError(ref: String, msg: String) {
        val error = Outgoing.RefOutgoing.Error(ref, msg)
        val data = pipeline.encoder.encode(error)
        onMsg.invoke(data)
    }

    private fun sendAccept(ref: String, rType: KType? = null, result: Any? = null) {
        val acceptHeader = Outgoing.RefOutgoing.Accept(ref)
        val acceptMsg = pipeline.encoder.encode(acceptHeader, rType, result)
        onMsg.invoke(acceptMsg)
    }

    private fun sendReply(ref: String, rType: KType, result: Any?) {
        val replyHeader = Outgoing.RefOutgoing.Reply(ref)
        val replyMsg = pipeline.encoder.encode(replyHeader, rType, result)
        onMsg.invoke(replyMsg)
    }

    private fun sendNext(ref: String, seq: Long, rType: KType, result: Any) {
        val nextHeader = Outgoing.RefOutgoing.Next(ref, seq)
        val nextMsg = pipeline.encoder.encode(nextHeader, rType, result)
        onMsg.invoke(nextMsg)
    }

    private fun sendEnd(ref: String, seq: Long) {
        val endHeader = Outgoing.RefOutgoing.End(ref, seq)
        val endMsg = pipeline.encoder.encode(endHeader)
        onMsg.invoke(endMsg)
    }

    private fun sendEvent(id: String, seq: Long, rType: KType, result: Any) {
        val eventHeader = Outgoing.Event(id, seq)
        val eventMsg = pipeline.encoder.encode(eventHeader, rType, result)
        onMsg.invoke(eventMsg)
    }
}