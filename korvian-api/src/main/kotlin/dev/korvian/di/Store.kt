package dev.korvian.di

import dev.korvian.di.store.ChannelStore
import dev.korvian.di.store.ContextStore
import dev.korvian.di.store.ServiceStore

object Store {
    val Service = ServiceStore()
    val Channel = ChannelStore()
    val Context = ContextStore()
}