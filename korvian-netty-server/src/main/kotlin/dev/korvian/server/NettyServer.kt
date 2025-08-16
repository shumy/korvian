package dev.korvian.server

import dev.korvian.pipeline.Pipeline
import dev.korvian.server.handler.HttpServerHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpServerCodec

class NettyServer() {
    private val pipelines = mutableMapOf<String, Pipeline<String, String>>()

    fun setPipelineAt(pipeline: Pipeline<String, String>, uri: String) {
        val correctUri = if (uri.startsWith("/")) uri else "/$uri"
        pipelines[correctUri] = pipeline
    }

    fun start(port: Int) {
        val workerGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            val srv = ServerBootstrap()
                .option(ChannelOption.SO_BACKLOG, 1024)
                .group(workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(HTTPInitializer(pipelines))

            val ch = srv.bind(port).sync().channel()
            ch.closeFuture().sync()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }
}

private class HTTPInitializer(private val pipelines: Map<String, Pipeline<String, String>>): ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline()
            .addLast("HttpServerCodec", HttpServerCodec())
            .addLast("HttpServerHandler", HttpServerHandler(pipelines))
    }
}