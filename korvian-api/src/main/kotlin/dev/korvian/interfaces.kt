package dev.korvian

interface ISink<T: Any> {
    fun publish(data: T)
}

interface ISource<T: Any> {
    fun subscribe(): ISubscription<T>
}

interface ISubscription<T: Any> {
    fun on(handler: (msg: T) -> Unit)
}

fun interface IStream<T: Any> {
    fun invoke(sink: ISink<T>)
}