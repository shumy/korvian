package dev.korvian

typealias IReply<T> = () -> T
typealias IStream<T> = (sink: ISink<T>) -> Unit

interface IChannel<T: Any> {
    val name: String
    val source: ISource<T>
    val sink: ISink<T>
}

fun interface ISink<T: Any> {
    fun publish(data: T)
}

fun interface ISource<T: Any> {
    // TODO: support filters?
    fun subscribe(): ISubscription<T>
}

interface ISubscription<T: Any> {
    val id: String
    fun on(handler: (msg: T) -> Unit)
}