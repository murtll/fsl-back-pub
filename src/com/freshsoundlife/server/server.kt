package com.freshsoundlife.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.freshsoundlife.auth.Role
import com.freshsoundlife.auth.RoleBasedAuthorization
import com.freshsoundlife.auth.SimpleJWT
import com.freshsoundlife.dependency.kodein
import com.freshsoundlife.entity.Post
import com.freshsoundlife.entity.User
import com.freshsoundlife.entity.UserModel
import com.freshsoundlife.exception.UnauthorizedException
import com.icerockdev.service.email.MailerService
import com.jillesvangurp.eskotlinwrapper.IndexRepository
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.kodein.di.generic.instance
import java.io.File
import java.util.*

internal val userRepo: IndexRepository<User> by kodein.instance()
internal val postRepo: IndexRepository<Post> by kodein.instance()
internal val jwt: SimpleJWT by kodein.instance()
internal val mailService: MailerService by kodein.instance()
internal val objectMapper: ObjectMapper by kodein.instance()

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused")
fun Application.module() {
    install(CORS) {
//        method(HttpMethod.Put)
//        method(HttpMethod.Get)
//        method(HttpMethod.Post)
//        method(HttpMethod.Delete)
//        method(HttpMethod.Options)
//
//        header(HttpHeaders.Authorization)
//        header(HttpHeaders.AccessControlAllowHeaders)
//        header(HttpHeaders.AccessControlAllowMethods)
//        header(HttpHeaders.AccessControlAllowCredentials)
//        header(HttpHeaders.AccessControlAllowOrigin)
//
//        header(HttpHeaders.AccessControlRequestHeaders)
//        header(HttpHeaders.AccessControlRequestMethod)
//        header(HttpHeaders.Accept)
//        header(HttpHeaders.AcceptLanguage)
//        header(HttpHeaders.AcceptEncoding)
//        header(HttpHeaders.AcceptEncoding)
//        header("Referer")
//        header(HttpHeaders.Origin)
//        header(HttpHeaders.Connection)
//        header(HttpHeaders.Host)
//        header(HttpHeaders.UserAgent)

//        header(HttpHeaders.Origin)
//        allowNonSimpleContentTypes = true
//        allowCredentials = true

//        anyHost()
//        allowSameOrigin = true
        method(HttpMethod.Options)
        method(HttpMethod.Delete)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost()
    }
//    install(Locations)
    install(DefaultHeaders) {
//        header("Access-Control-Allow-Origin", "*")
//        header("Access-Control-Allow-Headers", "*")
//        header("Access-Control-Allow-Methods", "*")
        header("Access-Control-Expose-Headers", "*")
//        header("Access-Control-Allow-Credentials", "true")
    }
    install(StatusPages) {
        exception<UnauthorizedException> { e ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to e.message))
        }
        exception<Exception> { e ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to e.message, "stacktrace" to e.stackTraceToString()))
        }
    }
    install(Authentication) {
        jwt {
//            skipWhen { call -> call.sessions.get<User>() != null }

            realm = "FreshSoundLife"
            verifier(jwt.verifier)

            validate { token ->
                val email = token.payload.getClaim("email").asString()
                val username = token.payload.getClaim("username").asString()
                val id = token.payload.getClaim("id").asString()
                val expDate = token.payload.expiresAt
                val role = Role.valueOf(token.payload.getClaim("role").asString())

                if (username != null && email != null && id != null && Date().time <= expDate.time)
                    UserModel(
                        username = username,
                        email = email,
                        id = id,
                        role = role
                    )
                else
                    null
            }
        }
    }
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(RoleBasedAuthorization)
    install(WebSockets)

    routing {
        static("/resources") {
            resources("/public/")
        }

        root()
        users()
        tokens()
        posts()
        annos()
        data()
//        chat()
    }
}

fun Routing.root() = get("/") {
    call.respondText("<center><h1>FSL PROJECT API ROOT</h1></center>", contentType = ContentType.Text.Html)
}

fun Routing.data() = route("/data") {
    get("/cities") {
        call.respond(objectMapper.readValue(File("resources/public/data/pipirka.json").readText(), List::class.java))
    }
}
