import com.lectra.koson.obj
import dev.korvian.di.Store
import dev.korvian.pipeline.IPipeline
import dev.korvian.pipeline.JsonDecoder
import dev.korvian.pipeline.JsonEncoder
import dev.korvian.pipeline.Pipeline
import dev.korvian.pipeline.Router
import handler.HelloChannelHandler

fun main() {
    Store.Channel.add(String::class, HelloChannelHandler())
    Store.Service += IHelloService::class

    val pipeline: IPipeline = Pipeline(JsonDecoder(), JsonEncoder(), Router(Store.Service))

    val msg = obj {
        "head" to obj {
            "typ" to "req"
            "ref" to "ref-1"
            "srv" to "IHelloService"
            "trg" to "simpleHello"
        }

        "body" to obj {

        }
    }

    pipeline.process(msg.toString().toByteArray()) {
        println(it)
    }
}