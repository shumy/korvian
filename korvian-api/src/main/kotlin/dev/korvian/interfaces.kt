package dev.korvian

typealias IReply<T> = () -> T
typealias IStream<T> = (sink: ISink<T>) -> Unit

interface IChannel<T: Any> {
    val name: String

    fun source(): ISource<T>
    fun sink(): ISink<T>
}

interface ISink<T: Any> {
    fun publish(data: T)
}

interface ISource<T: Any> {
    // TODO: support filters?
    fun subscribe(): ISubscription<T>
}

interface ISubscription<T: Any> {
    fun on(handler: (msg: T) -> Unit)
}