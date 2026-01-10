package cn.ts.configure

import cn.ts.exception.AuthenticationException
import cn.ts.exception.ForbiddenException
import cn.ts.utils.Validatable
import cn.ts.utils.json
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    install(SSE)
    install(RequestValidation) {
        validate<Validatable> { it.validate() }
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.message ?: "")
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, cause.message ?: "")
        }
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
        exception<MissingFieldException> { call, cause ->
            val errMsg = "缺少必填字段[${cause.missingFields.joinToString()}]"
            call.respond(HttpStatusCode.BadRequest, errMsg)
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "服务器异常")
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        staticResources("/static", "static")
    }
}
