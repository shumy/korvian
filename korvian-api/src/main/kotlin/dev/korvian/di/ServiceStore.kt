package dev.korvian.di

import dev.korvian.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

object ServiceStore {
    private val services = mutableMapOf<KClass<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(dataType: KClass<T>): T {
        val srv = services[dataType]
            ?: throw DIException("[DI] Service ${dataType.simpleName} not found!")
        return srv as T
    }

    fun <T: Any> add(spec: KClass<T>) {
        if(services.contains(spec))
            throw DIException("[DI] Service ${spec.simpleName} is already available in store!")

        if(!spec.java.isInterface)
            throw DIException("[DI] Service ${spec.simpleName} is not an interface!")

        val annService = spec.findAnnotations(Service::class).firstOrNull()
            ?: throw DIException("[DI] Service ${spec.simpleName} requires @Service annotation!")

        if(!spec.isSuperclassOf(annService.handler))
            throw DIException("[DI] Handler for service ${spec.simpleName} doesn't implement the service interface!")

        val defaultConstructor = annService.handler.primaryConstructor!!
        if (defaultConstructor.parameters.isNotEmpty())
            throw DIException("[DI] Service ${spec.simpleName} with parameters is not supported!")

        // TODO: setup routes for @Request / @Publish / @Subscribe
        // TODO: check function params and return values!

        services[spec] = defaultConstructor.call()
    }
}

class DIException(msg: String): RuntimeException(msg)