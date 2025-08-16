package dev.korvian.server.handler

import dev.korvian.RejectError
import dev.korvian.pipeline.ConnectionInfo
import dev.korvian.pipeline.ErrorCode
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import java.util.Base64
import kotlin.text.split

object ConnectionInitiator {
    fun extractConnectionInfo(req: HttpRequest): ConnectionInfo {
        val origin = req.headers().get(HttpHeaderNames.ORIGIN) ?:
            throw RejectError(ErrorCode.ConnectionRejected.code, "Expecting Origin header!")

        val uri = req.uri() ?:
            throw RejectError(ErrorCode.ConnectionRejected.code, "Expecting uri!")

        val encodedHeaders = req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL) ?:
            throw RejectError(ErrorCode.ConnectionRejected.code, "Expecting header: ${HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL}")

        val multiEncodedHeaders = encodedHeaders.split(",").map { it.trim() }
        if (multiEncodedHeaders.size != 2 && multiEncodedHeaders[0] != "korvian")
            throw RejectError(ErrorCode.ConnectionRejected.code, "Expecting korvian protocol initiator!")

        try {
            val rawHeaders = Base64.getUrlDecoder().decode(multiEncodedHeaders[1]).toString(Charsets.UTF_8)
            val headers = mutableMapOf<String, String>()
            rawHeaders.lines().forEach {
                val line = it.split(":", limit = 2)
                if (line.size != 2)
                    throw RejectError(ErrorCode.ConnectionRejected.code, "Invalid korvian headers!")
                headers[line[0].trim()] = line[1].trim()
            }

            return ConnectionInfo(origin, uri, headers)
        } catch (ex: IllegalArgumentException) {
            throw RejectError(ErrorCode.ConnectionRejected.code, "Base64URL decoding error for korvian header! ${ex.message}")
        }
    }

    fun getWebSocketServerHandshaker(ctx: ChannelHandlerContext, req: HttpRequest): WebSocketServerHandshaker? {
        // TODO: URL for secured connection
        val wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + req.uri()

        val wsFactory = WebSocketServerHandshakerFactory(wsUrl, "korvian", true)
        val handshaker = wsFactory.newHandshaker(req)
        if (handshaker === null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
            return null
        }

        return handshaker
    }

    fun reject(channel: Channel, error: RejectError) {
        val status = when(error.code) {
            ErrorCode.ConnectionRejected.code -> HttpResponseStatus.BAD_REQUEST
            ErrorCode.Unauthorized.code -> HttpResponseStatus.UNAUTHORIZED
            ErrorCode.Forbidden.code -> HttpResponseStatus.FORBIDDEN
            else -> {
                channel.close()
                return
            }
        }

        val res = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
        res.headers().set("Sec-Websocket-Reject", error.reason)
        channel.writeAndFlush(res)
        channel.close()
    }
}