package dev.korvian.serialization

import dev.korvian.pipeline.IEncoder
import dev.korvian.pipeline.Outgoing
import dev.korvian.pipeline.OutgoingHeaderType
import kotlinx.serialization.serializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class JsonEncoder: IEncoder<String> {
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
}