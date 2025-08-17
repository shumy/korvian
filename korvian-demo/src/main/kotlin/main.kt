import com.lectra.koson.arr
import com.lectra.koson.obj
import dev.korvian.RejectError
import dev.korvian.Request
import dev.korvian.Service
import dev.korvian.di.Store
import dev.korvian.di.context
import dev.korvian.pipeline.CheckError
import dev.korvian.pipeline.ErrorCode
import dev.korvian.pipeline.IEndpointCheck
import dev.korvian.pipeline.Pipeline
import dev.korvian.server.JsonSerializer
import dev.korvian.server.NettyServer
import handler.HelloChannelHandler
import handler.Origin


fun main() {
    Store.Channel.add(String::class, HelloChannelHandler())
    Store.Service += IHelloService::class

    val pipeline = Pipeline(Store.Service, JsonSerializer()).apply {
        addConnectionCheck {
            if (it.origin != "http://localhost:8080")
                throw RejectError(ErrorCode.Unauthorized.code, "Requires Origin to be localhost")

            //TODO: add connection info to context - setContext(Roles)
            context(Origin(it.origin))
        }

        addEndpointCheck(IEndpointCheck<Request> { anno, spec ->
            println("Custom check on endpoint ${spec.name} with annotation $anno")
            if (spec.name == "IHelloService:simpleHello")
                throw CheckError(ErrorCode.Forbidden)
        })

        addEndpointCheck(Service::class) { _, spec ->
            println("Top Service annotation check on endpoint ${spec.name}")

            //TODO: check roles from connection context - context<Roles>()
            val origin = context<Origin?>()
            println("ORIGIN - $origin")
        }
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

    println(multipleHello.toString())
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

    println(simpleHello.toString())
    connection.process(simpleHello.toString())

    NettyServer().apply {
        setPipelineAt(pipeline, "/api")
        start(8080)
    }
}