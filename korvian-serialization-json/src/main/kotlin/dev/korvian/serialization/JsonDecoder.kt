package dev.korvian.serialization

import dev.korvian.di.store.EndpointParam
import dev.korvian.pipeline.IDecoder
import dev.korvian.pipeline.Incoming
import dev.korvian.pipeline.IncomingHeaderType
import dev.korvian.pipeline.IncomingMessage
import dev.korvian.pipeline.PipeException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType

class JsonDecoder: IDecoder<String, JsonElement> {
    override fun preDecode(msg: String): IncomingMessage<JsonElement> {
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

    override fun bodyDecode(body: JsonElement, params: List<EndpointParam>): Array<Any?> {
        return params
            .map { decodeBodyElement(it.kType, body.jsonObject[it.name]) }
            .toTypedArray()
    }

    private fun decodeBodyElement(kType: KType, bodyElement: JsonElement?): Any? {
        if (bodyElement === null)
            return null

        return json.decodeFromJsonElement(serializer(kType), bodyElement)
    }
}