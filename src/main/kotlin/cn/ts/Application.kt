package cn.ts

import cn.ts.configure.configureDI
import cn.ts.configure.configureDatabases
import cn.ts.configure.configureMq
import cn.ts.configure.configureHTTP
import cn.ts.configure.configureMonitoring
import cn.ts.configure.configureRedisson
import cn.ts.configure.configureRouting
import cn.ts.configure.configureSecurity
import cn.ts.configure.configureSerialization
import cn.ts.configure.configureSockets
import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDI()
    configureRedisson()
    configureMq()
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureSecurity()
    configureHTTP()
    configureRouting()
}
