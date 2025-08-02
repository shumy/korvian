package dev.korvian.pipeline

import kotlinx.serialization.Serializable

enum class IncomingHeaderType(val code: String) {
    REQUEST("req"), PUBLISH("pub"), SUBSCRIBE("sub");
    companion object {
        fun fromCode(code: String) = when (code) {
            REQUEST.code -> REQUEST
            PUBLISH.code -> PUBLISH
            SUBSCRIBE.code -> SUBSCRIBE
            else -> throw PipeException("'head.typ=$code' not supported for incoming messages!")
        }
    }
}

enum class OutgoingHeaderType(code: String) {
    REPLY("rpl"), ACCEPT("acp"), REJECT("rej"),
    NEXT("nxt"), END("end"), ERROR("err"),
    EVENT("evt")
}

sealed interface Incoming {
    val ref: String
    val srv: String
    val trg: String

    @Serializable
    data class Request(override val ref: String, override val srv: String, override val trg: String): Incoming

    @Serializable
    data class Publish(override val ref: String, override val srv: String, override val trg: String): Incoming

    @Serializable
    data class Subscribe(override val ref: String, override val srv: String, override val trg: String): Incoming
}

sealed interface Outgoing {
    sealed interface RefOutgoing: Outgoing {
        val ref: String

        @Serializable
        data class Reply(override val ref: String) : RefOutgoing

        @Serializable
        data class Accept(override val ref: String) : RefOutgoing

        @Serializable
        data class Reject(override val ref: String) : RefOutgoing

        @Serializable
        data class Next(override val ref: String, val seq: Unit) : RefOutgoing

        @Serializable
        data class End(override val ref: String) : RefOutgoing

        @Serializable
        data class Error(override val ref: String, val msg: String) : RefOutgoing
    }

    object Event: Outgoing
}

@Serializable
class IncomingMessage<T: Any>(val header: Incoming, val body: T)