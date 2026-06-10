package cn.ts.configure

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.partialcontent.*

fun Application.configureHTTP() {
    install(PartialContent) {
        maxRangeCount = 10
    }
    install(Compression)
}
