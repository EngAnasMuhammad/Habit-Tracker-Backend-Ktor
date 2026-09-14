
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation(ktorLibs.serialization.gson)
    implementation(ktorLibs.server.contentNegotiation)
    //Exposed
    val exposed = "0.46.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.postgresql:postgresql:42.7.12")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
