package dev.korvian.server.handler

import dev.korvian.pipeline.Connection
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

internal class WebSocketHandler(private val connection: Connection<String, String>): ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
        if (msg is WebSocketFrame) {
            when (msg) {
                is TextWebSocketFrame -> {
                    println("TextWebSocketFrame Received: ${msg.text()}")
                    connection.process(msg.text())
                }

                is CloseWebSocketFrame -> {
                    println("CloseWebSocketFrame Received: (reason=${msg.reasonText()}, code=${msg.statusCode()})")
                    ctx.flush()
                    ctx.close()
                    connection.close()
                }

                is PingWebSocketFrame -> {
                    println("PingWebSocketFrame Received")
                    ctx.channel().writeAndFlush(PongWebSocketFrame())
                }

                is PongWebSocketFrame -> {
                    println("PongWebSocketFrame Received")
                }

                else -> {
                    println("Received Unsupported WebSocketFrame: " + ctx.channel())
                }
            }
        }
    }
}