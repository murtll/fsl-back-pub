package com.freshsoundlife.auth

import com.freshsoundlife.entity.UserModel
import com.freshsoundlife.exception.NoNeededRoleException
import com.freshsoundlife.exception.UnauthorizedException
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

enum class Role {
    ADMIN,
    USER
}

class RoleBasedAuthorization {

    @KtorExperimentalAPI
    fun interceptPipeline(
        pipeline: ApplicationCallPipeline, roles: Set<Role>
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, Authentication.ChallengePhase)
        pipeline.insertPhaseAfter(Authentication.ChallengePhase, AuthorizationPhase)

        pipeline.intercept(AuthorizationPhase) {
            val principal =
                call.authentication.principal<UserModel>() ?: throw UnauthorizedException()
            val role = principal.role
            if (!roles.contains(role)) {
                throw NoNeededRoleException()
            }
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Any, RoleBasedAuthorization> {
        override val key = AttributeKey<RoleBasedAuthorization>("RoleBasedAuthorization")

        val AuthorizationPhase = PipelinePhase("Authorization")

        override fun install(
            pipeline: ApplicationCallPipeline, configure: Any.() -> Unit
        ): RoleBasedAuthorization {
            return RoleBasedAuthorization()
        }


    }
}

class AuthorizedRouteSelector(private val description: String) :
    RouteSelector(RouteSelectorEvaluation.qualityConstant) {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant

    override fun toString(): String = "(authorize ${description})"
}

