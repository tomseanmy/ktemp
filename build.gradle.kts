plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "cn.ts"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

dependencies {
    implementation(libs.javamoney)
    implementation(libs.ktor.server.rabbitmq)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.hikari)
    implementation(libs.redisson)
    implementation(libs.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.money)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.simple.cache)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.csrf)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.simple.memory.cache)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

}
