package cn.ts.configure

import cn.ts.exception.AuthenticationException
import cn.ts.exception.ForbiddenException
import cn.ts.model.R
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
import org.slf4j.LoggerFactory

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    install(SSE)
    install(RequestValidation) {
        validate<Validatable> { it.validate() }
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(StatusPages) {
        status(HttpStatusCode.Unauthorized) { call, cause ->
            log.error("认证异常", cause)
            call.respond(HttpStatusCode.Unauthorized, R.err<String>(msg = "请先登录"))
        }
        exception<AuthenticationException> { call, cause ->
            log.error("认证异常", cause)
            call.respond(HttpStatusCode.Unauthorized, R.err<String>(msg = cause.message ?: ""))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, R.err<String>(msg = cause.message ?: ""))
        }
        exception<RequestValidationException> { call, cause ->
            log.error("参数错误", cause)
            call.respond(HttpStatusCode.BadRequest, R.err<String>(msg = cause.reasons.joinToString()))
        }
        exception<MissingFieldException> { call, cause ->
            val errMsg = "缺少必填字段[${cause.missingFields.joinToString()}]"
            call.respond(HttpStatusCode.BadRequest, R.err<String>(msg = errMsg))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, R.err<String>(msg = cause.message ?: "服务器异常"))
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        staticResources("/static", "static")
    }
}
