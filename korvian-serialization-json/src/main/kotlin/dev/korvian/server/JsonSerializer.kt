package dev.korvian.server

import dev.korvian.di.store.EndpointParam
import dev.korvian.pipeline.ISerializer
import dev.korvian.pipeline.Incoming
import dev.korvian.pipeline.IncomingHeaderType
import dev.korvian.pipeline.IncomingMessage
import dev.korvian.pipeline.Outgoing
import dev.korvian.pipeline.OutgoingHeaderType
import dev.korvian.pipeline.PipeException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class JsonSerializer: ISerializer<String, String> {
    override fun preDecode(msg: String): IncomingMessage {
        val jsonElement = json.decodeFromString<JsonElement>(msg)
        val jsonHead = jsonElement.jsonObject["head"] ?: throw PipeException("'head' missing in message!")
        val jsonHeadType = jsonHead.jsonObject["typ"] ?: throw PipeException("'head.typ' missing in message!")
        val type = IncomingHeaderType.fromCode(jsonHeadType.jsonPrimitive.content)

        val head = when(type) {
            IncomingHeaderType.REQUEST -> json.decodeFromJsonElement<Incoming.ServiceIncoming.Request>(jsonHead)
            IncomingHeaderType.PUBLISH -> json.decodeFromJsonElement<Incoming.ServiceIncoming.Publish>(jsonHead)
            IncomingHeaderType.SUBSCRIBE -> json.decodeFromJsonElement<Incoming.ServiceIncoming.Subscribe>(jsonHead)
            IncomingHeaderType.UNSUBSCRIBE -> json.decodeFromJsonElement<Incoming.UnSubscribe>(jsonHead)
        }

        return IncomingMessage(head, jsonElement)
    }

    override fun bodyDecode(body: Any?, params: List<EndpointParam>): Array<Any?> {
        val bodyElement = body as JsonElement?
        return params
            .map { decodeBodyElement(it.kType, bodyElement?.jsonObject[it.name]) }
            .toTypedArray()
    }

    override fun encode(header: Outgoing, rType: KType?, result: Any?): String {
        val type = OutgoingHeaderType.fromCode(header.typ)
        val jsonHead = when(type) {
            OutgoingHeaderType.REPLY -> json.encodeToString(header as Outgoing.RefOutgoing.Reply)
            OutgoingHeaderType.ACCEPT -> json.encodeToString(header as Outgoing.RefOutgoing.Accept)
            OutgoingHeaderType.REJECT -> json.encodeToString(header as Outgoing.RefOutgoing.Reject)
            OutgoingHeaderType.NEXT -> json.encodeToString(header as Outgoing.RefOutgoing.Next)
            OutgoingHeaderType.END -> json.encodeToString(header as Outgoing.RefOutgoing.End)
            OutgoingHeaderType.ERROR -> json.encodeToString(header as Outgoing.RefOutgoing.Error)
            OutgoingHeaderType.EVENT -> json.encodeToString(header as Outgoing.Event)
        }

        if (rType !== null && result !== null) {
            val jsonResult = when (rType) {
                typeOf<LocalDate>() -> json.encodeToString(LocalDateSerializer, result as LocalDate)
                typeOf<LocalTime>() -> json.encodeToString(LocalTimeSerializer, result as LocalTime)
                typeOf<LocalDateTime>() -> json.encodeToString(LocalDateTimeSerializer, result as LocalDateTime)
                else -> json.encodeToString(serializer(rType), result)
            }

            return """{"head":$jsonHead,"res":$jsonResult}"""
        }

        return """{"head":$jsonHead}"""
    }

    private fun decodeBodyElement(kType: KType, bodyElement: JsonElement?): Any? {
        if (bodyElement === null)
            return null

        return json.decodeFromJsonElement(serializer(kType), bodyElement)
    }
}