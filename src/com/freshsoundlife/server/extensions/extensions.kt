package com.freshsoundlife.server.extensions

import com.freshsoundlife.auth.AuthorizedRouteSelector
import com.freshsoundlife.auth.Role
import com.freshsoundlife.auth.RoleBasedAuthorization
import com.freshsoundlife.auth.SimpleJWT
import com.freshsoundlife.dependency.kodein
import com.freshsoundlife.entity.User
import com.jillesvangurp.eskotlinwrapper.IndexRepository
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.util.*

private val jwt: SimpleJWT by kodein.instance()
private val userRepo: IndexRepository<User> by kodein.instance()

fun ApplicationResponse.addTokens(id: String, email: String, username: String, role: Role) {

    val refreshToken = jwt.sign(
        id,
        email,
        username,
        Date().apply { time += 259200000 },
        role
    )

    GlobalScope.launch {
        userRepo.update(id) {
            it.copy(token = refreshToken)
        }
    }

    header(
        "Token", jwt.sign(
            id,
            email,
            username,
            Date().apply { time += 1800000 },
            role)
    )
    header(
        "Refresh-Token", refreshToken
    )
}

@KtorExperimentalAPI
fun Route.withRole(role: Role, build: Route.() -> Unit) = authorizedRoute(setOf(role), build)

@KtorExperimentalAPI
fun Route.withRoles(vararg roles: Role, build: Route.() -> Unit) = authorizedRoute(roles.toSet(), build)
/*
fun Route.withAnyRole(vararg roles: Role, build: Route.() -> Unit) = authorizedRoute(any = roles.toSet(), build = build)

fun Route.withoutRoles(vararg roles: Role, build: Route.() -> Unit) =
    authorizedRoute(none = roles.toSet(), build = build)
*/

@KtorExperimentalAPI
private fun Route.authorizedRoute(roles: Set<Role>, build: Route.() -> Unit): Route {

    val authorizedRoute = createChild(AuthorizedRouteSelector(roles.joinToString(", ")))
    application.feature(RoleBasedAuthorization).interceptPipeline(authorizedRoute, roles)
    authorizedRoute.build()
    return authorizedRoute
}
