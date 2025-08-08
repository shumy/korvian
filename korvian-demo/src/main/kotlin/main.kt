import com.lectra.koson.arr
import com.lectra.koson.obj
import dev.korvian.di.Store
import dev.korvian.pipeline.Pipeline
import dev.korvian.serialization.JsonDecoder
import dev.korvian.serialization.JsonEncoder
import handler.HelloChannelHandler

fun main() {
    Store.Channel.add(String::class, HelloChannelHandler())
    Store.Service += IHelloService::class

    val pipeline = Pipeline(Store.Service, JsonDecoder(), JsonEncoder())

    val connection = pipeline.connect {
        println(it)
    }

    val multipleHello = obj {
        "head" to obj {
            "typ" to "req"
            "ref" to "ref-1"
            "srv" to "IHelloService"
            "trg" to "multipleHello"
        }

        "names" to arr["Pedro", "Mica", "Lima"]
    }
    connection.process(multipleHello.toString())

    val simpleHello = obj {
        "head" to obj {
            "typ" to "req"
            "ref" to "ref-1"
            "srv" to "IHelloService"
            "trg" to "simpleHello"
        }

        "name" to "Alex"
    }
    connection.process(simpleHello.toString())
}