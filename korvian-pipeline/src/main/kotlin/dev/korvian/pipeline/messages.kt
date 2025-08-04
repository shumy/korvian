package dev.korvian.pipeline

import kotlinx.serialization.Serializable

enum class IncomingHeaderType(val code: String) {
    REQUEST("req"), PUBLISH("pub"), SUBSCRIBE("sub");

    companion object {
        fun fromCode(code: String) = when (code) {
            REQUEST.code -> REQUEST
            PUBLISH.code -> PUBLISH
            SUBSCRIBE.code -> SUBSCRIBE
            else -> throw PipeException("Code $code not supported for IncomingHeaderType!")
        }
    }
}

enum class OutgoingHeaderType(val code: String) {
    REPLY("rpl"), ACCEPT("acp"), REJECT("rej"),
    NEXT("nxt"), END("end"), ERROR("err"),
    EVENT("evt");

    companion object {
        fun fromCode(code: String) = when (code) {
            REPLY.code -> REPLY
            ACCEPT.code -> ACCEPT
            REJECT.code -> REJECT
            NEXT.code -> NEXT
            END.code -> END
            ERROR.code -> ERROR
            EVENT.code -> EVENT
            else -> throw PipeException("Code $code not supported for OutgoingHeaderType!")
        }
    }
}

sealed interface Incoming {
    val typ: String
    val ref: String
    val srv: String
    val trg: String

    @Serializable
    data class Request(override val typ: String, override val ref: String, override val srv: String, override val trg: String): Incoming

    @Serializable
    data class Publish(override val typ: String, override val ref: String, override val srv: String, override val trg: String): Incoming

    @Serializable
    data class Subscribe(override val typ: String, override val ref: String, override val srv: String, override val trg: String): Incoming
}

sealed interface Outgoing {
    val typ: String

    sealed interface RefOutgoing: Outgoing {
        val ref: String

        @Serializable
        data class Reply(override val ref: String): RefOutgoing {
            override val typ: String = OutgoingHeaderType.REPLY.code
        }

        @Serializable
        data class Accept(override val ref: String): RefOutgoing {
            override val typ: String = OutgoingHeaderType.ACCEPT.code
        }

        @Serializable
        data class Reject(override val ref: String): RefOutgoing {
            override val typ: String = OutgoingHeaderType.REJECT.code
        }

        @Serializable
        data class Next(override val ref: String, val seq: ULong): RefOutgoing {
            override val typ: String = OutgoingHeaderType.NEXT.code
        }

        @Serializable
        data class End(override val ref: String, val seq: ULong): RefOutgoing {
            override val typ: String = OutgoingHeaderType.END.code
        }

        @Serializable
        data class Error(override val ref: String, val msg: String): RefOutgoing {
            override val typ: String = OutgoingHeaderType.ERROR.code
        }
    }

    @Serializable
    data class Event(val channel: String, val seq: ULong): Outgoing {
        override val typ: String = OutgoingHeaderType.EVENT.code
    }
}

@Serializable
class IncomingMessage<T: Any>(val header: Incoming, val body: T)