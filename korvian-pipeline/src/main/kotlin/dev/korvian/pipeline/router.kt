package dev.korvian.pipeline

import dev.korvian.di.store.Endpoint
import dev.korvian.di.store.EndpointType
import dev.korvian.di.store.ServiceStore

data class Route(val srv: Any, val endpoint: Endpoint)

class Router(private val store: ServiceStore) {
    fun resolve(header: Incoming): Route {
        val srv = store.resolveService(header.srv)
            ?: throw PipeException("Service ${header.srv} not found!")

        val endpointName = "${header.srv}:${header.trg}"
        val endpoint = when(header) {
            is Incoming.Request -> store.resolveEndpoint(EndpointType.REQUEST, endpointName)
            is Incoming.Publish -> store.resolveEndpoint(EndpointType.PUBLISH, endpointName)
            is Incoming.Subscribe -> store.resolveEndpoint(EndpointType.SUBSCRIBE, endpointName)
        }

        if (endpoint === null)
            throw PipeException("Endpoint $endpointName not found!")

        return Route(srv, endpoint)
    }
}