package dev.korvian.di.store

import dev.korvian.IChannel
import dev.korvian.Service
import dev.korvian.di.DIException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

class ServiceStore {
    private val store = mutableMapOf<KClass<*>, Any>()

    fun getUndefined(type: KClass<*>): Any? = store[type]

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getOptional(type: KClass<T>): T? =
        store[type] as T?

    fun <T: Any> get(type: KClass<T>): T =
        getOptional(type) ?: throw DIException("Service ${type.simpleName} not found!")

    operator fun plusAssign(type: KClass<*>) = add(type)
    fun add(type: KClass<*>) {
        if(store.contains(type))
            throw DIException("Service ${type.simpleName} is already available in store!")

        if(!type.java.isInterface)
            throw DIException("Service ${type.simpleName} is not an interface!")

        val annService = type.findAnnotations(Service::class).firstOrNull()
            ?: throw DIException("Service ${type.simpleName} requires @Service annotation!")

        if(!type.isSuperclassOf(annService.handler))
            throw DIException("Handler for service ${type.simpleName} doesn't implement the service interface!")

        val defaultConstructor = annService.handler.primaryConstructor!!
        if (defaultConstructor.parameters.isNotEmpty())
            throw DIException("Service ${type.simpleName} with parameters is not supported!")

        // TODO: setup routes for @Request / @Publish / @Subscribe
        // TODO: check function params and return values!

        store[type] = defaultConstructor.call()
    }
}