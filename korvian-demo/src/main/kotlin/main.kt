import com.lectra.koson.arr
import com.lectra.koson.obj
import dev.korvian.Request
import dev.korvian.Service
import dev.korvian.di.Store
import dev.korvian.pipeline.CheckError
import dev.korvian.pipeline.ErrorCode
import dev.korvian.pipeline.ICheck
import dev.korvian.pipeline.Pipeline
import dev.korvian.serialization.JsonSerializer
import handler.HelloChannelHandler

fun main() {
    Store.Channel.add(String::class, HelloChannelHandler())
    Store.Service += IHelloService::class

    val pipeline = Pipeline(Store.Service, JsonSerializer())

    pipeline.addCheck(ICheck<Request> { anno, spec ->
        println("Custom check on endpoint ${spec.name} with annotation $anno")
        if (spec.name == "IHelloService:simpleHello")
            throw CheckError(ErrorCode.Forbidden)
    })

    pipeline.addCheck(Service::class) { _, spec ->
        println("Top Service annotation check on endpoint ${spec.name}")
    }

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
            "ref" to "ref-2"
            "srv" to "IHelloService"
            "trg" to "simpleHello"
        }

        "name" to "Alex"
    }
    connection.process(simpleHello.toString())
}