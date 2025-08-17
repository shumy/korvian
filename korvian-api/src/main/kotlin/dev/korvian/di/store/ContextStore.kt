package dev.korvian.di.store

import dev.korvian.di.DIException
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import java.util.Collections
import kotlin.collections.contains
import kotlin.reflect.KClass

class ContextStore {
    private val tlStore = ThreadLocal<MutableMap<String, Any>>()

    fun getAll(): Map<String, Any> =
        tlStore.get() ?: Collections.emptyMap()

    fun getAsContextElement(): ThreadContextElement<MutableMap<String, Any>> =
        tlStore.asContextElement()

    fun addAll(ctx: Map<String, Any>) {
        tlStore.set(ctx.toMutableMap())
    }

    fun getUndefined(kClass: KClass<*>): Any? =
        getAll()[kClass.qualifiedName!!]

    fun <T: Any> add(kClass: KClass<T>, obj: T) {
        var store = tlStore.get()
        if (store === null) {
            store = mutableMapOf()
            tlStore.set(store)
        }

        val typeName = kClass.qualifiedName!!
        if(store.contains(typeName))
            throw DIException("Context $typeName is already available in store!")

        store[typeName] = obj
    }
}