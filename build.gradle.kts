plugins {
    kotlin("jvm") version "2.2.0"
}

subprojects {
    group = "dev.korvian"
    version = "0.1.0"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        testImplementation("org.jetbrains.kotlin:kotlin-test")
    }
}