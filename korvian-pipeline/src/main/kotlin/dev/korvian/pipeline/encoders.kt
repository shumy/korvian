package dev.korvian.pipeline

import dev.korvian.di.store.EndpointParam
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType

private val json = Json {
    ignoreUnknownKeys = true
}

class JsonDecoder: IDecoder<String, JsonElement> {
    override fun preDecode(msg: String): IncomingMessage<JsonElement> {
        val jsonElement = json.decodeFromString<JsonElement>(msg)
        val jsonHead = jsonElement.jsonObject["head"] ?: throw PipeException("'head' missing in message!")
        val jsonHeadType = jsonHead.jsonObject["typ"] ?: throw PipeException("'head.typ' missing in message!")
        val type = IncomingHeaderType.fromCode(jsonHeadType.jsonPrimitive.content)

        val head = when(type) {
            IncomingHeaderType.REQUEST -> json.decodeFromJsonElement<Incoming.Request>(jsonHead)
            IncomingHeaderType.PUBLISH -> json.decodeFromJsonElement<Incoming.Publish>(jsonHead)
            IncomingHeaderType.SUBSCRIBE -> json.decodeFromJsonElement<Incoming.Subscribe>(jsonHead)
        }

        return IncomingMessage(head, jsonElement)
    }

    override fun bodyDecode(body: JsonElement, params: List<EndpointParam>): Array<Any?> {
        return params
            .map { decodeBodyElement(it.kType, body.jsonObject[it.name]) }
            .toTypedArray()
    }

    fun decodeBodyElement(kType: KType, bodyElement: JsonElement?): Any? {
        if (bodyElement === null)
            return null

        return json.decodeFromJsonElement(serializer(kType), bodyElement)
    }
}

class JsonEncoder: IEncoder<String> {
    override fun encode(header: Outgoing, kType: KType?, body: Any?): String {
        val type = OutgoingHeaderType.fromCode(header.typ)
        val head = when(type) {
            OutgoingHeaderType.REPLY -> json.encodeToString(header as Outgoing.RefOutgoing.Reply)
            OutgoingHeaderType.ACCEPT -> json.encodeToString(header as Outgoing.RefOutgoing.Accept)
            OutgoingHeaderType.REJECT -> json.encodeToString(header as Outgoing.RefOutgoing.Reject)
            OutgoingHeaderType.NEXT -> json.encodeToString(header as Outgoing.RefOutgoing.Next)
            OutgoingHeaderType.END -> json.encodeToString(header as Outgoing.RefOutgoing.End)
            OutgoingHeaderType.ERROR -> json.encodeToString(header as Outgoing.RefOutgoing.Error)
            OutgoingHeaderType.EVENT -> json.encodeToString(header as Outgoing.Event)
        }

        if (kType !== null && body !== null) {
            val body = json.encodeToString(serializer(kType), body)
            return """{"head":$head,$body}"""
        }

        return """{"head":$head}"""
    }
}