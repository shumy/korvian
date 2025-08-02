package handler

import dev.korvian.IChannel
import dev.korvian.ISink
import dev.korvian.ISource
import dev.korvian.ISubscription

class HelloChannelHandler: IChannel<String> {
    override val name = "ch:hello"

    override val source = object: ISource<String> {
        override fun subscribe(): ISubscription<String> {
            TODO("Not yet implemented")
        }
    }

    override val sink = object: ISink<String> {
        override fun publish(data: String) {
            println(data)
        }
    }
}