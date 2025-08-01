package dev.korvian.di.store

import dev.korvian.IChannel
import dev.korvian.di.DIException
import kotlin.reflect.KClass

class ChannelStore {
    private val store = mutableMapOf<KClass<*>, MutableMap<String, IChannel<*>>>()

    // TODO: match KClass with assignable?
    // TODO: search for channels via filter match?

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getOptional(type: KClass<T>, name: String): IChannel<T>? =
        store[type]?.get(name) as IChannel<T>?

    fun <T: Any> get(type: KClass<T>, name: String): IChannel<T> =
        getOptional(type, name)
            ?: throw DIException("Channel for ${type.simpleName}:$name not found!")

    fun <T: Any> add(type: KClass<T>, channel: IChannel<T>) {
        val typeStore = store.getOrPut(type) { mutableMapOf() }
        if (typeStore[channel.name] != null)
            throw DIException("Channel for ${type.simpleName}:${channel.name} is already available in store!")

        typeStore[channel.name] = channel
    }
}