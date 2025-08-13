package dev.korvian

import kotlin.reflect.KClass
import kotlin.annotation.AnnotationTarget.*
import kotlin.annotation.AnnotationRetention.*

@Retention(RUNTIME)
@Target(CLASS)
annotation class Service(val handler: KClass<*>)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Request

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Publish

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Subscribe

// TODO: should ignore the annotation in ICheck processor
annotation class Doc(val desc: String)