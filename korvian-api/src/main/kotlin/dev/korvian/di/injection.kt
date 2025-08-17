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

inline fun <reified T: Any, reified C: IChannel<T>?> channel(name: String): C {
    val channel = Store.Channel.getUndefined(T::class, name)
    if (null !is C && channel === null)
        throw DIException("Channel for ${T::class.simpleName}:$name not found!")

    return channel as C
}

inline fun <reified T: Any, reified S: ISource<T>?> source(name: String): S {
    val source: ISource<T>? = channel<T, IChannel<T>?>(name)?.source
    if (null !is S && source === null)
        throw DIException("Source for ${T::class.simpleName}:$name not found!")

    return source as S
}

inline fun <reified T: Any, reified S: ISink<T>?> sink(name: String): S {
    val sink: ISink<T>? = channel<T, IChannel<T>?>(name)?.sink
    if (null !is S && sink === null)
        throw DIException("Sink for ${T::class.simpleName}:$name not found!")

    return sink as S
}

inline fun <reified T> context(): T {
    val ctx = Store.Context.getUndefined(T::class)
    if (null !is T && ctx === null)
        throw DIException("Context for ${T::class.simpleName} not found!")

    return ctx as T
}

inline fun <reified T: Any> context(obj: T): Unit {
    Store.Context.add(T::class, obj)
}