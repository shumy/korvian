package dev.korvian.di.store

import dev.korvian.ISubscription
import dev.korvian.Publish
import dev.korvian.Request
import dev.korvian.Service
import dev.korvian.Subscribe
import dev.korvian.di.DIException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

enum class EndpointType { REQUEST, PUBLISH, SUBSCRIBE }
data class EndpointParam(val type: KClass<*>, val name: String, val isOptional: Boolean)
data class Endpoint(val type: EndpointType, val name: String, val exec: EndpointExecutor, val params: List<EndpointParam>, val retType: EndpointParam)

fun interface EndpointExecutor {
    fun process(vararg args: Any?): Any?
}

class ServiceStore {
    private val store = mutableMapOf<String, Any>()
    private val endpoints = mutableMapOf<String, Endpoint>()

    fun getUndefined(type: KClass<*>): Any? = store[type.qualifiedName!!]

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getOptional(type: KClass<T>): T? =
        store[type.qualifiedName!!] as T?

    fun <T: Any> get(type: KClass<T>): T =
        getOptional(type) ?: throw DIException("Service ${type.simpleName} not found!")

    fun resolveService(srv: String) = store[srv]

    fun resolveEndpoint(type: EndpointType, target: String): Endpoint? {
        val endpoint = endpoints[target]
        if (endpoint?.type !== type)
            return null

        return endpoint
    }

    operator fun plusAssign(type: KClass<*>) = add(type)
    fun add(type: KClass<*>) {
        val typeName = type.qualifiedName!!
        if(store.contains(typeName))
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

        for (eMember in type.declaredMembers) {
            val endpoint = getEndpoint(typeName, eMember)
            endpoints[endpoint.name] = endpoint
        }

        store[typeName] = defaultConstructor.call()
    }

    private fun getEndpoint(typeName: String, eMember: KCallable<*>): Endpoint {
        val endpointName = "$typeName:${eMember.name}"
        var eType: EndpointType? = null

        if (eMember.hasAnnotation<Request>())
            eType = EndpointType.REQUEST

        if (eMember.hasAnnotation<Publish>()) {
            if (eType != null)
                throw DIException("Service method $endpointName with multiple endpoint annotations!")
            eType = EndpointType.PUBLISH
        }

        if (eMember.hasAnnotation<Subscribe>()) {
            if (eType != null)
                throw DIException("Service method $endpointName with multiple endpoint annotations!")
            eType = EndpointType.SUBSCRIBE
        }

        eType ?: throw DIException("Service method $endpointName requires an endpoint annotation!")

        val executor = EndpointExecutor { eMember.call(it) }
        val params = eMember.parameters.drop(1).map { checkAndConvertParameter(endpointName, it) }
        val retType = checkAndConvertReturnType(endpointName, eType, eMember.returnType)

        return Endpoint(eType, endpointName, executor, params, retType)
    }
}

private fun checkAndConvertParameter(endpoint: String, param: KParameter): EndpointParam {
    if (param.isVararg)
        throw DIException("Varargs are not supported in service methods! Param ${param.name} for $endpoint.")

    val type = param.type.classifier!! as KClass<*>

    // TODO: check supported types (native, @Serializable)
    println("${param.type.classifier!!} - ${param.type == typeOf<String>()}")

    return EndpointParam(type, param.name!!, param.isOptional)
}

private fun checkAndConvertReturnType(endpoint: String, eType: EndpointType, retType: KType): EndpointParam {
    val type = retType.classifier!! as KClass<*>

    if (eType == EndpointType.PUBLISH && type != Unit::class)
        throw DIException("Return value is not supported @Publish methods! Return type $retType for $endpoint.")

    if (eType == EndpointType.SUBSCRIBE && type != ISubscription::class)
        throw DIException("Expecting ISubscription return value for @Subscribe method! Return type $retType for $endpoint.")

    // TODO: check return type for REQUEST

    return EndpointParam(type, "_return_", retType.isMarkedNullable)
}