package dev.korvian.di

import dev.korvian.IChannel
import kotlin.reflect.KClass

object ChannelStore {
    private val channels = mutableMapOf<KClass<*>, Map<String, IChannel<*>>>()

    // TODO: match KClass with assignable?
    // TODO: search for channels via filter match?

    fun <T: Any> get(dataType: KClass<T>, name: String): IChannel<T> {
        TODO()
    }

    fun <T: Any> add(dataType: KClass<T>, channel: IChannel<T>) {
        TODO()
    }
}