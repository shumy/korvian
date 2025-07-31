package dev.korvian.di

import dev.korvian.IChannel
import dev.korvian.ISink
import dev.korvian.ISource

inline fun <reified T: Any> service(): T =
    ServiceStore.get(T::class)

inline fun <reified T: Any> channel(channel: String): IChannel<T> =
    ChannelStore.get(T::class, channel)

inline fun <reified T: Any> source(channel: String): ISource<T> =
    channel<T>(channel).source()

inline fun <reified T: Any> sink(channel: String): ISink<T> =
    channel<T>(channel).sink()