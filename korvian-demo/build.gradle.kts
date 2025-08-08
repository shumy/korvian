plugins {
    id("application")
}

dependencies {
    implementation(project(":korvian-pipeline"))
    implementation(project(":korvian-serialization-json"))

    implementation("com.lectra:koson:1.2.9")
}

application {
    mainClass = "MainKt"
}