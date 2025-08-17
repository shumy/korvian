package dev.korvian.server.handler

import dev.korvian.RejectError
import dev.korvian.pipeline.ErrorCode
import dev.korvian.pipeline.Pipeline
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.util.*

internal class HttpServerHandler(private val pipelines: Map<String, Pipeline<String, String>>): ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
        if (msg is HttpRequest) {
            println("Http Request Received")
            val headers = msg.headers()

            val connectionHeader = headers.get(HttpHeaderNames.CONNECTION)?.lowercase(Locale.getDefault())
            val upgradeHeader = headers.get(HttpHeaderNames.UPGRADE)?.lowercase(Locale.getDefault())
            if (connectionHeader == "upgrade" && upgradeHeader == "websocket")
                try {
                    tryConnect(ctx, msg)
                } catch (ex: RejectError) {
                    ConnectionInitiator.reject(ctx.channel(), ex)
                }

        }
    }

    private fun tryConnect(ctx: ChannelHandlerContext, req: HttpRequest) {
        val handshaker = ConnectionInitiator.getWebSocketServerHandshaker(ctx, req) ?: return
        val connInfo = ConnectionInitiator.extractConnectionInfo(req)
        val pipeline = pipelines[connInfo.uri] ?:
            throw RejectError(ErrorCode.ConnectionRejected.code, "No pipeline found for URI: ${connInfo.uri}")

        pipeline.checkConnection(connInfo)

        println("Opened Channel : " + ctx.channel())
        val connection = pipeline.connect {
            val evtFrame = TextWebSocketFrame(it)
            // TODO: flush immediately or in time intervals?
            ctx.channel().writeAndFlush(evtFrame)
        }

        ctx.pipeline().replace(this, "WebSocketHandler", WebSocketHandler(connection))
        handshaker.handshake(ctx.channel(), req)
    }
}