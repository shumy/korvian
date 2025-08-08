package handler

import IHelloService
import dev.korvian.IReply
import dev.korvian.ISink
import dev.korvian.ISource
import dev.korvian.IStream
import dev.korvian.ISubscription
import dev.korvian.RejectError
import dev.korvian.di.sink
import dev.korvian.di.source
import java.time.LocalDateTime

class HelloServiceHandler: IHelloService {
    val helloSource: ISource<String> = source("ch:hello")
    val helloSink: ISink<String> = sink("ch:hello")

    override fun pubHello(name: String) {
        helloSink.publish("pubHello $name")
    }

    override fun subHello(): ISubscription<String> =
        helloSource.subscribe()

    // This will send the accept signal at the same time as the response
    override fun simpleHello(name: String): LocalDateTime {
        println("Running simpleHello!")
        return LocalDateTime.now()
    }

    // This will send the accept signal before starting the reply process
    override fun deferredHello(name: String): IReply<String> {
        if (name == "Alex")
            throw RejectError("Sorry, Alex is a terrorist!")

        return IReply { "deferredHello $name" }
    }

    // This will send the accept signal before starting the stream process
    override fun multipleHello(names: List<String>): IStream<String> {
        // make some validations before accept request!
        for (name in names) {
            if (name == "Alex")
                throw RejectError("Sorry, Alex is a terrorist!")
        }

        return IStream {
            for (name in names)
                it.publish("multipleHello $name")
        }
    }
}