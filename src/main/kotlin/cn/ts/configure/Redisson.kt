package cn.ts.configure

import io.ktor.server.application.*
import io.ktor.server.config.*
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.redisson.Redisson
import org.redisson.config.Config
import org.redisson.config.SslVerificationMode

enum class RedisMode {
    SINGLE, CLUSTER
}

/**
 * redis配置
 * @author tomsean
 */
fun Application.configureRedisson() {
    val redisConfig = environment.config.config("redis")
    val enable = redisConfig.tryGetString("enable")?.toBoolean() ?: false
    if (!enable) {
        return
    }
    val mode = redisConfig.tryGetString("mode")?.let { RedisMode.valueOf(it) } ?: RedisMode.SINGLE
    val password = redisConfig.tryGetString("password")

    val db = redisConfig.tryGetString("db")?.toInt() ?: 0
    val timeout = redisConfig.tryGetString("timeout")?.toInt() ?: 10000 // 默认10秒超时

    val redissonClient = Redisson.create(Config().apply {
        when (mode) {
            RedisMode.SINGLE -> useSingleServer().apply {
                val url = redisConfig.tryGetString("url")
                setSslVerificationMode(SslVerificationMode.NONE)
                this.address = url
                this.database = db
                if (!password.isNullOrBlank()) {
                    this.password = password
                }
                val minSize = redisConfig.tryGetString("min-idle-size")?.toInt()
                val poolSize = redisConfig.tryGetString("pool-size")?.toInt()
                this.timeout = timeout
                this.connectTimeout = 30000
                this.retryAttempts = 0
                poolSize?.let { this.connectionPoolSize = poolSize }
                minSize?.let { this.connectionMinimumIdleSize = minSize }
            }

            RedisMode.CLUSTER -> useClusterServers().apply {
                val url = redisConfig.tryGetStringList("url")
                this.nodeAddresses = url
                if (!password.isNullOrBlank()) {
                    this.password = password
                }
                this.timeout = timeout
                this.connectTimeout = timeout
                val poolSize = redisConfig.tryGetStringList("pool-size")?.map { it.toInt()} ?: listOf(16, 16)
                this.masterConnectionPoolSize = poolSize[0]
                this.slaveConnectionPoolSize = poolSize[1]
            }
        }
    })
    loadKoinModules(module {
        single { redisConfig }
        single { redissonClient }
    })
}