package handler

import IHelloService
import dev.korvian.IReply
import dev.korvian.IStream
import dev.korvian.ISubscription
import dev.korvian.di.sink
import dev.korvian.di.source

class HelloServiceHandler: IHelloService {
    val helloSource = source<String>("ch:hello")
    val helloSink = sink<String>("ch:hello")

    override fun pubHello(name: String) {
        helloSink.publish("pubHello $name")
    }

    override fun subHello(): ISubscription<String> =
        helloSource.subscribe()

    // This will send the accept signal at the same time as the response
    override fun simpleHello(name: String) =
        "simpleHello $name"

    // This will send the accept signal before starting the reply process
    override fun deferredHello(name: String): IReply<String> {
        if (name == "Alex")
            throw Exception("Sorry, Alex is a terrorist!")

        return { "deferredHello $name" }
    }

    // This will send the accept signal before starting the stream process
    override fun multipleHello(names: List<String>): IStream<String> {
        // make some validations before accept request!
        for (name in names) {
            if (name == "Alex")
                throw Exception("Sorry, Alex is a terrorist!")
        }

        return {
            for (name in names)
                it.publish("multipleHello $name")
        }
    }
}