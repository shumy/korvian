package dev.korvian

fun interface IReply<T: Any> {
    fun invoke(): T
}

fun interface IReplyTask<T: Any> {
    suspend fun invoke(): T
}

fun interface IStream<T: Any> {
    fun invoke(sink: ISink<T>): Unit
}

fun interface IStreamTask<T: Any> {
    suspend fun invoke(sink: ISink<T>): Unit
}

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

    fun unsubscribe()
    fun on(handler: (msg: T) -> Unit)
}