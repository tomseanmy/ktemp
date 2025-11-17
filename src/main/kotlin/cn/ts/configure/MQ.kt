package cn.ts.configure

import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

fun Application.configureMq() {
    val mqConfig = environment.config.config("mq")
    val enable = mqConfig.tryGetString("enable")?.toBoolean() ?: false
    if (!enable) return
    val url = mqConfig.tryGetString("url") ?: "amqp://guest:guest@localhost:5672"
    val defaultConnectionName = mqConfig.tryGetString("default-name") ?: "default-connection"
    val dispatcherThreadPollSize = mqConfig.tryGetString("thread-poll-size")?.toInt() ?: 6
    val tlsEnable = mqConfig.tryGetString("tls-enable")?.toBoolean() ?: false
    val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log.error("ExceptionHandler got $throwable") }
    val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)
    
    install(RabbitMQ) {
        this.uri = url
        this.defaultConnectionName = defaultConnectionName
        this.dispatcherThreadPollSize = dispatcherThreadPollSize
        this.tlsEnabled = tlsEnable
        scope = rabbitMQScope // custom scope, default is the one provided by Ktor
    }
}
