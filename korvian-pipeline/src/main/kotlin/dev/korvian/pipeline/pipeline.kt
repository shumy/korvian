package dev.korvian.pipeline

import dev.korvian.RejectError
import dev.korvian.di.store.Endpoint
import dev.korvian.di.store.EndpointType
import dev.korvian.di.store.ServiceStore
import kotlin.reflect.KClass

class Pipeline<I: Any, R: Any>(val srvStore: ServiceStore, val serializer: ISerializer<I, R>) {
    private val checks = mutableMapOf<KClass<*>, MutableList<ICheck<Annotation>>>()

    inline fun <reified H: Annotation> addCheck(check: ICheck<H>) {
        addCheck(H::class, check)
    }

    @Suppress("UNCHECKED_CAST")
    fun <H: Annotation> addCheck(kClass: KClass<H>, check: ICheck<H>) {
        var checkList = checks[kClass]
        if (checkList === null) {
            checkList = mutableListOf()
            checks[kClass] = checkList
        }

        checkList += check as ICheck<Annotation>
    }

    fun connect(onMsg: MsgCallback<R>): Connection<I, R> =
        Connection(this, onMsg)

    internal fun process(connection: Connection<I, R>, msg: I) {
        val dMsg = serializer.preDecode(msg)
        try {
            if (dMsg.header is Incoming.UnSubscribe) {
                connection.processUnSubscribe(dMsg.header.ref, dMsg.header.sub)
                return
            }

            val srvHeader = dMsg.header as Incoming.ServiceIncoming
            val srv = srvStore.resolveService(srvHeader.srv)
                ?: throw PipeException("Service ${srvHeader.srv} not found!")

            val endpoint = resolveEndpoint(srvHeader)
            endpoint.spec.annotations.forEach { anno ->
                checks[anno.annotationClass]?.forEach { it.check(anno, endpoint.spec) }
            }

            val args = serializer.bodyDecode(dMsg.body, endpoint.spec.params)
            if (args.size != endpoint.spec.params.size)
                throw PipeException("Incorrect number of arguments from serializer!")

            val response = executeEndpoint(connection, srvHeader.ref, srv, endpoint, args)
            if (!response.hasResult)
                return

            val rType = endpoint.spec.retType.kType
            when (endpoint.spec.type) {
                EndpointType.REQUEST -> connection.processRequest(dMsg.header.ref, rType, response.result)
                EndpointType.PUBLISH -> connection.processPublish(dMsg.header.ref)
                EndpointType.SUBSCRIBE -> connection.processSubscribe(dMsg.header.ref, rType, response.result!!)
            }
        } catch (ex: RejectError) {
            connection.sendReject(dMsg.header.ref, ex.code, ex.reason)
        } catch (ex: PipeException) {
            connection.sendError(dMsg.header.ref, ex.message ?: "Unknown error!")
        } catch (ex: Throwable) {
            println("Unexpected error in pipeline!")
            ex.printStackTrace()
        }
    }

    private fun resolveEndpoint(header: Incoming.ServiceIncoming): Endpoint {
        val endpointName = "${header.srv}:${header.trg}"
        val endpoint = when(header) {
            is Incoming.ServiceIncoming.Request -> srvStore.resolveEndpoint(EndpointType.REQUEST, endpointName)
            is Incoming.ServiceIncoming.Publish -> srvStore.resolveEndpoint(EndpointType.PUBLISH, endpointName)
            is Incoming.ServiceIncoming.Subscribe -> srvStore.resolveEndpoint(EndpointType.SUBSCRIBE, endpointName)
        }

        if (endpoint === null)
            throw PipeException("Endpoint $endpointName not found!")

        return endpoint
    }

    private fun executeEndpoint(connection: Connection<I, R>, ref: String, srv: Any, endpoint: Endpoint, args: Array<Any?>): HasResult {
        try {
            args.zip(endpoint.spec.params) { value, spec ->
                if (value === null && !spec.isOptional)
                    throw RejectError(ErrorCode.Invalid.code, "Mandatory argument ${spec.name} not present!")
            }

            return HasResult.yes(endpoint.exec.process(srv, args))
        } catch (ex: Throwable) {
            connection.sendError(ref, ex.message ?: "Unknown error!")
            return HasResult.no()
        }
    }
}

class PipeException(msg: String): RuntimeException(msg)

class HasResult private constructor(val hasResult: Boolean, val result: Any?) {
    companion object {
        fun no() = HasResult(false, null)
        fun yes(result: Any?) = HasResult(true, result)
    }
}