package dev.korvian.pipeline

import dev.korvian.di.store.EndpointParam
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream

class JsonDecoder: IDecoder<JsonElement> {
    val json = Json {
        ignoreUnknownKeys = true
    }

    override fun preDecode(msg: ByteArray): IncomingMessage<JsonElement> {
        val jsonElement = decodeByteArrayToJsonElement(msg)
        val jsonHead = jsonElement.jsonObject["head"] ?: throw PipeException("'head' missing in message!")
        val jsonHeadType = jsonHead.jsonObject["typ"] ?: throw PipeException("'head.typ' missing in message!")
        val type = IncomingHeaderType.fromCode(jsonHeadType.jsonPrimitive.content)

        val head = when(type) {
            IncomingHeaderType.REQUEST -> json.decodeFromJsonElement<Incoming.Request>(jsonHead)
            IncomingHeaderType.PUBLISH -> json.decodeFromJsonElement<Incoming.Publish>(jsonHead)
            IncomingHeaderType.SUBSCRIBE -> json.decodeFromJsonElement<Incoming.Subscribe>(jsonHead)
        }

        val body = jsonElement.jsonObject["body"] ?: throw PipeException("'body' missing in message!")
        return IncomingMessage(head, body)
    }

    override fun bodyDecode(body: JsonElement, params: List<EndpointParam>): Array<Any?> {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun decodeByteArrayToJsonElement(bytes: ByteArray): JsonElement {
        val inputStream = ByteArrayInputStream(bytes)
        return json.decodeFromStream<JsonElement>(inputStream)
    }
}

class JsonEncoder: IEncoder<JsonElement> {
    override fun encode(header: Outgoing, data: JsonElement?): ByteArray {
        TODO("Not yet implemented")
    }
}