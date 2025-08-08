package dev.korvian.pipeline

import dev.korvian.RejectError
import dev.korvian.di.store.Endpoint
import dev.korvian.di.store.EndpointType
import dev.korvian.di.store.ServiceStore

class Pipeline<I: Any, B: Any, R: Any>(val srvStore: ServiceStore, val decoder: IDecoder<I, B>, val encoder: IEncoder<R>) {

    fun connect(onMsg: MsgCallback<R>): Connection<I, R> =
        Connection(this, onMsg)

    internal fun process(connection: Connection<I, R>, msg: I) {
        val dMsg = decoder.preDecode(msg)
        try {
            if (dMsg.header is Incoming.UnSubscribe) {
                connection.processUnSubscribe(dMsg.header.ref, dMsg.header.sub)
                return
            }

            val srvHeader = dMsg.header as Incoming.ServiceIncoming
            val srv = srvStore.resolveService(srvHeader.srv)
                ?: throw PipeException("Service ${srvHeader.srv} not found!")

            val endpoint = resolveEndpoint(srvHeader)
            // TODO: header checks (security, custom) --> CheckError(error)

            val args = decoder.bodyDecode(dMsg.body, endpoint.params)
            val response = executeEndpoint(connection, srvHeader.ref, srv, endpoint, args)
            if (!response.hasResult)
                return

            val rType = endpoint.retType.kType
            when (endpoint.type) {
                EndpointType.REQUEST -> connection.processRequest(dMsg.header.ref, rType, response.result)
                EndpointType.PUBLISH -> connection.processPublish(dMsg.header.ref)
                EndpointType.SUBSCRIBE -> connection.processSubscribe(dMsg.header.ref, rType, response.result!!)
            }
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
            // TODO: body checks (validation, mandatory, custom) --> RejectError(code, reason)
            return HasResult.yes(endpoint.exec.process(srv, args))
        } catch (ex: RejectError) {
            connection.sendReject(ref, ex.code, ex.reason)
            return HasResult.no()
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