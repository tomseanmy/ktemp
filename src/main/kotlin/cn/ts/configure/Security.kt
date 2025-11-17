package cn.ts.configure

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.csrf.*

fun Application.configureSecurity() {

    // Please read the jwt property from the config file if you are using EngineMain
    val jwtSecret = "secret"
    authentication {
        jwt {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret)).build()
            )
            validate { credential ->
                credential.subject
            }
        }
    }
}
