package dev.korvian.pipeline

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

            val endpoint = resolve(srvHeader)
            // TODO: header checks (security, custom)

            val paramValues = decoder.bodyDecode(dMsg.body, endpoint.params)
            // TODO: body checks (validation, mandatory, custom)


            // TODO: how to reject?
            val result = endpoint.exec.process(srv, paramValues)


            val rType = endpoint.retType.kType
            when (endpoint.type) {
                EndpointType.REQUEST -> connection.processRequest(dMsg.header.ref, rType, result)
                EndpointType.PUBLISH -> connection.processPublish(dMsg.header.ref)
                EndpointType.SUBSCRIBE -> connection.processSubscribe(dMsg.header.ref, rType, result!!)
            }
        } catch (ex: PipeException) {
            connection.sendError(dMsg.header.ref, ex.message!!)
        } catch (ex: Throwable) {
            println("Unexpected Exception!")
            ex.printStackTrace()
        }
    }

    private fun resolve(header: Incoming.ServiceIncoming): Endpoint {
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
}

class PipeException(msg: String): RuntimeException(msg)