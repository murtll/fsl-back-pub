package com.freshsoundlife.server

import com.auth0.jwt.exceptions.JWTVerificationException
import com.freshsoundlife.auth.Role
import com.freshsoundlife.exception.BadCredentialsException
import com.freshsoundlife.exception.TokenStillValidException
import com.freshsoundlife.server.extensions.addTokens
import com.jillesvangurp.eskotlinwrapper.dsl.TermQuery
import com.jillesvangurp.eskotlinwrapper.dsl.bool
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.elasticsearch.action.search.dsl
import org.mindrot.jbcrypt.BCrypt
import java.util.*

fun Routing.tokens() = route("/token") {
    //    logging in

    options ("/get") {
//        call.response.header("Access-Control-Allow-Origin", "*")
        call.respond(HttpStatusCode.OK)
    }

    post("/get") {
        val creds = call.receive<Map<String, String>>()
//        val password = call.request.headers["Password"] ?: throw BadCredentialsException()
        val password = creds["password"] ?: throw BadCredentialsException()
        val email = creds["email"] ?: throw BadCredentialsException()
//        val email = call.request.headers["Email"] ?: throw BadCredentialsException()

        val foundUser = userRepo.search {
            dsl {
                resultSize = 1
                query = bool {
                    must(
                        TermQuery("email.keyword", email)
                    )
                }
            }
        }.mappedHits.toList().takeIf { it.isNotEmpty() }?.get(0) ?: throw BadCredentialsException()

        if (BCrypt.checkpw(password, foundUser.password)
            /*foundUser.password == password*/
            && foundUser.status == "active") {
            call.response.addTokens(
                id = foundUser.id,
                email = foundUser.email,
                username = foundUser.username,
                role = foundUser.role
            )

            call.respond(HttpStatusCode.OK)
        } else throw BadCredentialsException()
    }

    post("/refresh") {
        val refreshToken = call.request.headers["Refresh-Token"] ?: throw BadCredentialsException()
        val token = call.request.headers["Authorization"]?.substring(7) ?: throw BadCredentialsException()

//        checking tokens
        try {
            jwt.verifier.verify(refreshToken)
        } catch (e: JWTVerificationException) {
            throw BadCredentialsException()
        }

        val accessExpDate = jwt.decode(token).expiresAt
        if (accessExpDate.time >= Date().time)
            throw TokenStillValidException()

        val expDate = jwt.decode(refreshToken).expiresAt
        val email = jwt.decode(refreshToken).getClaim("email").asString()
        val username = jwt.decode(refreshToken).getClaim("username").asString()
        val id = jwt.decode(refreshToken).getClaim("id").asString()
        val role = Role.valueOf(jwt.decode(refreshToken).getClaim("role").asString())

        val realUser = userRepo.get(id) ?: throw BadCredentialsException()

        if (username == null
            || email == null
            || id == null
            || Date().time > expDate.time
            || refreshToken != realUser.token
        ) {
            throw BadCredentialsException()
        }

        call.response.addTokens(id = id, email = email, username = username, role = role)
        call.respond(HttpStatusCode.OK)

    }
}
