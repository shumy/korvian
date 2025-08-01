import dev.korvian.IChannel
import dev.korvian.ISink
import dev.korvian.ISource
import dev.korvian.ISubscription
import dev.korvian.di.Store
import dev.korvian.di.service

fun main() {
    val helloSource = object: ISource<String> {
        override fun subscribe(): ISubscription<String> {
            TODO("Not yet implemented")
        }
    }

    val helloSink = object: ISink<String> {
        override fun publish(data: String) {
            println(data)
        }

    }

    val helloChannel = object: IChannel<String> {
        override val name = "ch:hello"
        override val source = helloSource
        override val sink = helloSink
    }

    Store.Channel.add(String::class, helloChannel)
    Store.Service += IHelloService::class

    val srv = service<IHelloService>()
    srv.pubHello("Test")
}