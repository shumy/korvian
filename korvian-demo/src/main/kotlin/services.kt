import dev.korvian.*
import handler.HelloServiceHandler

@Service(HelloServiceHandler::class)
interface IHelloService {
    @Publish
    @Doc("Publish a compliment in the chHello channel.")
    fun pubHello(@Doc("The name to compliment.") name: String)

    @Subscribe
    fun subHello(): ISubscription<String>

    @Request
    @Doc("A simple hello method, returning the compliment.")
    fun simpleHello(name: String): String

    @Request
    @Doc("A deferred hello method, returning the compliment.")
    fun deferredHello(name: String): IReply<String>

    @Request
    fun multipleHello(names: List<String>): IStream<String>
}

