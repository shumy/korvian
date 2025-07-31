import dev.korvian.Doc
import dev.korvian.IStream
import dev.korvian.Request
import dev.korvian.Service

@Service(HelloServiceHandler::class)
interface IHelloService {
    @Request
    @Doc("A simple hello method, returning the compliment.")
    fun simpleHello(name: String): String

    @Request
    fun multipleHello(names: List<String>): IStream<String>

}

class HelloServiceHandler: IHelloService {
    override fun simpleHello(name: String) =
        "simpleHello $name"

    override fun multipleHello(names: List<String>): IStream<String> {
        // make some validations before accept request!
        for (name in names)
            if (name == "Alex")
                throw Exception("Sorry, Alex is a terrorist!")

        return IStream {
            for (name in names)
                it.publish("multipleHello $name")
        }
    }

}

fun main() {
    val all = listOf("X", "Y")
    println("Test $all")
}