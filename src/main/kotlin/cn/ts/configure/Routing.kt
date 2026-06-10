package cn.ts.configure

import cn.ts.exception.AuthenticationException
import cn.ts.exception.ForbiddenException
import cn.ts.model.R
import cn.ts.utils.Json
import cn.ts.utils.Validatable
import io.ktor.http.*
import io.ktor.serialization.jackson3.jackson
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
import org.slf4j.LoggerFactory
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import tools.jackson.databind.module.SimpleModule
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    install(SSE)
    install(RequestValidation) {
        validate<Validatable> { it.validate() }
    }
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.INDENT_OUTPUT)
            enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            addModule(SimpleModule().apply {
                addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(formatter))
                addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(formatter))
                //addSerializer(BigDecimal::class.java, DecimalScaleSerializer())
            })
        }
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
        exception<MismatchedInputException> { call, cause ->
            val fieldPath = cause.path.joinToString(".") { it.propertyName ?: "[${it.index}]" }
            val errMsg = if (cause.message?.contains("missing", ignoreCase = true) == true) {
                "缺少必填字段[$fieldPath]"
            } else {
                "请求参数不匹配[$fieldPath]: ${cause.originalMessage}"
            }
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
