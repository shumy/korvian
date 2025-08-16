plugins {
    id("application")
}

dependencies {
    implementation(project(":korvian-pipeline"))
    implementation(project(":korvian-serialization-json"))
    implementation(project(":korvian-netty-server"))

    implementation("com.lectra:koson:1.2.9")
}

application {
    mainClass = "MainKt"
}