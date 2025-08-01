package dev.korvian.di

import dev.korvian.IChannel
import dev.korvian.ISink
import dev.korvian.ISource

class DIException(msg: String): RuntimeException(msg)

inline fun <reified T> service(): T {
    val srv = Store.Service.getUndefined(T::class)
    if (null !is T && srv === null)
        throw DIException("Service ${T::class.simpleName} not found!")

    return srv as T
}

inline fun <reified T: Any> channel(channel: String): IChannel<T> =
    Store.Channel.get(T::class, channel)

inline fun <reified T: Any> channelOptional(channel: String): IChannel<T>? =
    Store.Channel.getOptional(T::class, channel)

inline fun <reified T: Any> source(channel: String): ISource<T> =
    channel<T>(channel).source

inline fun <reified T: Any> sourceOptional(channel: String): ISource<T>? =
    channelOptional<T>(channel)?.source

inline fun <reified T: Any> sink(channel: String): ISink<T> =
    channel<T>(channel).sink

inline fun <reified T: Any> sinkOptional(channel: String): ISink<T>? =
    channelOptional<T>(channel)?.sink