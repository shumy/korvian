import dev.korvian.di.ServiceStore

fun main() {
    ServiceStore.add(IHelloService::class)

    val all = listOf("X", "Y")
    println("Test $all")
}