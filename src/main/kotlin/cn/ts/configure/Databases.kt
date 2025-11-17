package cn.ts.configure

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dbConfig = environment.config.config("db")
    val url = dbConfig.tryGetString("url")
    val driver = dbConfig.tryGetString("driver")
    val user = dbConfig.tryGetString("user")
    val password = dbConfig.tryGetString("password")
    val poolSize = dbConfig.tryGetString("pool")?.toInt() ?: 20
    val maxPoolSize = dbConfig.tryGetString("max-pool")?.toInt()
    val maxLifetime = dbConfig.tryGetString("max-lifetime")?.toLong()
    if (url == null || driver == null || user == null || password == null) {
        throw Exception("Please specify database connection details.")
    }
    val db = Database.connect(HikariDataSource().apply {
        this.jdbcUrl = url
        this.driverClassName = driver
        this.username = user
        this.password = password
        this.maximumPoolSize = poolSize
        maxPoolSize?.let { v -> this.maximumPoolSize = v }
        maxLifetime?.let { v -> this.maxLifetime = v }
    }, databaseConfig = DatabaseConfig {
        /* 其他设置 */
    })
    transaction(db) {
        // 自动创建数据表
        // SchemaUtils.create()
    }
}
