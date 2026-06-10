plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

group = "cn.ts"
version = project.properties["app.version"] as String

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.javamoney)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.websockets)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.jackson3)
    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.hikari)
    implementation(libs.redisson)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.money)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.sse)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.csrf)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.partialContent)
    implementation(ktorLibs.server.forwardedHeader)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.compression)
    implementation(ktorLibs.server.cachingHeaders)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation(ktorLibs.server.config.yaml)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotlin.test.junit)

}
