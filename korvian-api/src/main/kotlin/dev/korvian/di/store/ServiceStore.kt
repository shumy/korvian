package dev.korvian.di.store

import dev.korvian.IReply
import dev.korvian.IReplyTask
import dev.korvian.IStream
import dev.korvian.IStreamTask
import dev.korvian.ISubscription
import dev.korvian.Publish
import dev.korvian.Request
import dev.korvian.Service
import dev.korvian.Subscribe
import dev.korvian.di.DIException
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

class ServiceStore {
    private val store = mutableMapOf<String, Any>()
    private val endpoints = mutableMapOf<String, Endpoint>()

    fun getUndefined(kClass: KClass<*>): Any? =
        store[kClass.qualifiedName!!]

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getOptional(kClass: KClass<T>): T? =
        store[kClass.qualifiedName!!] as T?

    fun <T: Any> get(kClass: KClass<T>): T =
        getOptional(kClass) ?: throw DIException("Service ${kClass.qualifiedName} not found!")

    fun resolveService(srv: String) = store[srv]

    fun resolveEndpoint(type: EndpointType, target: String): Endpoint? {
        val endpoint = endpoints[target]
        if (endpoint?.type !== type)
            return null

        return endpoint
    }

    operator fun plusAssign(kClass: KClass<*>) = add(kClass)
    fun add(kClass: KClass<*>) {
        val typeName = kClass.qualifiedName!!
        if(store.contains(typeName))
            throw DIException("Service ${kClass.qualifiedName} is already available in store!")

        if(!kClass.java.isInterface)
            throw DIException("Service ${kClass.qualifiedName} is not an interface!")

        val annService = kClass.findAnnotations(Service::class).firstOrNull()
            ?: throw DIException("Service ${kClass.qualifiedName} requires @Service annotation!")

        if(!kClass.isSuperclassOf(annService.handler))
            throw DIException("Handler for service ${kClass.qualifiedName} doesn't implement the service interface!")

        val defaultConstructor = annService.handler.primaryConstructor!!
        if (defaultConstructor.parameters.isNotEmpty())
            throw DIException("Service ${kClass.qualifiedName} with parameters is not supported!")

        for (eMember in kClass.declaredMembers) {
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

        val executor = EndpointExecutor { srv, args -> eMember.call(srv, *args) }
        val params = eMember.parameters.drop(1).map { checkAndConvertParameter(endpointName, it) }
        val retType = checkAndConvertReturnType(endpointName, eType, eMember.returnType)

        return Endpoint(eType, endpointName, executor, params, retType)
    }
}

enum class EndpointType { REQUEST, PUBLISH, SUBSCRIBE }
data class EndpointParam(val kType: KType, val name: String, val isOptional: Boolean)
data class Endpoint(val type: EndpointType, val name: String, val exec: EndpointExecutor, val params: List<EndpointParam>, val retType: EndpointParam)

fun interface EndpointExecutor {
    fun process(srv: Any, args: Array<Any?>): Any?
}

private fun checkAndConvertParameter(endpoint: String, param: KParameter): EndpointParam {
    if (param.isVararg)
        throw DIException("Varargs are not supported in service methods! Param ${param.name} for $endpoint.")

    if (!isSupportedType(param.type))
        throw DIException("Unsupported parameter type (${param.name}: ${param.type}) for $endpoint.")

    return EndpointParam(param.type, param.name!!, param.isOptional)
}

private fun checkAndConvertReturnType(endpoint: String, eType: EndpointType, retType: KType): EndpointParam {
    val kClass = retType.classifier!! as KClass<*>
    val resolvedRetType = when (eType) {
        EndpointType.PUBLISH -> {
            if (kClass != Unit::class)
                throw DIException("Expected Unit return type for @Publish endpoint $endpoint! Return type is ($retType).")
            typeOf<Unit>()
        }

        EndpointType.SUBSCRIBE -> {
            if (kClass != ISubscription::class)
                throw DIException("Expecting ISubscription return type for @Subscribe endpoint $endpoint! Return type is ($retType).")
            retType.arguments[0].type!!
        }

        EndpointType.REQUEST -> {
            if (replyTypes.contains(kClass))
                retType.arguments[0].type!!
            else retType
        }
    }

    if (!isSupportedType(resolvedRetType))
        throw DIException("Unsupported return type (${retType}) for $endpoint.")

    return EndpointParam(resolvedRetType, "_return_", retType.isMarkedNullable)
}

fun isSupportedType(rType: KType): Boolean {
    val rClass = rType.classifier!! as KClass<*>

    if (rClass == List::class || rClass == Set::class)
        return isSupportedType(rType.arguments[0].type!!)

    if (rClass == Map::class)
        return isSupportedType(rType.arguments[0].type!!) && isSupportedType(rType.arguments[1].type!!)

    return nativeTypes.contains(rClass) || rClass.hasAnnotation<Serializable>()
}

private val replyTypes = setOf(
    IReply::class, IReplyTask::class,
    IStream::class, IStreamTask::class
)

private val nativeTypes = setOf<Any>(
    Unit::class,
    Boolean::class,
    Char::class, String::class,
    Int::class, UInt::class,
    Long::class, ULong::class,
    Float::class, Double::class,
    LocalDate::class, LocalTime::class, LocalDateTime::class,
)