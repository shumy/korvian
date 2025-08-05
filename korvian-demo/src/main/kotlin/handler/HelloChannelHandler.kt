package handler

import dev.korvian.IChannel
import dev.korvian.ISink
import dev.korvian.ISource
import dev.korvian.ISubscription
import java.util.UUID

class HelloChannelHandler: IChannel<String> {
    override val name = "ch:hello"

    override val source = ISource<String> {
        object: ISubscription<String> {
            override val id = UUID.randomUUID().toString()
            override fun on(handler: (msg: String) -> Unit) {
                TODO("Not yet implemented")
            }
        }
    }

    override val sink = ISink<String> {
        println(it)
    }
}